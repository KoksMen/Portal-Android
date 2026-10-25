package com.xxmrk888ytxx.portal.data.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.getSystemService
import com.xxmrk888ytxx.coreandroid.buildNotification
import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.portal.R
import com.xxmrk888ytxx.portal.data.service.model.ClientUnlockServiceParams
import com.xxmrk888ytxx.portal.domain.ActiveUnlockTracker
import com.xxmrk888ytxx.portal.domain.BluetoothDeviceRepository
import com.xxmrk888ytxx.portal.domain.DeviceUnlockManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

abstract class ClientUnlockService(
    private val wolManager: WOLManager,
    private val deviceUnlockManager: DeviceUnlockManager,
    private val wifiDeviceRepository: WifiDeviceRepository,
    private val bluetoothDeviceRepository: BluetoothDeviceRepository,
    private val activeUnlockTracker: ActiveUnlockTracker,
    private val wakeLockManager: WakeLockManager,
    private val settingsRepository: SettingsRepository,
    private val unlockRequestHandler: dagger.Lazy<UnlockRequestHandler>
) : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentUnlockJob: Job? = null

    protected abstract val channelId: String
    protected abstract val notificationId: Int
    protected abstract val notification: Notification
    protected abstract val action: String

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_UNLOCK) {
            fastDebugLog("Cancel action received in ClientUnlockService")
            currentUnlockJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(notificationId, notification)
        }
        currentUnlockJob = serviceScope.launch {
            handleUnlock(clientUnlockServiceParams)
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
        try {
            val wifiDevice = wifiDeviceRepository.getDeviceById(clientId).first()
            val bluetoothDevice = bluetoothDeviceRepository.getDeviceById(clientId).first()

            when {
                wifiDevice != null -> {
                    unlockSucceeded = doUnlockWifiDevice(wifiDevice, clientUnlockServiceParams, timeoutMillis)
                }
                bluetoothDevice != null -> {
                    unlockSucceeded = unlockBluetoothDevice(bluetoothDevice, clientUnlockServiceParams, timeoutMillis)
                }
                else -> {
                    fastDebugLog("Device where id = $clientId doesn't exist. $this Skip ")
                }
            }
        } finally {
            wakeLockManager.releaseWakeLock(wakeLockTag)
            activeUnlockTracker.unregisterType1Unlock(clientId, unlockSucceeded)
            if (!unlockSucceeded) {
                val deferredRequest = activeUnlockTracker.pollDeferredType2Request(clientId)
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
        timeoutMillis: Long
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
            tryWifiDeviceUnlock(
                wifiDevice = wifiDevice,
                macAddress = macAddress,
                clientUnlockServiceParams = clientUnlockServiceParams
            )
        }
    }

    private suspend fun doUnlock(
        clientUnlockServiceParams: ClientUnlockServiceParams,
        deviceName: String,
        timeoutMillis: Long,
        unlockBlock: suspend () -> Boolean
    ): Boolean {
        var isSuccess = false
        val startTime = System.currentTimeMillis()
        var attemptCount = 0
        val notificationManager = getSystemService<NotificationManager>()

        withTimeoutOrNull(timeoutMillis) {
            do {
                attemptCount++
                if (clientUnlockServiceParams.isSendWOLRequest) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val remaining = (timeoutMillis - elapsed).coerceAtLeast(0L)
                    val remainingSec = (remaining / 1000) % 60
                    val remainingMin = (remaining / 1000) / 60
                    val remainingStr = String.format(Locale.getDefault(), "%d:%02d", remainingMin, remainingSec)
                    val statusText = getString(R.string.wol_unlock_status_format, attemptCount, remainingStr)

                    val updatedNotification = buildWolNotification(
                        deviceName = deviceName.ifEmpty { getString(R.string.unlocking_the_device_wol) },
                        statusText = statusText,
                        isIndeterminate = true,
                        isOngoing = true
                    )
                    notificationManager?.notify(notificationId, updatedNotification)
                }

                val isUnlockSuccessful = unlockBlock()
                if (isUnlockSuccessful) {
                    fastDebugLog("Unlock success. Stop $this")
                    isSuccess = true
                    if (clientUnlockServiceParams.isSendWOLRequest) {
                        val successNotification = buildWolNotification(
                            deviceName = deviceName.ifEmpty { getString(R.string.unlocking_the_device_wol) },
                            statusText = getString(R.string.wol_unlock_success),
                            isIndeterminate = false,
                            isOngoing = false
                        )
                        stopForeground(STOP_FOREGROUND_DETACH)
                        notificationManager?.notify(notificationId, successNotification)
                    }
                    return@withTimeoutOrNull
                } else {
                    fastDebugLog("Unlock failed. Restart $this (attempt $attemptCount)")
                    delay(RETRY_UNLOCK_TIMEOUT)
                }
            } while (isActive && clientUnlockServiceParams.tryToRetryUnlockUntilSuccessOrTimeout)
        } ?: run {
            fastDebugLog("Unlock timeout. Stop $this")
            if (clientUnlockServiceParams.isSendWOLRequest) {
                val failNotification = buildWolNotification(
                    deviceName = deviceName.ifEmpty { getString(R.string.unlocking_the_device_wol) },
                    statusText = getString(R.string.wol_unlock_failed),
                    isIndeterminate = false,
                    isOngoing = false
                )
                stopForeground(STOP_FOREGROUND_DETACH)
                notificationManager?.notify(notificationId, failNotification)
            }
        }
        return isSuccess
    }

    private fun buildWolNotification(
        deviceName: String,
        statusText: String,
        isIndeterminate: Boolean,
        isOngoing: Boolean
    ): Notification {
        val cancelIntent = PendingIntent.getService(
            this,
            notificationId,
            Intent(this, this::class.java).apply { action = ACTION_CANCEL_UNLOCK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return buildNotification(channelId) {
            setContentTitle(deviceName)
            setContentText(statusText)
            setSmallIcon(com.xxmrk888ytxx.unlockservice.R.drawable.portal)
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
                        cancelIntent
                    ).build()
                )
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
                return deviceUnlockManager.unlockWifiDevice(wifiDevice).isSuccess
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
                deviceUnlockManager.unlockBluetoothDevice(bluetoothDevice).isSuccess
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
        serviceScope.cancel()
    }

    companion object {
        const val CLIENT_UNLOCK_SERVICE_PARAMS_KEY = "CLIENT_UNLOCK_SERVICE_PARAMS_KEY"
        const val ACTION_CANCEL_UNLOCK = "com.xxmrk888ytxx.portal.ACTION_CANCEL_UNLOCK"
        const val RETRY_UNLOCK_TIMEOUT = 2000L
        const val WOL_UNLOCK_TIMEOUT_MILLS = 360_000L
    }
}
