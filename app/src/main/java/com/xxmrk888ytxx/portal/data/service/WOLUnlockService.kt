package com.xxmrk888ytxx.portal.data.service

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import com.xxmrk888ytxx.coreandroid.buildForegroundServiceNotificationChannel
import com.xxmrk888ytxx.coreandroid.buildNotification
import com.xxmrk888ytxx.portal.R
import com.xxmrk888ytxx.portal.domain.ActiveUnlockTracker
import com.xxmrk888ytxx.portal.domain.BluetoothDeviceRepository
import com.xxmrk888ytxx.portal.domain.DeviceUnlockManager
import com.xxmrk888ytxx.portal.domain.SettingsRepository
import com.xxmrk888ytxx.portal.domain.UnlockRequestHandler
import com.xxmrk888ytxx.portal.domain.WakeLockManager
import com.xxmrk888ytxx.portal.domain.WOLManager
import com.xxmrk888ytxx.portal.domain.WifiDeviceRepository
import javax.inject.Inject

class WOLUnlockService @Inject constructor(
    wifiDeviceRepository: WifiDeviceRepository,
    deviceUnlockManager: DeviceUnlockManager,
    bluetoothDeviceRepository: BluetoothDeviceRepository,
    wolManager: WOLManager,
    activeUnlockTracker: ActiveUnlockTracker,
    wakeLockManager: WakeLockManager,
    settingsRepository: SettingsRepository,
    unlockRequestHandler: dagger.Lazy<UnlockRequestHandler>
) : ClientUnlockService(
    wolManager = wolManager,
    deviceUnlockManager = deviceUnlockManager,
    wifiDeviceRepository = wifiDeviceRepository,
    bluetoothDeviceRepository = bluetoothDeviceRepository,
    activeUnlockTracker = activeUnlockTracker,
    wakeLockManager = wakeLockManager,
    settingsRepository = settingsRepository,
    unlockRequestHandler = unlockRequestHandler
) {
    override val channelId: String
        get() = WOL_UNLOCK_CHANNEL_ID
    override val notificationId: Int
        get() = NOTIFICATION_ID
    override val action: String
        get() = WOL_UNLOCK_ACTION
    override val notification: Notification
        get() = buildNotification(WOL_UNLOCK_CHANNEL_ID) {
            setContentTitle(getString(R.string.unlocking_the_device_wol))
            setSmallIcon(com.xxmrk888ytxx.unlockservice.R.drawable.portal)
            setContentText(getString(R.string.please_wait_a_moment))
            setProgress(0, 0, true)
            setOngoing(true)
        }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        buildForegroundServiceNotificationChannel(
            WOL_UNLOCK_CHANNEL_ID,
            getString(R.string.unblocking_via_wol)
        )
    }

    companion object {
        const val WOL_UNLOCK_CHANNEL_ID = "WOL_UNLOCK_CHANNEL_ID"
        private const val NOTIFICATION_ID = 3456
        const val WOL_UNLOCK_ACTION = "com.xxmrk888ytxx.portal.WOL_UNLOCK_ACTION"
    }
}
