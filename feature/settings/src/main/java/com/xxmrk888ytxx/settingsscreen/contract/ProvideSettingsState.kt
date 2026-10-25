package com.xxmrk888ytxx.settingsscreen.contract

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow

interface ProvideSettingsState {
    val appVersion: Flow<String>
    val isBiometricProtectionEnabled: Flow<Boolean>
    val isAdditionalPasswordAuthEnabled: Flow<Boolean>
    val isRemovePairedClientsIfBiometricEnvironmentChangedEnabled: Flow<Boolean>
    val isUnsafeUnlockTypesDisabled: Flow<Boolean>
    val isWatchDogEnabled: Flow<Boolean>
    val themeColor: Flow<Color?>
    val appIconId: Flow<String>
    val customAppIconPath: Flow<String?>
    val unlockScreenStyle: Flow<String>
    val isAutoBiometricOnNotificationEnabled: Flow<Boolean>
    val isBackgroundServiceEnabled: Flow<Boolean>
    val isWakeLockEnabled: Flow<Boolean>
    val isWatchDogFastCheckEnabled: Flow<Boolean>
    val isWatchDogAlarmClockEnabled: Flow<Boolean>
    val type1UnlockTimeoutMinutes: Flow<Int>
    val type2IncomingConflictBehavior: Flow<String>
    val type1RetryIntervalSeconds: Flow<Int>
}
