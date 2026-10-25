package com.xxmrk888ytxx.portal.data.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.content.getSystemService
import com.xxmrk888ytxx.coreandroid.buildNotification
import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.coreandroid.getActiveAppIconBitmap
import com.xxmrk888ytxx.portal.R
import com.xxmrk888ytxx.portal.data.service.model.ClientUnlockServiceParams
import com.xxmrk888ytxx.portal.domain.ActiveUnlockTracker
import com.xxmrk888ytxx.portal.domain.AppIconManager
import com.xxmrk888ytxx.portal.domain.BluetoothDeviceRepository
import com.xxmrk888ytxx.portal.domain.DeviceUnlockManager
import com.xxmrk888ytxx.portal.domain.MdnsManager
import com.xxmrk888ytxx.portal.domain.SettingsRepository
import com.xxmrk888ytxx.portal.domain.UnlockRequestHandler
import com.xxmrk888ytxx.portal.domain.WakeLockManager
import com.xxmrk888ytxx.portal.domain.WOLManager
import com.xxmrk888ytxx.portal.domain.WifiDeviceRepository
import com.xxmrk888ytxx.portal.domain.model.BluetoothDevice
import com.xxmrk888ytxx.portal.domain.model.WifiDevice
import com.xxmrk888ytxx.portal.utils.getParsableExtraCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

abstract class ClientUnlockService(
    private val wolManager: WOLManager,
    private val deviceUnlockManager: DeviceUnlockManager,
    private val wifiDeviceRepository: WifiDeviceRepository,
    private val bluetoothDeviceRepository: BluetoothDeviceRepository,
    private val activeUnlockTracker: ActiveUnlockTracker,
    private val wakeLockManager: WakeLockManager,
    private val settingsRepository: SettingsRepository,
    private val unlockRequestHandler: dagger.Lazy<UnlockRequestHandler>,
    protected val appIconManager: AppIconManager? = null,
    protected val mdnsManager: MdnsManager,
) : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentUnlockJob: Job? = null
    private var currentRunningClientId: String? = null

    protected abstract val channelId: String
    protected abstract val notificationId: Int
    protected abstract val notification: Notification
    protected abstract val action: String

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_UNLOCK) {
            fastDebugLog("Cancel action received in ClientUnlockService")
            cancelNotificationDismiss(notificationId)
            currentUnlockJob?.cancel()
            currentRunningClientId = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            val notificationManager = getSystemService<NotificationManager>()
            notificationManager?.cancel(notificationId)
            stopSelf()
            return START_NOT_STICKY
        }
        val clientUnlockServiceParams = intent?.getParsableExtraCompat(
            CLIENT_UNLOCK_SERVICE_PARAMS_KEY,
            ClientUnlockServiceParams::class.java
        )
        if (intent?.action != action || clientUnlockServiceParams == null) {
            fastDebugLog("${intent?.action} != $action or clientUnlockServiceParams $clientUnlockServiceParams == null")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        cancelNotificationDismiss(notificationId)

        if (currentUnlockJob?.isActive == true) {
            if (currentRunningClientId == clientUnlockServiceParams.clientId) {
                fastDebugLog("Unlock job is already active for $currentRunningClientId, ignoring duplicate start command")
                return START_NOT_STICKY
            } else {
                fastDebugLog("Cancelling previous unlock job for $currentRunningClientId to start for ${clientUnlockServiceParams.clientId}")
                currentUnlockJob?.cancel()
            }
        }
        currentRunningClientId = clientUnlockServiceParams.clientId

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(notificationId, notification)
        }
        currentUnlockJob = serviceScope.launch {
            try {
                handleUnlock(clientUnlockServiceParams)
            } finally {
                currentRunningClientId = null
            }
        }
        currentUnlockJob?.invokeOnCompletion { stopSelf(startId) }
        return START_NOT_STICKY
    }

    private suspend fun handleUnlock(clientUnlockServiceParams: ClientUnlockServiceParams) {
        val clientId = clientUnlockServiceParams.clientId
        val timeoutMinutes = settingsRepository.portalSettings.first().type1UnlockTimeoutMinutes
        val timeoutMillis = (timeoutMinutes * 60 * 1000L).coerceAtLeast(60_000L)
        val wakeLockTag = "ClientUnlockService:$clientId"

        activeUnlockTracker.registerType1Unlock(clientId)
        wakeLockManager.acquireWakeLock(wakeLockTag)
        var unlockSucceeded = false

        val mdnsJob = serviceScope.launch {
            mdnsManager.foundedHosts.collect { hosts ->
                val discoveredHost = hosts[clientId]?.hostIp
                if (!discoveredHost.isNullOrEmpty()) {
                    fastDebugLog("Continuous mDNS discovered host for $clientId: $discoveredHost")
                    wifiDeviceRepository.updateHost(clientId, discoveredHost)
                }
            }
        }

        var matchingBtDeviceClientId: String? = null
        try {
            val wifiDevice = wifiDeviceRepository.getDeviceById(clientId).first()
            val bluetoothDevice = bluetoothDeviceRepository.getDeviceById(clientId).first()

            if (wifiDevice != null) {
                val matchingBt = bluetoothDeviceRepository.devices.first().firstOrNull { it.name == wifiDevice.deviceName }
                if (matchingBt != null) {
                    matchingBtDeviceClientId = matchingBt.clientId
                    activeUnlockTracker.registerType1Unlock(matchingBt.clientId)
                }
            } else if (bluetoothDevice != null) {
                val matchingWifi = wifiDeviceRepository.devices.first().firstOrNull { it.deviceName == bluetoothDevice.name }
                if (matchingWifi != null) {
                    activeUnlockTracker.registerType1Unlock(matchingWifi.clientId)
                }
            }

            when {
                wifiDevice != null -> {
                    val matchingBt = matchingBtDeviceClientId?.let { btId ->
                        bluetoothDeviceRepository.getDeviceById(btId).first()
                    }
                    unlockSucceeded = doUnlockWifiDevice(
                        wifiDevice = wifiDevice,
                        clientUnlockServiceParams = clientUnlockServiceParams,
                        timeoutMillis = timeoutMillis,
                        matchingBtDevice = matchingBt
                    )
                }
                bluetoothDevice != null -> {
                    unlockSucceeded = unlockBluetoothDevice(bluetoothDevice, clientUnlockServiceParams, timeoutMillis)
                }
                else -> {
                    fastDebugLog("Device where id = $clientId doesn't exist. $this Skip ")
                }
            }
        } finally {
            mdnsJob.cancel()
            matchingBtDeviceClientId?.let { activeUnlockTracker.unregisterType1Unlock(it, unlockSucceeded) }
            wakeLockManager.releaseWakeLock(wakeLockTag)
            activeUnlockTracker.unregisterType1Unlock(clientId, unlockSucceeded)
            if (!unlockSucceeded) {
                val deferredRequest = activeUnlockTracker.pollDeferredType2Request(clientId)
                    ?: matchingBtDeviceClientId?.let { activeUnlockTracker.pollDeferredType2Request(it) }
                if (deferredRequest != null) {
                    fastDebugLog("Dispatching deferred Type 2 request for $clientId after Type 1 unlock finished")
                    unlockRequestHandler.get().onNewRequest(clientId, deferredRequest)
                }
            }
        }
    }

    private suspend fun unlockBluetoothDevice(
        bluetoothDevice: BluetoothDevice,
        clientUnlockServiceParams: ClientUnlockServiceParams,
        timeoutMillis: Long
    ): Boolean {
        return doUnlock(
            clientUnlockServiceParams = clientUnlockServiceParams,
            deviceName = bluetoothDevice.name,
            timeoutMillis = timeoutMillis
        ) { tryBluetoothDeviceUnlock(bluetoothDevice, clientUnlockServiceParams) }
    }

    private suspend fun doUnlockWifiDevice(
        wifiDevice: WifiDevice,
        clientUnlockServiceParams: ClientUnlockServiceParams,
        timeoutMillis: Long,
        matchingBtDevice: BluetoothDevice? = null
    ): Boolean {
        val macAddress = wifiDevice.wolMacAddress
        if (macAddress == null) {
            fastDebugLog("Mac address is null. WOL unavailable for deviceId ${wifiDevice.clientId} ")
        }
        return doUnlock(
            clientUnlockServiceParams = clientUnlockServiceParams,
            deviceName = wifiDevice.deviceName,
            timeoutMillis = timeoutMillis
        ) {
            val wifiSuccess = tryWifiDeviceUnlock(
                wifiDevice = wifiDevice,
                macAddress = macAddress,
                clientUnlockServiceParams = clientUnlockServiceParams
            )
            if (wifiSuccess) {
                true
            } else if (matchingBtDevice != null && clientUnlockServiceParams.isSendUnlockRequest) {
                fastDebugLog("Wi-Fi unlock not yet succeeded, trying matching Bluetooth device: ${matchingBtDevice.name}")
                tryBluetoothDeviceUnlock(matchingBtDevice, clientUnlockServiceParams)
            } else {
                false
            }
        }
    }

    private suspend fun doUnlock(
        clientUnlockServiceParams: ClientUnlockServiceParams,
        deviceName: String,
        timeoutMillis: Long,
        unlockBlock: suspend () -> Boolean
    ): Boolean = coroutineScope {
        var isSuccess = false
        val startTime = System.currentTimeMillis()
        val currentAttemptCount = AtomicInteger(1)
        val notificationManager = getSystemService<NotificationManager>()

        val resolvedDeviceName = deviceName.ifEmpty {
            if (clientUnlockServiceParams.isSendWOLRequest) getString(R.string.unlocking_the_device_wol)
            else getString(R.string.unlocking_the_device)
        }
        val cancelIntent = PendingIntent.getService(
            this@ClientUnlockService,
            notificationId,
            Intent(this@ClientUnlockService, this@ClientUnlockService::class.java).apply { action = ACTION_CANCEL_UNLOCK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val smallIconRes = appIconManager?.getAppIconSmallIconRes()
            ?: com.xxmrk888ytxx.unlockservice.R.drawable.portal
        val iconBitmap = appIconManager?.getAppIconBitmap() ?: getActiveAppIconBitmap()

        // Update countdown timer every second in notification
        val tickerJob = if (clientUnlockServiceParams.isSendWOLRequest) {
            launch {
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val remaining = (timeoutMillis - elapsed).coerceAtLeast(0L)
                    val remainingSec = (remaining / 1000) % 60
                    val remainingMin = (remaining / 1000) / 60
                    val remainingStr = String.format(Locale.getDefault(), "%d:%02d", remainingMin, remainingSec)
                    val statusText = getString(R.string.wol_unlock_status_format, currentAttemptCount.get(), remainingStr)

                    val updatedNotification = buildWolNotification(
                        deviceName = resolvedDeviceName,
                        statusText = statusText,
                        isIndeterminate = true,
                        isOngoing = true,
                        cancelIntent = cancelIntent,
                        smallIconRes = smallIconRes,
                        iconBitmap = iconBitmap
                    )
                    notificationManager?.notify(notificationId, updatedNotification)
                    delay(1000L)
                }
            }
        } else null

        try {
            withTimeoutOrNull(timeoutMillis) {
                var isFirstAttempt = true
                do {
                    val attemptStartTime = System.currentTimeMillis()
                    if (!isFirstAttempt) {
                        currentAttemptCount.incrementAndGet()
                    }
                    isFirstAttempt = false

                    val isUnlockSuccessful = unlockBlock()
                    if (isUnlockSuccessful) {
                        fastDebugLog("Unlock success. Stop $this")
                        isSuccess = true
                        return@withTimeoutOrNull
                    } else {
                        fastDebugLog("Unlock failed. Restart $this (attempt ${currentAttemptCount.get()})")
                        if (isActive && clientUnlockServiceParams.tryToRetryUnlockUntilSuccessOrTimeout) {
                            val elapsedThisAttempt = System.currentTimeMillis() - attemptStartTime
                            val delayNeeded = (ATTEMPT_INTERVAL - elapsedThisAttempt).coerceAtLeast(500L)
                            delay(delayNeeded)
                        }
                    }
                } while (isActive && clientUnlockServiceParams.tryToRetryUnlockUntilSuccessOrTimeout)
            }
        } finally {
            tickerJob?.cancel()
        }

        if (isSuccess) {
            val successNotification = buildWolNotification(
                deviceName = resolvedDeviceName,
                statusText = getString(R.string.wol_unlock_success),
                isIndeterminate = false,
                isOngoing = false,
                timeoutAfterMillis = NOTIFICATION_DISMISS_DELAY,
                cancelIntent = cancelIntent,
                smallIconRes = smallIconRes,
                iconBitmap = iconBitmap
            )
            stopForeground(STOP_FOREGROUND_DETACH)
            notificationManager?.notify(notificationId, successNotification)
            scheduleNotificationDismiss(notificationManager, notificationId, NOTIFICATION_DISMISS_DELAY)
        } else {
            fastDebugLog("Unlock timeout or failed. Stop $this")
            val failNotification = buildWolNotification(
                deviceName = resolvedDeviceName,
                statusText = getString(R.string.wol_unlock_failed),
                isIndeterminate = false,
                isOngoing = false,
                timeoutAfterMillis = NOTIFICATION_DISMISS_DELAY,
                cancelIntent = cancelIntent,
                smallIconRes = smallIconRes,
                iconBitmap = iconBitmap
            )
            stopForeground(STOP_FOREGROUND_DETACH)
            notificationManager?.notify(notificationId, failNotification)
            scheduleNotificationDismiss(notificationManager, notificationId, NOTIFICATION_DISMISS_DELAY)
        }
        return@coroutineScope isSuccess
    }

    private fun buildWolNotification(
        deviceName: String,
        statusText: String,
        isIndeterminate: Boolean,
        isOngoing: Boolean,
        timeoutAfterMillis: Long? = null,
        cancelIntent: PendingIntent? = null,
        smallIconRes: Int = appIconManager?.getAppIconSmallIconRes() ?: com.xxmrk888ytxx.unlockservice.R.drawable.portal,
        iconBitmap: Bitmap? = appIconManager?.getAppIconBitmap() ?: getActiveAppIconBitmap()
    ): Notification {
        val resolvedCancelIntent = cancelIntent ?: PendingIntent.getService(
            this,
            notificationId,
            Intent(this, this::class.java).apply { action = ACTION_CANCEL_UNLOCK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return buildNotification(channelId) {
            setContentTitle(deviceName)
            setContentText(statusText)
            setSmallIcon(smallIconRes)
            iconBitmap?.let { setLargeIcon(it) }
            if (isIndeterminate) {
                setProgress(0, 0, true)
            } else {
                setProgress(0, 0, false)
            }
            setOngoing(isOngoing)
            setOnlyAlertOnce(true)
            setAutoCancel(!isOngoing)
            if (isOngoing) {
                addAction(
                    Notification.Action.Builder(
                        null,
                        getString(android.R.string.cancel),
                        resolvedCancelIntent
                    ).build()
                )
            }
            if (timeoutAfterMillis != null && timeoutAfterMillis > 0) {
                setTimeoutAfter(timeoutAfterMillis)
            }
        }
    }

    private suspend fun tryWifiDeviceUnlock(
        wifiDevice: WifiDevice,
        macAddress: String?,
        clientUnlockServiceParams: ClientUnlockServiceParams
    ): Boolean {
        return try {
            if (macAddress != null && clientUnlockServiceParams.isSendWOLRequest) {
                wolManager.sendWOLRequest(macAddress)
            }
            if (clientUnlockServiceParams.isSendUnlockRequest) {
                val res = deviceUnlockManager.unlockWifiDevice(wifiDevice)
                if (res.isFailure) {
                    fastDebugLog("tryWifiDeviceUnlock failed: ${res.exceptionOrNull()?.message}")
                }
                return res.isSuccess
            }
            true
        } catch (e: Exception) {
            fastDebugLog("Error during tryUnlock: ${e.message}")
            false
        }
    }

    private suspend fun tryBluetoothDeviceUnlock(
        bluetoothDevice: BluetoothDevice,
        clientUnlockServiceParams: ClientUnlockServiceParams
    ): Boolean {
        return try {
            if (clientUnlockServiceParams.isSendUnlockRequest) {
                val res = deviceUnlockManager.unlockBluetoothDevice(bluetoothDevice)
                if (res.isFailure) {
                    fastDebugLog("tryBluetoothDeviceUnlock failed: ${res.exceptionOrNull()?.message}")
                }
                return res.isSuccess
            }
            true
        } catch (e: Exception) {
            fastDebugLog("Error during tryUnlock: ${e.message}")
            false
        }
    }

    override fun onTimeout(startId: Int) {
        stopSelf(startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        currentUnlockJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        const val CLIENT_UNLOCK_SERVICE_PARAMS_KEY = "CLIENT_UNLOCK_SERVICE_PARAMS_KEY"
        const val ACTION_CANCEL_UNLOCK = "com.xxmrk888ytxx.portal.ACTION_CANCEL_UNLOCK"
        const val ATTEMPT_INTERVAL = 5000L
        const val RETRY_UNLOCK_TIMEOUT = 5000L
        const val NOTIFICATION_DISMISS_DELAY = 30_000L
        const val WOL_UNLOCK_TIMEOUT_MILLS = 360_000L

        private val dismissScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        private val pendingDismissJobs = ConcurrentHashMap<Int, Job>()

        fun scheduleNotificationDismiss(
            notificationManager: NotificationManager?,
            notificationId: Int,
            delayMillis: Long = NOTIFICATION_DISMISS_DELAY
        ) {
            cancelNotificationDismiss(notificationId)
            val job = dismissScope.launch {
                delay(delayMillis)
                fastDebugLog("Auto-dismissing notification $notificationId after ${delayMillis}ms")
                notificationManager?.cancel(notificationId)
                pendingDismissJobs.remove(notificationId)
            }
            pendingDismissJobs[notificationId] = job
        }

        fun cancelNotificationDismiss(notificationId: Int) {
            pendingDismissJobs.remove(notificationId)?.cancel()
        }
    }
}
