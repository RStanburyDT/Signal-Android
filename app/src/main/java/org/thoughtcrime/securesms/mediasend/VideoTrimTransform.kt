package org.thoughtcrime.securesms.ryan.mediasend

import android.content.Context
import androidx.annotation.WorkerThread
import org.thoughtcrime.securesms.ryan.database.AttachmentTable.TransformProperties
import org.thoughtcrime.securesms.ryan.mediasend.v2.videos.VideoTrimData
import org.thoughtcrime.securesms.ryan.mms.SentMediaQuality
import java.util.Optional

class VideoTrimTransform(private val data: VideoTrimData) : MediaTransform {
  @WorkerThread
  override fun transform(context: Context, media: Media): Media {
    return Media(
      media.uri,
      media.contentType,
      media.date,
      media.width,
      media.height,
      media.size,
      media.duration,
      media.isBorderless,
      media.isVideoGif,
      media.bucketId,
      media.caption,
      Optional.of(TransformProperties(false, data.isDurationEdited, data.startTimeUs, data.endTimeUs, SentMediaQuality.STANDARD.code, false)),
      media.fileName
    )
  }
}
