package com.xxmrk888ytxx.portal.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.portal.data.broadcastReceiver.WatchDogReceiver
import com.xxmrk888ytxx.portal.domain.WatchDogAlarmController
import com.xxmrk888ytxx.portal.view.mainActivity.MainActivity
import javax.inject.Inject

class WatchDogAlarmControllerImpl @Inject constructor(
    private val context: Context
) : WatchDogAlarmController {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(AlarmManager::class.java)

    override fun scheduleFastCheck(intervalMillis: Long) {
        try {
            val am = alarmManager ?: return
            val triggerTime = SystemClock.elapsedRealtime() + intervalMillis
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    createFastPendingIntent()
                )
            } else {
                am.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    createFastPendingIntent()
                )
            }
            fastDebugLog("Scheduled WatchDog Fast Check in ${intervalMillis / 1000}s")
        } catch (e: Exception) {
            fastDebugLog("Failed to schedule WatchDog fast check: ${e.message}")
        }
    }

    override fun scheduleAlarmClockCheck(intervalMillis: Long) {
        try {
            val am = alarmManager ?: return
            val triggerTimeMillis = System.currentTimeMillis() + intervalMillis
            val showIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTimeMillis, showIntent),
                createAlarmClockPendingIntent()
            )
            fastDebugLog("Scheduled WatchDog AlarmClock Check in ${intervalMillis / 1000}s")
        } catch (e: Exception) {
            fastDebugLog("Failed to schedule WatchDog alarm clock check: ${e.message}")
        }
    }

    override fun cancelFastCheck() {
        try {
            alarmManager?.cancel(createFastPendingIntent())
            fastDebugLog("Cancelled WatchDog Fast Check")
        } catch (e: Exception) {
            fastDebugLog("Failed to cancel WatchDog fast check: ${e.message}")
        }
    }

    override fun cancelAlarmClockCheck() {
        try {
            alarmManager?.cancel(createAlarmClockPendingIntent())
            fastDebugLog("Cancelled WatchDog AlarmClock Check")
        } catch (e: Exception) {
            fastDebugLog("Failed to cancel WatchDog alarm clock check: ${e.message}")
        }
    }

    override fun cancelAll() {
        cancelFastCheck()
        cancelAlarmClockCheck()
    }

    private fun createFastPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE_FAST,
        Intent(context, WatchDogReceiver::class.java).apply {
            action = WatchDogReceiver.ACTION_WATCH_DOG_FAST
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createAlarmClockPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE_ALARM_CLOCK,
        Intent(context, WatchDogReceiver::class.java).apply {
            action = WatchDogReceiver.ACTION_WATCH_DOG_ALARM_CLOCK
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private companion object {
        const val REQUEST_CODE_FAST = 888_301
        const val REQUEST_CODE_ALARM_CLOCK = 888_302
    }
}
