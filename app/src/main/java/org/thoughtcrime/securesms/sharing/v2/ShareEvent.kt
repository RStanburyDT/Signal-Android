package org.thoughtcrime.securesms.ryan.sharing.v2

import org.thoughtcrime.securesms.ryan.contacts.paged.ContactSearchKey
import org.thoughtcrime.securesms.ryan.sharing.MultiShareArgs

sealed class ShareEvent {

  protected abstract val shareData: ResolvedShareData
  protected abstract val contacts: List<ContactSearchKey.RecipientSearchKey>

  fun getMultiShareArgs(): MultiShareArgs {
    return shareData.toMultiShareArgs().buildUpon(
      contacts.toSet()
    ).build()
  }

  data class OpenConversation(override val shareData: ResolvedShareData, val contact: ContactSearchKey.RecipientSearchKey) : ShareEvent() {
    override val contacts: List<ContactSearchKey.RecipientSearchKey> = listOf(contact)
  }

  data class OpenMediaInterstitial(override val shareData: ResolvedShareData, override val contacts: List<ContactSearchKey.RecipientSearchKey>) : ShareEvent()
  data class OpenTextInterstitial(override val shareData: ResolvedShareData, override val contacts: List<ContactSearchKey.RecipientSearchKey>) : ShareEvent()
  data class SendWithoutInterstitial(override val shareData: ResolvedShareData, override val contacts: List<ContactSearchKey.RecipientSearchKey>) : ShareEvent()
}
