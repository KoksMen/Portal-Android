package com.xxmrk888ytxx.portal.domain

import androidx.compose.ui.graphics.Color
import com.xxmrk888ytxx.portal.domain.model.PortalSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val portalSettings: Flow<PortalSettings>
    suspend fun updateBiometricAuthEnabled(isEnabled: Boolean)
    suspend fun updateAdditionalPasswordAuthEnabled(isEnabled: Boolean)
    suspend fun updateRemovePairedClientsIfBiometricEnvironmentChanged(isEnabled: Boolean)
    suspend fun updatePairedClientsWasRemoveBySecurityChangesCode(newCode: Int)
    suspend fun updateUnsafeUnlockTypesDisabled(newState: Boolean)
    suspend fun markOnboardingAsPassed()
    suspend fun updateThemeColor(newColor: Color?)
    suspend fun updateWatchDogEnabled(isEnabled: Boolean)
    suspend fun updateAppIconId(iconId: String)
    suspend fun updateCustomAppIconPath(path: String?)
    suspend fun updateUnlockScreenStyle(styleId: String)
    suspend fun updateAutoBiometricOnNotificationEnabled(isEnabled: Boolean)
    suspend fun updateBackgroundServiceEnabled(isEnabled: Boolean)
    suspend fun updateWakeLockEnabled(isEnabled: Boolean)
    suspend fun updateWatchDogFastCheckEnabled(isEnabled: Boolean)
    suspend fun updateWatchDogAlarmClockEnabled(isEnabled: Boolean)
    suspend fun updateType1UnlockTimeoutMinutes(minutes: Int)
    suspend fun updateType2IncomingConflictBehavior(behavior: String)
    suspend fun updateType1RetryIntervalSeconds(seconds: Int)
    suspend fun updateMixedUnlockEnabled(isEnabled: Boolean)

    companion object {
        const val DEFAULT_VALUE = 0
        const val REMOVED_BY_SECURITY_SETTINGS_CHANGES = 1
        const val REMOVED_BY_CHANGES_IN_BIOMETRIC_ENVIRONMENT = 2
    }
}
