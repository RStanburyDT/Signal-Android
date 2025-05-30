/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.ryan.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.thoughtcrime.securesms.ryan.crash.CrashConfig
import org.thoughtcrime.securesms.ryan.database.LogDatabase
import org.thoughtcrime.securesms.ryan.dependencies.AppDependencies
import org.thoughtcrime.securesms.ryan.keyvalue.SignalStore
import org.thoughtcrime.securesms.ryan.notifications.DeviceSpecificNotificationConfig.ShowCondition
import org.thoughtcrime.securesms.ryan.util.ConnectivityWarning
import org.thoughtcrime.securesms.ryan.util.NetworkUtil
import org.whispersystems.signalservice.api.websocket.WebSocketConnectionState
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

/**
 * View model for checking for various app vitals, like slow notifications and crashes.
 */
class VitalsViewModel(private val context: Application) : AndroidViewModel(context) {

  private val checkSubject = BehaviorSubject.create<Unit>()

  val vitalsState: Observable<State>

  init {
    vitalsState = checkSubject
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .throttleFirst(1, TimeUnit.MINUTES)
      .switchMapSingle {
        checkHeuristics()
      }
      .distinctUntilChanged()
      .observeOn(AndroidSchedulers.mainThread())
  }

  fun checkSlowNotificationHeuristics() {
    checkSubject.onNext(Unit)
  }

  private fun checkHeuristics(): Single<State> {
    return Single.fromCallable {
      val deviceSpecificCondition = SlowNotificationHeuristics.getDeviceSpecificShowCondition()

      if (deviceSpecificCondition == ShowCondition.ALWAYS && SlowNotificationHeuristics.shouldShowDeviceSpecificDialog()) {
        return@fromCallable State.PROMPT_SPECIFIC_BATTERY_SAVER_DIALOG
      }

      if (deviceSpecificCondition == ShowCondition.HAS_BATTERY_OPTIMIZATION_ON && SlowNotificationHeuristics.shouldShowDeviceSpecificDialog() && SlowNotificationHeuristics.isBatteryOptimizationsOn()) {
        return@fromCallable State.PROMPT_SPECIFIC_BATTERY_SAVER_DIALOG
      }

      if (deviceSpecificCondition == ShowCondition.HAS_SLOW_NOTIFICATIONS && SlowNotificationHeuristics.shouldShowDeviceSpecificDialog() && SlowNotificationHeuristics.isHavingDelayedNotifications()) {
        return@fromCallable State.PROMPT_SPECIFIC_BATTERY_SAVER_DIALOG
      }

      if (SlowNotificationHeuristics.isHavingDelayedNotifications() && SlowNotificationHeuristics.shouldPromptBatterySaver()) {
        return@fromCallable State.PROMPT_GENERAL_BATTERY_SAVER_DIALOG
      }

      if (SlowNotificationHeuristics.isHavingDelayedNotifications() && SlowNotificationHeuristics.shouldPromptUserForDelayedNotificationLogs()) {
        return@fromCallable State.PROMPT_DEBUGLOGS_FOR_NOTIFICATIONS
      }

      val timeSinceLastConnection = System.currentTimeMillis() - SignalStore.misc.lastWebSocketConnectTime
      val timeSinceLastConnectionWarning = System.currentTimeMillis() - SignalStore.misc.lastConnectivityWarningTime
      val connectedToWebSocket = AppDependencies.webSocketObserver.value == WebSocketConnectionState.CONNECTED

      if (ConnectivityWarning.isEnabled && timeSinceLastConnection > ConnectivityWarning.threshold && timeSinceLastConnectionWarning > 14.days.inWholeMilliseconds && NetworkUtil.isConnected(context) && SignalStore.account.isRegistered && !connectedToWebSocket) {
        return@fromCallable if (ConnectivityWarning.isDebugPromptEnabled) {
          State.PROMPT_DEBUGLOGS_FOR_CONNECTIVITY_WARNING
        } else {
          State.PROMPT_CONNECTIVITY_WARNING
        }
      }

      if (LogDatabase.getInstance(context).crashes.anyMatch(patterns = CrashConfig.patterns, promptThreshold = System.currentTimeMillis() - 14.days.inWholeMilliseconds)) {
        val timeSinceLastPrompt = System.currentTimeMillis() - SignalStore.uiHints.lastCrashPrompt

        if (timeSinceLastPrompt > 1.days.inWholeMilliseconds) {
          return@fromCallable State.PROMPT_DEBUGLOGS_FOR_CRASH
        }
      }

      return@fromCallable State.NONE
    }.subscribeOn(Schedulers.io())
  }

  enum class State {
    NONE,
    PROMPT_SPECIFIC_BATTERY_SAVER_DIALOG,
    PROMPT_GENERAL_BATTERY_SAVER_DIALOG,
    PROMPT_DEBUGLOGS_FOR_NOTIFICATIONS,
    PROMPT_DEBUGLOGS_FOR_CRASH,
    PROMPT_CONNECTIVITY_WARNING,
    PROMPT_DEBUGLOGS_FOR_CONNECTIVITY_WARNING
  }
}
