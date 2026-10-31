package com.xxmrk888ytxx.settingsscreen.contract

import androidx.compose.ui.graphics.Color

interface ChangeSettingsContract {
    suspend fun updateBiometricProtectionState(isEnabled: Boolean)
    suspend fun updateAdditionalPasswordAuthState(isEnabled: Boolean)
    suspend fun updateRemovePairedClientsIfBiometricEnvironmentChangedState(isEnabled: Boolean)
    suspend fun updateUnsafeUnlockTypesState(newState: Boolean)
    suspend fun updateWatchDogState(isEnabled: Boolean)
    suspend fun updateThemeColor(newColor: Color?)
    suspend fun updateAppIcon(iconId: String)
    suspend fun setCustomAppIcon(imageUriString: String)
    suspend fun pinCustomAppIconShortcut()
    suspend fun updateUnlockScreenStyle(styleId: String)
    suspend fun updateAutoBiometricOnNotificationState(isEnabled: Boolean)
    suspend fun updateBackgroundServiceState(isEnabled: Boolean)
    suspend fun updateWakeLockState(isEnabled: Boolean)
    suspend fun updateWatchDogFastCheckState(isEnabled: Boolean)
    suspend fun updateWatchDogAlarmClockState(isEnabled: Boolean)
    suspend fun updateType1UnlockTimeoutMinutes(minutes: Int)
    suspend fun updateType2IncomingConflictBehavior(behavior: String)
    suspend fun updateType1RetryIntervalSeconds(seconds: Int)
    suspend fun updateMixedUnlockState(isEnabled: Boolean)
}
