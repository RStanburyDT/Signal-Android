package org.thoughtcrime.securesms.ryan.mediasend.v2.gallery

import org.thoughtcrime.securesms.ryan.util.adapter.mapping.MappingModel

data class MediaGalleryState(
  val bucketId: String?,
  val bucketTitle: String?,
  val items: List<MappingModel<*>> = listOf()
)
