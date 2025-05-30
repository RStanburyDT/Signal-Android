package org.thoughtcrime.securesms.ryan.stories

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import org.signal.core.util.concurrent.ListenableFuture
import org.signal.core.util.concurrent.SettableFuture
import org.signal.core.util.isAbsent
import org.thoughtcrime.securesms.ryan.R
import org.thoughtcrime.securesms.ryan.components.ThumbnailView
import org.thoughtcrime.securesms.ryan.databinding.StoriesTextPostLinkPreviewBinding
import org.thoughtcrime.securesms.ryan.linkpreview.LinkPreview
import org.thoughtcrime.securesms.ryan.linkpreview.LinkPreviewState
import org.thoughtcrime.securesms.ryan.linkpreview.LinkPreviewUtil
import org.thoughtcrime.securesms.ryan.mms.ImageSlide
import org.thoughtcrime.securesms.ryan.mms.Slide
import org.thoughtcrime.securesms.ryan.util.views.Stub
import org.thoughtcrime.securesms.ryan.util.visible
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

class StoryLinkPreviewView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

  init {
    inflate(context, R.layout.stories_text_post_link_preview, this)
  }

  private val binding = StoriesTextPostLinkPreviewBinding.bind(this)
  private val spinnerStub = Stub<View>(binding.loadingSpinner)

  init {
    binding.linkPreviewImage.isClickable = false
    binding.linkPreviewLarge.isClickable = false
  }

  private fun getThumbnailTarget(useLargeThumbnail: Boolean): ThumbnailView {
    return if (useLargeThumbnail) binding.linkPreviewLarge else binding.linkPreviewImage
  }

  fun getThumbnailViewWidth(useLargeThumbnail: Boolean): Int {
    return getThumbnailTarget(useLargeThumbnail).measuredWidth
  }

  fun getThumbnailViewHeight(useLargeThumbnail: Boolean): Int {
    return getThumbnailTarget(useLargeThumbnail).measuredHeight
  }

  fun setThumbnailDrawable(drawable: Drawable?, useLargeThumbnail: Boolean) {
    val image = getThumbnailTarget(useLargeThumbnail)
    image.setImageDrawable(Glide.with(this), drawable)
  }

  fun bind(linkPreview: LinkPreview?, hiddenVisibility: Int = View.INVISIBLE, useLargeThumbnail: Boolean, loadThumbnail: Boolean = true): ListenableFuture<Boolean> {
    var future: ListenableFuture<Boolean>? = null

    if (isPartialLinkPreview(linkPreview)) {
      future = bindPartialLinkPreview(linkPreview!!)
    } else if (linkPreview != null) {
      future = bindFullLinkPreview(linkPreview, useLargeThumbnail, loadThumbnail)
    } else {
      visibility = hiddenVisibility
    }

    return future ?: SettableFuture(false)
  }

  fun bind(linkPreviewState: LinkPreviewState, hiddenVisibility: Int = View.INVISIBLE, useLargeThumbnail: Boolean) {
    val linkPreview: LinkPreview? = linkPreviewState.linkPreview.orElseGet {
      linkPreviewState.activeUrlForError?.let {
        LinkPreview(it, LinkPreviewUtil.getTopLevelDomain(it) ?: it, null, -1L, null)
      }
    }

    bind(linkPreview, hiddenVisibility, useLargeThumbnail)

    spinnerStub.get().visible = linkPreviewState.isLoading
    if (linkPreviewState.isLoading) {
      visible = true
    }
  }

  private fun isPartialLinkPreview(linkPreview: LinkPreview?): Boolean {
    return linkPreview != null &&
      TextUtils.isEmpty(linkPreview.description) &&
      linkPreview.thumbnail.isAbsent() &&
      linkPreview.attachmentId == null
  }

  private fun bindPartialLinkPreview(linkPreview: LinkPreview): ListenableFuture<Boolean> {
    visibility = View.VISIBLE
    binding.linkPreviewCard.visible = false
    binding.linkPreviewPlaceholderCard.visible = true

    binding.linkPreviewPlaceholderTitle.text = Uri.parse(linkPreview.url).host

    return SettableFuture(false)
  }

  private fun bindFullLinkPreview(linkPreview: LinkPreview, useLargeThumbnail: Boolean, loadThumbnail: Boolean): ListenableFuture<Boolean> {
    visibility = View.VISIBLE
    binding.linkPreviewCard.visible = true
    binding.linkPreviewPlaceholderCard.visible = false

    val image = getThumbnailTarget(useLargeThumbnail)
    val notImage = getThumbnailTarget(!useLargeThumbnail)
    var future: ListenableFuture<Boolean>? = null

    notImage.visible = false

    val imageSlide: Slide? = linkPreview.thumbnail.map { ImageSlide(it) }.orElse(null)
    if (imageSlide != null) {
      if (loadThumbnail) {
        future = image.setImageResource(
          Glide.with(this),
          imageSlide,
          false,
          false
        )
      }

      image.visible = true
      binding.linkPreviewFallbackIcon.visible = false
    } else {
      image.visible = false
      binding.linkPreviewFallbackIcon.visible = true
    }

    binding.linkPreviewTitle.text = linkPreview.title
    binding.linkPreviewDescription.text = linkPreview.description
    binding.linkPreviewDescription.visible = linkPreview.description.isNotEmpty()

    formatUrl(linkPreview)

    return future ?: SettableFuture(false)
  }

  private fun formatUrl(linkPreview: LinkPreview) {
    val domain: String? = LinkPreviewUtil.getTopLevelDomain(linkPreview.url)

    if (linkPreview.title == domain) {
      binding.linkPreviewUrl.visibility = View.GONE
      return
    }

    if (domain != null && linkPreview.date > 0) {
      binding.linkPreviewUrl.text = context.getString(R.string.LinkPreviewView_domain_date, domain, formatDate(linkPreview.date))
      binding.linkPreviewUrl.visibility = View.VISIBLE
    } else if (domain != null) {
      binding.linkPreviewUrl.text = domain
      binding.linkPreviewUrl.visibility = View.VISIBLE
    } else if (linkPreview.date > 0) {
      binding.linkPreviewUrl.text = formatDate(linkPreview.date)
      binding.linkPreviewUrl.visibility = View.VISIBLE
    } else {
      binding.linkPreviewUrl.visibility = View.GONE
    }
  }

  fun setOnCloseClickListener(onClickListener: OnClickListener?) {
    binding.linkPreviewClose.setOnClickListener(onClickListener)
  }

  fun setOnPreviewClickListener(onClickListener: OnClickListener?) {
    binding.linkPreviewCard.setOnClickListener { onClickListener?.onClick(this) }
    binding.linkPreviewPlaceholderCard.setOnClickListener { onClickListener?.onClick(this) }
  }

  fun setCanClose(canClose: Boolean) {
    binding.linkPreviewClose.visible = canClose
  }

  private fun formatDate(date: Long): String? {
    val dateFormat: DateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(date)
  }
}
