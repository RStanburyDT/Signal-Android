package org.thoughtcrime.securesms.ryan.sharing.v2

data class ShareState(
  val loadState: ShareDataLoadState = ShareDataLoadState.Init
) {
  sealed class ShareDataLoadState {
    object Init : ShareDataLoadState()
    data class Loaded(val resolvedShareData: ResolvedShareData) : ShareDataLoadState()
    object Failed : ShareDataLoadState()
  }
}
