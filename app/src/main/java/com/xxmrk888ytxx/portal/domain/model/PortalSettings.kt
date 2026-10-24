package com.xxmrk888ytxx.portal.domain.model

import androidx.compose.ui.graphics.Color

data class PortalSettings(
    val isBiometricAuthEnabled: Boolean,
    val isAdditionalPasswordAuthEnabled: Boolean,
    val isRemovePairedClientsIfBiometricEnvironmentChangedEnabled: Boolean,
    val pairedClientsWasRemoveBySecurityChangesCode: Int,
    val isUnsafeUnlockTypesDisabled: Boolean,
    val isOnboardingPassed: Boolean,
    val themeColor: Color?,
    val isWatchDogEnabled: Boolean,
    val appIconId: String = "default",
    val customAppIconPath: String? = null,
    val unlockScreenStyle: String = "classic",
    val isAutoBiometricOnNotificationEnabled: Boolean = false,
    val isBackgroundServiceEnabled: Boolean = false,
    val isWakeLockEnabled: Boolean = false,
    val isWatchDogFastCheckEnabled: Boolean = true,
    val isWatchDogAlarmClockEnabled: Boolean = true,
    val type1UnlockTimeoutMinutes: Int = 6,
    val type2IncomingConflictBehavior: String = "show"
)
