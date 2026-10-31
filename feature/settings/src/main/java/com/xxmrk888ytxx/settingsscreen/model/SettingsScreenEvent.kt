package com.xxmrk888ytxx.settingsscreen.model

import androidx.compose.ui.graphics.Color
import com.xxmrk888ytxx.coreandroid.mvi.UiEvent

sealed interface SettingsScreenEvent : UiEvent {
    data object OnLogsClick : SettingsScreenEvent
    data class OnBiometricProtectionStateChanged(val newState: Boolean) : SettingsScreenEvent
    data class OnAutoBiometricOnNotificationChanged(val newState: Boolean) : SettingsScreenEvent
    data class OnAdditionalPasswordAuthStateChanged(val newState: Boolean) : SettingsScreenEvent
    data class OnRemovePairedClientsIfBiometricEnvironmentStateChanged(val newState: Boolean) : SettingsScreenEvent
    data class OnChangeUnsafeUnlockTypesState(val newState: Boolean) : SettingsScreenEvent
    data class OnWatchDogStateChanged(val newState: Boolean) : SettingsScreenEvent
    class ConfirmSecurityChanges(val actionAfterConfirm: () -> Unit) : SettingsScreenEvent
    data class OnThemeColorSelected(val newColor: Color?): SettingsScreenEvent
    data object HideBottomSheet : SettingsScreenEvent
    data object OnTermsClicked: SettingsScreenEvent
    data object OnPrivacyClicked: SettingsScreenEvent
    data object OnAndroidSourceCodeClick: SettingsScreenEvent
    data object OnPCSourceCodeClick: SettingsScreenEvent
    data object OnAndroidDeveloperClick: SettingsScreenEvent
    data object OnPCDeveloperClicked: SettingsScreenEvent
    data object OpenOpenSourceLicenses : SettingsScreenEvent
    data object OnThemeClicked : SettingsScreenEvent
    data object OnAppIconClicked : SettingsScreenEvent
    data class OnAppIconSelected(val newIconId: String) : SettingsScreenEvent
    data class OnCustomAppIconPicked(val uriString: String) : SettingsScreenEvent
    data object OnPinCustomAppIconShortcut : SettingsScreenEvent
    data object OnUnlockScreenStyleClicked : SettingsScreenEvent
    data class OnUnlockScreenStyleSelected(val newStyleId: String) : SettingsScreenEvent
    data object OnDownloadPcClientClicked : SettingsScreenEvent
    data object OpenTroubleshootingLink : SettingsScreenEvent
    data class OnBackgroundServiceStateChanged(val newState: Boolean) : SettingsScreenEvent
    data class OnWakeLockStateChanged(val newState: Boolean) : SettingsScreenEvent
    data class OnWatchDogFastCheckStateChanged(val newState: Boolean) : SettingsScreenEvent
    data class OnWatchDogAlarmClockStateChanged(val newState: Boolean) : SettingsScreenEvent
    data object OnType1TimeoutClicked : SettingsScreenEvent
    data class OnType1TimeoutSelected(val minutes: Int) : SettingsScreenEvent
    data object OnType2ConflictClicked : SettingsScreenEvent
    data class OnType2ConflictSelected(val behavior: String) : SettingsScreenEvent
    data object OnType1RetryIntervalClicked : SettingsScreenEvent
    data class OnType1RetryIntervalSelected(val seconds: Int) : SettingsScreenEvent
    data class OnMixedUnlockStateChanged(val isEnabled: Boolean) : SettingsScreenEvent
}
