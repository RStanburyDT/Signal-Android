/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.conversation.v2

import android.content.Context
import android.transition.ChangeBounds
import android.transition.Slide
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.compose.ui.platform.ComposeView
import androidx.core.transition.addListener
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.banner.Banner
import org.thoughtcrime.securesms.ryan.banner.BannerManager
import org.thoughtcrime.securesms.ryan.components.identity.UnverifiedBannerView
import org.thoughtcrime.securesms.ryan.components.voice.VoiceNotePlayerView
import org.thoughtcrime.securesms.ryan.database.identity.IdentityRecordList
import org.thoughtcrime.securesms.ryan.database.model.IdentityRecord
import org.thoughtcrime.securesms.ryan.groups.GroupId
import org.thoughtcrime.securesms.ryan.profiles.spoofing.ReviewBannerView
import org.thoughtcrime.securesms.ryan.recipients.RecipientId
import org.thoughtcrime.securesms.ryan.util.IdentityUtil
import org.thoughtcrime.securesms.ryan.util.ViewUtil
import org.thoughtcrime.securesms.ryan.util.views.Stub
import org.thoughtcrime.securesms.ryan.util.visible

/**
 * Responsible for showing the various "banner" views at the top of a conversation
 *
 * - Expired Build
 * - Unregistered
 * - Group join requests
 * - GroupV1 suggestions
 * - Disable Chat Bubbles setting
 * - Service outage
 */
class ConversationBannerView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {
  private val unverifiedBannerStub: Stub<UnverifiedBannerView> by lazy { ViewUtil.findStubById(this, R.id.unverified_banner_stub) }
  private val bannerStub: Stub<ComposeView> by lazy { ViewUtil.findStubById(this, R.id.banner_stub) }
  private val reviewBannerStub: Stub<ReviewBannerView> by lazy { ViewUtil.findStubById(this, R.id.review_banner_stub) }
  private val voiceNotePlayerStub: Stub<View> by lazy { ViewUtil.findStubById(this, R.id.voice_note_player_stub) }

  var listener: Listener? = null

  init {
    orientation = VERTICAL
  }

  fun collectAndShowBanners(flows: List<Banner<*>>) {
    val bannerManager = BannerManager(flows)
    show(stub = bannerStub) {
      bannerManager.updateContent(this)
    }
  }

  fun clearBanner() {
    hide(bannerStub)
  }

  fun showUnverifiedBanner(identityRecords: IdentityRecordList) {
    show(
      stub = unverifiedBannerStub
    ) {
      setOnHideListener {
        clearUnverifiedBanner()
        true
      }
      display(
        IdentityUtil.getUnverifiedBannerDescription(context, identityRecords.unverifiedRecipients)!!,
        identityRecords.unverifiedRecords,
        { listener?.onUnverifiedBannerClicked(identityRecords.unverifiedRecords) },
        { listener?.onUnverifiedBannerDismissed(identityRecords.unverifiedRecords) }
      )
    }
  }

  fun clearUnverifiedBanner() {
    hide(unverifiedBannerStub)
  }

  fun showReviewBanner(requestReviewState: RequestReviewState) {
    show(
      stub = reviewBannerStub
    ) {
      if (requestReviewState.individualReviewState != null) {
        setBannerMessage(context.getString(R.string.ConversationFragment__review_banner_body))
        setBannerRecipients(requestReviewState.individualReviewState.target, requestReviewState.individualReviewState.firstDuplicate)
        setOnClickListener { listener?.onRequestReviewIndividual(requestReviewState.individualReviewState.target.id) }
      } else if (requestReviewState.groupReviewState != null) {
        setBannerMessage(context.getString(R.string.ConversationFragment__d_group_members_have_the_same_name, requestReviewState.groupReviewState.count))
        setBannerRecipients(requestReviewState.groupReviewState.target, requestReviewState.groupReviewState.firstDuplicate)
        setOnClickListener { listener?.onReviewGroupMembers(requestReviewState.groupReviewState.groupId) }
      }

      setOnHideListener {
        clearRequestReview()
        listener?.onDismissReview()
        true
      }
    }
  }

  fun clearRequestReview() {
    hide(reviewBannerStub)
  }

  fun showVoiceNotePlayer(state: VoiceNotePlayerView.State, voiceNotePlayerViewListener: VoiceNotePlayerView.Listener) {
    show(
      stub = voiceNotePlayerStub
    ) {
      val playerView: VoiceNotePlayerView = findViewById(R.id.voice_note_player_view)
      playerView.listener = voiceNotePlayerViewListener
      playerView.setState(state)
    }
  }

  fun clearVoiceNotePlayer() {
    hide(voiceNotePlayerStub)
  }

  private fun <V : View> show(stub: Stub<V>, bind: V.() -> Unit = {}) {
    TransitionManager.beginDelayedTransition(this, Slide(Gravity.TOP))
    stub.get().bind()
    stub.get().visible = true
  }

  private fun hide(stub: Stub<*>) {
    if (!stub.isVisible) {
      return
    }

    val slideTransition = Slide(Gravity.TOP)
    val changeTransition = ChangeBounds().apply {
      if (unverifiedBannerStub.isVisible) {
        addTarget(unverifiedBannerStub.get())
      }

      if (reviewBannerStub.isVisible) {
        addTarget(reviewBannerStub.get())
      }
    }

    val transition = TransitionSet().apply {
      addTransition(slideTransition)
      addTransition(changeTransition)
      addListener(
        onEnd = {
          layoutParams = layoutParams.apply { height = LayoutParams.WRAP_CONTENT }
        }
      )
    }

    layoutParams = layoutParams.apply { height = this@ConversationBannerView.height }
    TransitionManager.beginDelayedTransition(this, transition)
    stub.get().visible = false
  }

  interface Listener {
    fun updateAppAction()
    fun reRegisterAction()
    fun reviewJoinRequestsAction()
    fun gv1SuggestionsAction(actionId: Int)
    fun changeBubbleSettingAction(disableSetting: Boolean)
    fun onUnverifiedBannerClicked(unverifiedIdentities: List<IdentityRecord>)
    fun onUnverifiedBannerDismissed(unverifiedIdentities: List<IdentityRecord>)
    fun onRequestReviewIndividual(recipientId: RecipientId)
    fun onReviewGroupMembers(groupId: GroupId.V2)
    fun onDismissReview()
  }
}
