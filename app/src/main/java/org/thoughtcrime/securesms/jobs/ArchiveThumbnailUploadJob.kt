/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.jobs

import android.os.Build
import org.signal.core.util.logging.Log
import org.signal.protos.resumableuploads.ResumableUpload
import org.thoughtcrime.securesms.ryan.attachments.Attachment
import org.thoughtcrime.securesms.ryan.attachments.AttachmentId
import org.thoughtcrime.securesms.ryan.attachments.DatabaseAttachment
import org.thoughtcrime.securesms.ryan.attachments.PointerAttachment
import org.thoughtcrime.securesms.ryan.backup.v2.BackupRepository
import org.thoughtcrime.securesms.ryan.backup.v2.requireThumbnailMediaName
import org.thoughtcrime.securesms.ryan.database.SignalDatabase
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies
import org.thoughtcrime.securesms.ryan.jobmanager.Job
import org.thoughtcrime.securesms.ryan.jobmanager.impl.NetworkConstraint
import org.thoughtcrime.securesms.ryan.jobs.protos.ArchiveThumbnailUploadJobData
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.mms.DecryptableStreamUriLoader.DecryptableUri
import org.thoughtcrime.securesms.ryan.net.SignalNetwork
import org.thoughtcrime.securesms.ryan.util.ImageCompressionUtil
import org.thoughtcrime.securesms.ryan.util.MediaUtil
import org.whispersystems.signalservice.api.NetworkResult
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentStream
import org.whispersystems.signalservice.internal.push.http.ResumableUploadSpec
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Optional
import kotlin.time.Duration.Companion.days

/**
 * Uploads a thumbnail for the specified attachment to the archive service, if possible.
 */
class ArchiveThumbnailUploadJob private constructor(
  params: Parameters,
  val attachmentId: AttachmentId
) : Job(params) {

  companion object {
    const val KEY = "ArchiveThumbnailUploadJob"
    private val TAG = Log.tag(ArchiveThumbnailUploadJob::class.java)

    fun enqueueIfNecessary(attachmentId: AttachmentId) {
      if (SignalStore.backup.backsUpMedia) {
        AppDependencies.jobManager.add(ArchiveThumbnailUploadJob(attachmentId))
      }
    }
  }

  private constructor(attachmentId: AttachmentId) : this(
    Parameters.Builder()
      .setQueue("ArchiveThumbnailUploadJob")
      .addConstraint(NetworkConstraint.KEY)
      .setLifespan(1.days.inWholeMilliseconds)
      .setMaxAttempts(Parameters.UNLIMITED)
      .build(),
    attachmentId
  )

  override fun serialize(): ByteArray {
    return ArchiveThumbnailUploadJobData(
      attachmentId = attachmentId.id
    ).encode()
  }

  override fun getFactoryKey(): String = KEY

  override fun run(): Result {
    val attachment = SignalDatabase.attachments.getAttachment(attachmentId)
    if (attachment == null) {
      Log.w(TAG, "$attachmentId not found, assuming this job is no longer necessary.")
      return Result.success()
    }

    if (attachment.remoteDigest == null) {
      Log.w(TAG, "$attachmentId was never uploaded! Cannot proceed.")
      return Result.success()
    }

    // TODO [backups] Decide if we fail a job when associated attachment not already backed up
    // TODO [backups] Determine if we actually need to upload or are reusing a thumbnail from another attachment

    val thumbnailResult = generateThumbnailIfPossible(attachment)
    if (thumbnailResult == null) {
      Log.w(TAG, "Unable to generate a thumbnail result for $attachmentId")
      return Result.success()
    }

    val mediaRootBackupKey = SignalStore.backup.mediaRootBackupKey

    val specResult = BackupRepository
      .getAttachmentUploadForm()
      .then { form ->
        SignalNetwork.attachments.getResumableUploadSpec(
          key = mediaRootBackupKey.deriveThumbnailTransitKey(attachment.requireThumbnailMediaName()),
          iv = attachment.remoteIv!!,
          uploadForm = form
        )
      }

    val resumableUpload = when (specResult) {
      is NetworkResult.Success -> {
        Log.d(TAG, "Got an upload spec!")
        specResult.result.toProto()
      }
      is NetworkResult.ApplicationError -> {
        Log.w(TAG, "Failed to get an upload spec due to an application error. Retrying.", specResult.throwable)
        return Result.retry(defaultBackoff())
      }
      is NetworkResult.NetworkError -> {
        Log.w(TAG, "Encountered a transient network error when getting upload spec. Retrying.")
        return Result.retry(defaultBackoff())
      }
      is NetworkResult.StatusCodeError -> {
        Log.w(TAG, "Failed to get an upload spec with status code ${specResult.code}")
        return Result.retry(defaultBackoff())
      }
    }

    val stream = buildSignalServiceAttachmentStream(thumbnailResult, resumableUpload)

    val attachmentPointer: Attachment = try {
      val pointer = AppDependencies.signalServiceMessageSender.uploadAttachment(stream)
      PointerAttachment.forPointer(Optional.of(pointer)).get()
    } catch (e: IOException) {
      Log.w(TAG, "Failed to upload attachment", e)
      return Result.retry(defaultBackoff())
    }

    val mediaSecrets = mediaRootBackupKey.deriveMediaSecrets(attachment.requireThumbnailMediaName())

    return when (val result = BackupRepository.copyThumbnailToArchive(attachmentPointer, attachment)) {
      is NetworkResult.Success -> {
        // save attachment thumbnail
        SignalDatabase.attachments.finalizeAttachmentThumbnailAfterUpload(
          attachmentId = attachmentId,
          attachmentDigest = attachment.remoteDigest!!,
          data = thumbnailResult.data
        )

        Log.d(TAG, "Successfully archived thumbnail for $attachmentId mediaName=${attachment.requireThumbnailMediaName()}")
        Result.success()
      }
      is NetworkResult.NetworkError -> {
        Log.w(TAG, "Hit a network error when trying to archive thumbnail for $attachmentId", result.exception)
        Result.retry(defaultBackoff())
      }
      is NetworkResult.StatusCodeError -> {
        Log.w(TAG, "Hit a status code error of ${result.code} when trying to archive thumbnail for $attachmentId")
        Result.retry(defaultBackoff())
      }
      is NetworkResult.ApplicationError -> Result.fatalFailure(RuntimeException(result.throwable))
    }
  }

  override fun onFailure() {
  }

  private fun generateThumbnailIfPossible(attachment: DatabaseAttachment): ImageCompressionUtil.Result? {
    val uri: DecryptableUri = attachment.uri?.let { DecryptableUri(it) } ?: return null

    return if (MediaUtil.isImageType(attachment.contentType)) {
      ImageCompressionUtil.compress(context, attachment.contentType ?: "", uri, 256, 50)
    } else if (Build.VERSION.SDK_INT >= 23 && MediaUtil.isVideoType(attachment.contentType)) {
      MediaUtil.getVideoThumbnail(context, attachment.uri)?.let {
        ImageCompressionUtil.compress(context, attachment.contentType ?: "", uri, 256, 50)
      }
    } else {
      null
    }
  }

  private fun buildSignalServiceAttachmentStream(result: ImageCompressionUtil.Result, uploadSpec: ResumableUpload): SignalServiceAttachmentStream {
    return SignalServiceAttachment.newStreamBuilder()
      .withStream(ByteArrayInputStream(result.data))
      .withContentType(result.mimeType)
      .withLength(result.data.size.toLong())
      .withWidth(result.width)
      .withHeight(result.height)
      .withUploadTimestamp(System.currentTimeMillis())
      .withResumableUploadSpec(ResumableUploadSpec.from(uploadSpec))
      .build()
  }

  class Factory : Job.Factory<ArchiveThumbnailUploadJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): ArchiveThumbnailUploadJob {
      val data = ArchiveThumbnailUploadJobData.ADAPTER.decode(serializedData!!)
      return ArchiveThumbnailUploadJob(
        params = parameters,
        attachmentId = AttachmentId(data.attachmentId)
      )
    }
  }
}
