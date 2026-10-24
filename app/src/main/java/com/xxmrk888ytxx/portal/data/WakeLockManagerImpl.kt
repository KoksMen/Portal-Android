package com.xxmrk888ytxx.portal.data

import android.content.Context
import android.os.PowerManager
import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.portal.domain.SettingsRepository
import com.xxmrk888ytxx.portal.domain.WakeLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class WakeLockManagerImpl @Inject constructor(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : WakeLockManager {

    private val powerManager: PowerManager?
        get() = context.getSystemService(PowerManager::class.java)

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeWakeLocks = ConcurrentHashMap<String, PowerManager.WakeLock>()

    override fun acquireWakeLock(tag: String) {
        try {
            val pm = powerManager ?: return
            activeWakeLocks.computeIfAbsent(tag) {
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Portal:$tag").apply {
                    setReferenceCounted(false)
                }
            }.let { wakeLock ->
                if (!wakeLock.isHeld) {
                    wakeLock.acquire(60 * 60 * 1000L) // 1 hour safety timeout
                    fastDebugLog("WakeLock acquired for $tag")
                }
            }
        } catch (e: Exception) {
            fastDebugLog("Failed to acquire WakeLock for $tag: ${e.message}")
        }
    }

    override fun releaseWakeLock(tag: String) {
        try {
            activeWakeLocks.remove(tag)?.let { wakeLock ->
                if (wakeLock.isHeld) {
                    wakeLock.release()
                    fastDebugLog("WakeLock released for $tag")
                }
            }
        } catch (e: Exception) {
            fastDebugLog("Failed to release WakeLock for $tag: ${e.message}")
        }
    }

    override fun startObserving() {
        scope.launch {
            settingsRepository.portalSettings
                .map { it.isWakeLockEnabled }
                .distinctUntilChanged()
                .collect { isEnabled ->
                    if (isEnabled) {
                        acquireWakeLock(TAG_PERSISTENT)
                    } else {
                        releaseWakeLock(TAG_PERSISTENT)
                    }
                }
        }
    }

    private companion object {
        const val TAG_PERSISTENT = "PersistentWakeLock"
    }
}
