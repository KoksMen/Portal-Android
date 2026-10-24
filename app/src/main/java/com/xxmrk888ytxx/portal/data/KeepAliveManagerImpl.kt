package com.xxmrk888ytxx.portal.data

import android.content.Context
import android.content.Intent
import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.portal.data.service.PortalKeepAliveService
import com.xxmrk888ytxx.portal.domain.KeepAliveManager
import com.xxmrk888ytxx.portal.domain.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class KeepAliveManagerImpl @Inject constructor(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : KeepAliveManager {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun startObserving() {
        scope.launch {
            settingsRepository.portalSettings
                .map { it.isBackgroundServiceEnabled }
                .distinctUntilChanged()
                .collect { isEnabled ->
                    val intent = Intent(context, PortalKeepAliveService::class.java)
                    if (isEnabled) {
                        try {
                            context.startService(intent)
                            fastDebugLog("Started PortalKeepAliveService")
                        } catch (e: Exception) {
                            fastDebugLog("Failed to start PortalKeepAliveService: ${e.message}")
                        }
                    } else {
                        try {
                            context.stopService(intent)
                            fastDebugLog("Stopped PortalKeepAliveService")
                        } catch (e: Exception) {
                            fastDebugLog("Failed to stop PortalKeepAliveService: ${e.message}")
                        }
                    }
                }
        }
    }

    override fun ensureKeepAliveRunning() {
        scope.launch {
            val isEnabled = settingsRepository.portalSettings.map { it.isBackgroundServiceEnabled }.first()
            if (isEnabled) {
                try {
                    val intent = Intent(context, PortalKeepAliveService::class.java)
                    context.startService(intent)
                    fastDebugLog("Ensured PortalKeepAliveService is running")
                } catch (e: Exception) {
                    fastDebugLog("Failed to ensure PortalKeepAliveService: ${e.message}")
                }
            }
        }
    }
}
