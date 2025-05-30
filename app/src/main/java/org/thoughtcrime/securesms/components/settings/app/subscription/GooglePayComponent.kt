package org.thoughtcrime.securesms.ryan.components.settings.app.subscription

import android.content.Intent
import android.os.Parcelable
import io.reactivex.rxjava3.subjects.Subject
import kotlinx.parcelize.Parcelize

interface GooglePayComponent {
  val googlePayRepository: GooglePayRepository
  val googlePayResultPublisher: Subject<GooglePayResult>

  @Parcelize
  class GooglePayResult(val requestCode: Int, val resultCode: Int, val data: Intent?) : Parcelable
}
