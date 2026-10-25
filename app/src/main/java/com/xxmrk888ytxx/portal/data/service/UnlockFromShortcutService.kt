package com.xxmrk888ytxx.portal.data.service

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import com.xxmrk888ytxx.coreandroid.buildForegroundServiceNotificationChannel
import com.xxmrk888ytxx.coreandroid.buildNotification
import com.xxmrk888ytxx.coreandroid.getActiveAppIconBitmap
import com.xxmrk888ytxx.portal.R
import com.xxmrk888ytxx.portal.domain.ActiveUnlockTracker
import com.xxmrk888ytxx.portal.domain.AppIconManager
import com.xxmrk888ytxx.portal.domain.BluetoothDeviceRepository
import com.xxmrk888ytxx.portal.domain.DeviceUnlockManager
import com.xxmrk888ytxx.portal.domain.SettingsRepository
import com.xxmrk888ytxx.portal.domain.UnlockRequestHandler
import com.xxmrk888ytxx.portal.domain.WakeLockManager
import com.xxmrk888ytxx.portal.domain.WOLManager
import com.xxmrk888ytxx.portal.domain.WifiDeviceRepository
import javax.inject.Inject

class UnlockFromShortcutService @Inject constructor(
    wifiDeviceRepository: WifiDeviceRepository,
    deviceUnlockManager: DeviceUnlockManager,
    bluetoothDeviceRepository: BluetoothDeviceRepository,
    wolManager: WOLManager,
    activeUnlockTracker: ActiveUnlockTracker,
    wakeLockManager: WakeLockManager,
    settingsRepository: SettingsRepository,
    unlockRequestHandler: dagger.Lazy<UnlockRequestHandler>,
    appIconManager: AppIconManager
) : ClientUnlockService(
    wolManager = wolManager,
    deviceUnlockManager = deviceUnlockManager,
    wifiDeviceRepository = wifiDeviceRepository,
    bluetoothDeviceRepository = bluetoothDeviceRepository,
    activeUnlockTracker = activeUnlockTracker,
    wakeLockManager = wakeLockManager,
    settingsRepository = settingsRepository,
    unlockRequestHandler = unlockRequestHandler,
    appIconManager = appIconManager
) {
    override val channelId: String
        get() = SHORTCUT_UNLOCK_SERVICE_CHANNEL

    override val notificationId: Int
        get() = NOTIFICATION_ID

    override val action: String
        get() = SHORTCUT_UNLOCK_ACTION

    override val notification: Notification
        get() = buildNotification(SHORTCUT_UNLOCK_SERVICE_CHANNEL) {
            setContentTitle(getString(R.string.unlocking_the_device))
            val smallIconRes = appIconManager?.getAppIconSmallIconRes()
                ?: com.xxmrk888ytxx.unlockservice.R.drawable.portal
            setSmallIcon(smallIconRes)
            (appIconManager?.getAppIconBitmap() ?: getActiveAppIconBitmap())?.let { setLargeIcon(it) }
            setContentText(getString(R.string.please_wait_a_moment))
            setProgress(0, 0, true)
            setOngoing(true)
        }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        buildForegroundServiceNotificationChannel(
            SHORTCUT_UNLOCK_SERVICE_CHANNEL,
            getString(R.string.notification_during_unlocking_via_shortcut)
        )
    }

    companion object {
        const val SHORTCUT_UNLOCK_SERVICE_CHANNEL = "SHORTCUT_UNLOCK_SERVICE"
        private const val NOTIFICATION_ID = 4653
        const val SHORTCUT_UNLOCK_ACTION = "com.xxmrk888ytxx.portal.SHORTCUT_UNLOCK_ACTION"
    }
}
