package com.xxmrk888ytxx.portal.data.broadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.portal.PortalApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WatchDogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        fastDebugLog("WatchDog alarm received: $action")

        val app = context.applicationContext as? PortalApp ?: return
        val appComponent = app.appComponent

        // 1. Ensure keep-alive service is active
        appComponent.keepAliveManager.ensureKeepAliveRunning()

        // 2. Reschedule next tick if setting is enabled
        CoroutineScope(Dispatchers.IO).launch {
            val settings = appComponent.settingsRepository.portalSettings.first()
            if (!settings.isWatchDogEnabled) return@launch

            when (action) {
                ACTION_WATCH_DOG_FAST -> {
                    if (settings.isWatchDogFastCheckEnabled) {
                        appComponent.watchDogAlarmController.scheduleFastCheck(FAST_CHECK_INTERVAL_MILLIS)
                    }
                }
                ACTION_WATCH_DOG_ALARM_CLOCK -> {
                    if (settings.isWatchDogAlarmClockEnabled) {
                        appComponent.watchDogAlarmController.scheduleAlarmClockCheck(ALARM_CLOCK_INTERVAL_MILLIS)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_WATCH_DOG_FAST = "com.xxmrk888ytxx.portal.ACTION_WATCH_DOG_FAST"
        const val ACTION_WATCH_DOG_ALARM_CLOCK = "com.xxmrk888ytxx.portal.ACTION_WATCH_DOG_ALARM_CLOCK"

        const val FAST_CHECK_INTERVAL_MILLIS = 15_000L
        const val ALARM_CLOCK_INTERVAL_MILLIS = 30_000L
    }
}
