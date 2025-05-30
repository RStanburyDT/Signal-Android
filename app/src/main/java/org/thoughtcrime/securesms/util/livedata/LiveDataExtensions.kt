package org.thoughtcrime.securesms.ryan.util.livedata

import androidx.lifecycle.LiveData

fun <T, R> LiveData<T>.distinctUntilChanged(selector: (T) -> R): LiveData<T> {
  return LiveDataUtil.distinctUntilChanged(this, selector)
}
