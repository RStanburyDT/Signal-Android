package org.thoughtcrime.securesms.ryan.stories.viewer.post

import android.graphics.Typeface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.signal.core.util.Base64
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.ryan.database.model.databaseprotos.StoryTextPost
import org.thoughtcrime.securesms.ryan.stories.viewer.page.StoryPost
import org.thoughtcrime.securesms.ryan.util.rx.RxStore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

class StoryPostViewModel(private val repository: StoryTextPostRepository) : ViewModel() {

  companion object {
    val TAG = Log.tag(StoryPostViewModel::class.java)
  }

  private val store: RxStore<StoryPostState> = RxStore(StoryPostState.None())
  private val disposables = CompositeDisposable()

  val state = store.stateFlowable.observeOn(AndroidSchedulers.mainThread())

  override fun onCleared() {
    store.dispose()
    disposables.clear()
  }

  fun onPostContentChanged(storyPostContent: StoryPost.Content) {
    disposables.clear()

    when (storyPostContent) {
      is StoryPost.Content.AttachmentContent -> {
        if (storyPostContent.uri == null) {
          store.update { StoryPostState.None() }
        } else if (storyPostContent.isVideo()) {
          store.update {
            val shouldSkipTransform = storyPostContent.attachment.transformProperties?.shouldSkipTransform() == true

            val clipStart: Duration = if (shouldSkipTransform) {
              0L.microseconds
            } else {
              storyPostContent.attachment.transformProperties?.videoTrimStartTimeUs?.microseconds ?: 0L.microseconds
            }

            val clipEnd: Duration = if (shouldSkipTransform) {
              0L.microseconds
            } else {
              storyPostContent.attachment.transformProperties?.videoTrimEndTimeUs?.microseconds ?: 0L.microseconds
            }

            StoryPostState.VideoPost(
              videoUri = storyPostContent.uri,
              size = storyPostContent.attachment.size,
              clipStart = clipStart,
              clipEnd = clipEnd,
              blurHash = storyPostContent.attachment.blurHash
            )
          }
        } else {
          store.update { StoryPostState.ImagePost(storyPostContent.uri, storyPostContent.attachment.blurHash) }
        }
      }
      is StoryPost.Content.TextContent -> {
        loadTextContent(storyPostContent.recordId)
      }
    }
  }

  private fun loadTextContent(recordId: Long) {
    val typeface = repository.getTypeface(recordId)
      .doOnError { Log.w(TAG, "Failed to get typeface. Rendering with default.", it) }
      .onErrorReturn { Typeface.DEFAULT }

    disposables += Single.zip(typeface, repository.getRecord(recordId), ::Pair).subscribeBy(
      onSuccess = { (t, record) ->
        val text: StoryTextPost = if (record.body.isNotEmpty()) {
          StoryTextPost.ADAPTER.decode(Base64.decode(record.body))
        } else {
          Log.w(TAG, "Failed to decode empty story text post body.")
          store.update {
            StoryPostState.TextPost(
              loadState = StoryPostState.LoadState.FAILED
            )
          }
          return@subscribeBy
        }

        val linkPreview = record.linkPreviews.firstOrNull()

        store.update {
          StoryPostState.TextPost(
            storyTextPostId = record.id,
            storyTextPost = text,
            linkPreview = linkPreview,
            typeface = t,
            bodyRanges = record.messageRanges,
            loadState = StoryPostState.LoadState.LOADED
          )
        }
      },
      onError = {
        Log.d(TAG, "Couldn't load text post", it)
        store.update {
          StoryPostState.TextPost(
            loadState = StoryPostState.LoadState.FAILED
          )
        }
      }
    )
  }

  class Factory(private val repository: StoryTextPostRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(StoryPostViewModel(repository)) as T
    }
  }
}
