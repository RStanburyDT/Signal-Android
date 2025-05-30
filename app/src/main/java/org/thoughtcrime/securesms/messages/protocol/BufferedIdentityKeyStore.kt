package org.thoughtcrime.securesms.ryan.messages.protocol

import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.state.IdentityKeyStore
<<<<<<< HEAD
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
=======
import org.signal.libsignal.protocol.state.IdentityKeyStore.IdentityChange
import org.thoughtcrime.securesms.database.SignalDatabase
>>>>>>> 23669c3c372284d42db486a218d9f29bef247abf
import org.whispersystems.signalservice.api.SignalServiceAccountDataStore
import org.whispersystems.signalservice.api.push.ServiceId

/**
 * An in-memory identity key store that is intended to be used temporarily while decrypting messages.
 */
class BufferedIdentityKeyStore(
  private val selfServiceId: ServiceId,
  private val selfIdentityKeyPair: IdentityKeyPair,
  private val selfRegistrationId: Int
) : IdentityKeyStore {

  private val store: MutableMap<SignalProtocolAddress, IdentityKey> = HashMap()

  /** All of the keys that have been created or updated during operation. */
  private val updatedKeys: MutableMap<SignalProtocolAddress, IdentityKey> = mutableMapOf()

  override fun getIdentityKeyPair(): IdentityKeyPair {
    return selfIdentityKeyPair
  }

  override fun getLocalRegistrationId(): Int {
    return selfRegistrationId
  }

  override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): IdentityChange {
    val existing: IdentityKey? = getIdentity(address)

    store[address] = identityKey

    return if (identityKey != existing) {
      updatedKeys[address] = identityKey
      IdentityChange.REPLACED_EXISTING
    } else {
      IdentityChange.NEW_OR_UNCHANGED
    }
  }

  override fun isTrustedIdentity(address: SignalProtocolAddress, identityKey: IdentityKey, direction: IdentityKeyStore.Direction): Boolean {
    if (address.name == selfServiceId.toString()) {
      return identityKey == selfIdentityKeyPair.publicKey
    }

    return when (direction) {
      IdentityKeyStore.Direction.RECEIVING -> true
      IdentityKeyStore.Direction.SENDING -> error("Should not happen during the intended usage pattern of this class")
      else -> error("Unknown direction: $direction")
    }
  }

  override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
    val cached = store[address]

    return if (cached != null) {
      cached
    } else {
      val fromDatabase = SignalDatabase.identities.getIdentityStoreRecord(address.name)
      if (fromDatabase != null) {
        store[address] = fromDatabase.identityKey
      }

      fromDatabase?.identityKey
    }
  }

  fun flushToDisk(persistentStore: SignalServiceAccountDataStore) {
    for ((address, identityKey) in updatedKeys) {
      persistentStore.saveIdentity(address, identityKey)
    }

    updatedKeys.clear()
  }
}
