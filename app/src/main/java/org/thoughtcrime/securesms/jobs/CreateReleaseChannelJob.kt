package org.thoughtcrime.securesms.ryan.jobs

import androidx.core.content.ContextCompat
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.avatar.Avatar
import org.thoughtcrime.securesms.ryan.avatar.AvatarRenderer
import org.thoughtcrime.securesms.ryan.avatar.Avatars
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.jobmanager.Job
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.profiles.AvatarHelper
import org.thoughtcrime.securesms.ryan.profiles.ProfileName
import org.thoughtcrime.securesms.ryan.providers.BlobProvider
import org.thoughtcrime.securesms.ryan.recipients.Recipient
import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.thoughtcrime.securesms.ryan.transport.RetryLaterException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Creates the Release Channel (Signal) recipient.
 */
class CreateReleaseChannelJob private constructor(parameters: Parameters) : BaseJob(parameters) {
  companion object {
    const val KEY = "CreateReleaseChannelJob"

    private val TAG = Log.tag(CreateReleaseChannelJob::class.java)

    fun create(): CreateReleaseChannelJob {
      return CreateReleaseChannelJob(
        Parameters.Builder()
          .setQueue("CreateReleaseChannelJob")
          .setMaxInstancesForFactory(1)
          .setMaxAttempts(3)
          .build()
      )
    }
  }

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onRun() {
    if (!SignalStore.account.isRegistered) {
      Log.i(TAG, "Not registered, skipping.")
      return
    }

    if (SignalStore.releaseChannel.releaseChannelRecipientId != null) {
      Log.i(TAG, "Already created Release Channel recipient ${SignalStore.releaseChannel.releaseChannelRecipientId}")

      val recipient = Recipient.resolved(SignalStore.releaseChannel.releaseChannelRecipientId!!)
      if (recipient.profileAvatar.isNullOrEmpty() || !SignalStore.releaseChannel.hasUpdatedAvatar) {
        SignalStore.releaseChannel.hasUpdatedAvatar = true
        setAvatar(recipient.id)
      }
    } else {
      val recipients = SignalDatabase.recipients

      val releaseChannelId: RecipientId = recipients.insertReleaseChannelRecipient()
      SignalStore.releaseChannel.setReleaseChannelRecipientId(releaseChannelId)
      SignalStore.releaseChannel.hasUpdatedAvatar = true

      recipients.setProfileName(releaseChannelId, ProfileName.asGiven("Signal"))
      recipients.setMuted(releaseChannelId, Long.MAX_VALUE)
      setAvatar(releaseChannelId)
    }
  }

  private fun setAvatar(id: RecipientId) {
    val latch = CountDownLatch(1)
    AvatarRenderer.renderAvatar(
      context,
      Avatar.Resource(
        R.drawable.ic_signal_logo_large,
        Avatars.ColorPair(ContextCompat.getColor(context, R.color.notification_background_ultramarine), ContextCompat.getColor(context, R.color.core_white), "")
      ),
      onAvatarRendered = { media ->
        AvatarHelper.setAvatar(context, id, BlobProvider.getInstance().getStream(context, media.uri))
        SignalDatabase.recipients.setProfileAvatar(id, "local")
        latch.countDown()
      },
      onRenderFailed = { t ->
        Log.w(TAG, t)
        latch.countDown()
      }
    )

    try {
      val completed: Boolean = latch.await(30, TimeUnit.SECONDS)
      if (!completed) {
        throw RetryLaterException()
      }
    } catch (e: InterruptedException) {
      throw RetryLaterException()
    }
  }

  override fun onShouldRetry(e: Exception): Boolean = e is RetryLaterException

  class Factory : Job.Factory<CreateReleaseChannelJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): CreateReleaseChannelJob {
      return CreateReleaseChannelJob(parameters)
    }
  }
}
