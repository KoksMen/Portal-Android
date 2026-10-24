package com.xxmrk888ytxx.settingsscreen.model

data class ScreenState(
    val bottomSheetState: BottomSheetState = BottomSheetState.None,
    val isBiometricProtectionEnabled: Boolean = false,
    val appVersion: String = "",
    val isAdditionalPasswordAuthEnabled: Boolean = false,
    val isRemovePairedClientsIfBiometricEnvironmentChangedEnabled: Boolean = false,
    val isUnsafeUnlockTypesDisabled: Boolean = false,
    val isWatchDogEnabled: Boolean = false,
    val isBiometricAuthAvailable: Boolean = false,
    val currentAppIconId: String = AppIcon.DEFAULT.id,
    val customAppIconPath: String? = null,
    val unlockScreenStyle: String = UnlockScreenStyle.FULLSCREEN.id,
    val isAutoBiometricOnNotificationEnabled: Boolean = false,
    val isBackgroundServiceEnabled: Boolean = false,
    val isWakeLockEnabled: Boolean = false,
    val isWatchDogFastCheckEnabled: Boolean = true,
    val isWatchDogAlarmClockEnabled: Boolean = true,
    val type1UnlockTimeoutMinutes: Int = 6,
    val type2IncomingConflictBehavior: String = "show"
)
