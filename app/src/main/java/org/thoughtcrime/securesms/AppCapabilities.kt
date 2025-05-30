package org.thoughtcrime.securesms.ryan

import org.whispersystems.signalservice.api.account.AccountAttributes

object AppCapabilities {
  /**
   * @param storageCapable Whether or not the user can use storage service. This is another way of
   * asking if the user has set a Signal PIN or not.
   */
  @JvmStatic
  fun getCapabilities(storageCapable: Boolean): AccountAttributes.Capabilities {
    return AccountAttributes.Capabilities(
      storage = storageCapable,
      deleteSync = true,
      versionedExpirationTimer = true,
      storageServiceEncryptionV2 = true,
      attachmentBackfill = true
    )
  }
}
