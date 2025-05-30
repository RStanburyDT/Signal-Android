package org.thoughtcrime.securesms.ryan.jobs

import androidx.annotation.WorkerThread
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okio.ByteString.Companion.toByteString
import org.signal.core.util.concurrent.safeBlockingGet
import org.signal.core.util.logging.Log
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper
import org.signal.libsignal.protocol.util.Medium
import org.thoughtcrime.securesms.ryan.components.settings.app.changenumber.ChangeNumberViewModel
import org.thoughtcrime.securesms.ryan.crypto.PreKeyUtil
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies
import org.thoughtcrime.securesms.ryan.jobmanager.Job
import org.thoughtcrime.securesms.ryan.jobmanager.impl.NetworkConstraint
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.net.SignalNetwork
import org.whispersystems.signalservice.api.NetworkResult
import org.whispersystems.signalservice.api.account.PniKeyDistributionRequest
import org.whispersystems.signalservice.api.push.SignalServiceAddress
import org.whispersystems.signalservice.api.push.SignedPreKeyEntity
import org.whispersystems.signalservice.internal.push.KyberPreKeyEntity
import org.whispersystems.signalservice.internal.push.MismatchedDevices
import org.whispersystems.signalservice.internal.push.OutgoingPushMessage
import org.whispersystems.signalservice.internal.push.SyncMessage
import org.whispersystems.signalservice.internal.push.VerifyAccountResponse
import java.io.IOException
import java.security.SecureRandom

/**
 * To be run when all clients support PNP and we need to initialize all linked devices with appropriate PNP data.
 *
 * We reuse the change number flow as it already support distributing the necessary data in a way linked devices can understand.
 */
class PnpInitializeDevicesJob private constructor(parameters: Parameters) : BaseJob(parameters) {

  companion object {
    const val KEY = "PnpInitializeDevicesJob"
    private val TAG = Log.tag(PnpInitializeDevicesJob::class.java)

    @JvmStatic
    fun enqueueIfNecessary() {
      if (SignalStore.misc.hasPniInitializedDevices || !SignalStore.account.isRegistered || SignalStore.account.aci == null) {
        return
      }

      AppDependencies.jobManager.add(PnpInitializeDevicesJob())
    }
  }

  constructor() : this(Parameters.Builder().addConstraint(NetworkConstraint.KEY).build())

  override fun serialize(): ByteArray? {
    return null
  }

  override fun getFactoryKey(): String {
    return KEY
  }

  override fun onFailure() = Unit

  @Throws(Exception::class)
  public override fun onRun() {
    if (!SignalStore.account.isRegistered || SignalStore.account.aci == null) {
      Log.w(TAG, "Not registered! Skipping, as it wouldn't do anything.")
      return
    }

    if (!SignalStore.account.hasLinkedDevices) {
      Log.i(TAG, "Not multi device, aborting...")
      SignalStore.misc.hasPniInitializedDevices = true
      return
    }

    if (SignalStore.account.isLinkedDevice) {
      Log.i(TAG, "Not primary device, aborting...")
      SignalStore.misc.hasPniInitializedDevices = true
      return
    }

    ChangeNumberViewModel.CHANGE_NUMBER_LOCK.lock()
    try {
      if (SignalStore.misc.hasPniInitializedDevices) {
        Log.w(TAG, "We found out that things have been initialized after we got the lock! No need to do anything else.")
        return
      }

      val e164 = SignalStore.account.requireE164()

      try {
        Log.i(TAG, "Initializing PNI for linked devices")
        val result: NetworkResult<VerifyAccountResponse> = initializeDevices(e164)
          .safeBlockingGet()

        result.getCause()?.let { throw it }
      } catch (e: InterruptedException) {
        throw IOException("Retry", e)
      } catch (t: Throwable) {
        Log.w(TAG, "Unable to initialize PNI for linked devices", t)
        throw t
      }

      SignalStore.misc.hasPniInitializedDevices = true
    } finally {
      ChangeNumberViewModel.CHANGE_NUMBER_LOCK.unlock()
    }
  }

  private fun initializeDevices(newE164: String): Single<NetworkResult<VerifyAccountResponse>> {
    val messageSender = AppDependencies.signalServiceMessageSender

    return Single.fromCallable {
      var completed = false
      var attempts = 0
      lateinit var distributionResponse: NetworkResult<VerifyAccountResponse>

      while (!completed && attempts < 5) {
        val request = createInitializeDevicesRequest(
          newE164 = newE164
        )

        distributionResponse = SignalNetwork.account.distributePniKeys(request)
        when (val result = distributionResponse) {
          is NetworkResult.Success -> completed = true
          is NetworkResult.StatusCodeError -> {
            when (result.code) {
              409 -> {
                val mismatchedDevices: MismatchedDevices? = result.parseJsonBody()
                if (mismatchedDevices != null) {
                  messageSender.handleChangeNumberMismatchDevices(mismatchedDevices)
                } else {
                  Log.w(TAG, "Unable to parse mismatched devices", result.exception)
                }
                attempts++
              }
              else -> completed = true
            }
          }
          is NetworkResult.NetworkError -> attempts++
          is NetworkResult.ApplicationError -> completed = true
        }
      }

      distributionResponse
    }.subscribeOn(Schedulers.single())
  }

  @WorkerThread
  private fun createInitializeDevicesRequest(
    newE164: String
  ): PniKeyDistributionRequest {
    val selfIdentifier: String = SignalStore.account.requireAci().toString()
    val aciProtocolStore: SignalProtocolStore = AppDependencies.protocolStore.aci()
    val pniProtocolStore: SignalProtocolStore = AppDependencies.protocolStore.pni()
    val messageSender = AppDependencies.signalServiceMessageSender

    val pniIdentity: IdentityKeyPair = SignalStore.account.pniIdentityKey
    val deviceMessages = mutableListOf<OutgoingPushMessage>()
    val devicePniSignedPreKeys = mutableMapOf<Int, SignedPreKeyEntity>()
    val devicePniLastResortKyberPreKeys = mutableMapOf<Int, KyberPreKeyEntity>()
    val pniRegistrationIds = mutableMapOf<Int, Int>()
    val primaryDeviceId: Int = SignalServiceAddress.DEFAULT_DEVICE_ID

    val devices: List<Int> = listOf(primaryDeviceId) + aciProtocolStore.getSubDeviceSessions(selfIdentifier)

    devices
      .filter { it == primaryDeviceId || aciProtocolStore.containsSession(SignalProtocolAddress(selfIdentifier, it)) }
      .forEach { deviceId ->
        // Signed Prekeys
        val signedPreKeyRecord: SignedPreKeyRecord = if (deviceId == primaryDeviceId) {
          pniProtocolStore.loadSignedPreKey(SignalStore.account.pniPreKeys.activeSignedPreKeyId)
        } else {
          PreKeyUtil.generateSignedPreKey(SecureRandom().nextInt(Medium.MAX_VALUE), pniIdentity.privateKey)
        }
        devicePniSignedPreKeys[deviceId] = SignedPreKeyEntity(signedPreKeyRecord.id, signedPreKeyRecord.keyPair.publicKey, signedPreKeyRecord.signature)

        // Last-resort kyber prekeys
        val lastResortKyberPreKeyRecord: KyberPreKeyRecord = if (deviceId == primaryDeviceId) {
          pniProtocolStore.loadKyberPreKey(SignalStore.account.pniPreKeys.lastResortKyberPreKeyId)
        } else {
          PreKeyUtil.generateLastResortKyberPreKey(SecureRandom().nextInt(Medium.MAX_VALUE), pniIdentity.privateKey)
        }
        devicePniLastResortKyberPreKeys[deviceId] = KyberPreKeyEntity(lastResortKyberPreKeyRecord.id, lastResortKyberPreKeyRecord.keyPair.publicKey, lastResortKyberPreKeyRecord.signature)

        // Registration Ids
        var pniRegistrationId = if (deviceId == primaryDeviceId) {
          SignalStore.account.pniRegistrationId
        } else {
          -1
        }

        while (pniRegistrationId < 0 || pniRegistrationIds.values.contains(pniRegistrationId)) {
          pniRegistrationId = KeyHelper.generateRegistrationId(false)
        }
        pniRegistrationIds[deviceId] = pniRegistrationId

        // Device Messages
        if (deviceId != primaryDeviceId) {
          val pniChangeNumber = SyncMessage.PniChangeNumber(
            identityKeyPair = pniIdentity.serialize().toByteString(),
            signedPreKey = signedPreKeyRecord.serialize().toByteString(),
            lastResortKyberPreKey = lastResortKyberPreKeyRecord.serialize().toByteString(),
            registrationId = pniRegistrationId,
            newE164 = newE164
          )

          deviceMessages += messageSender.getEncryptedSyncPniInitializeDeviceMessage(deviceId, pniChangeNumber)
        }
      }

    return PniKeyDistributionRequest(
      pniIdentity.publicKey,
      deviceMessages,
      devicePniSignedPreKeys.mapKeys { it.key.toString() },
      devicePniLastResortKyberPreKeys.mapKeys { it.key.toString() },
      pniRegistrationIds.mapKeys { it.key.toString() },
      true
    )
  }

  override fun onShouldRetry(e: Exception): Boolean {
    return e is IOException
  }

  class Factory : Job.Factory<PnpInitializeDevicesJob?> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): PnpInitializeDevicesJob {
      return PnpInitializeDevicesJob(parameters)
    }
  }
}
