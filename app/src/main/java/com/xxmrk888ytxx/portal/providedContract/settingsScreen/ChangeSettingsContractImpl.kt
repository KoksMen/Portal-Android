package com.xxmrk888ytxx.portal.providedContract.settingsScreen

import androidx.compose.ui.graphics.Color
import com.xxmrk888ytxx.coreandroid.ToastManager
import com.xxmrk888ytxx.portal.domain.AppIconManager
import com.xxmrk888ytxx.portal.domain.SettingsRepository
import com.xxmrk888ytxx.settingsscreen.contract.ChangeSettingsContract
import javax.inject.Inject

class ChangeSettingsContractImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appIconManager: AppIconManager,
    private val toastManager: ToastManager
) : ChangeSettingsContract {
    override suspend fun updateBiometricProtectionState(isEnabled: Boolean) {
        settingsRepository.updateBiometricAuthEnabled(isEnabled)
    }

    override suspend fun updateAdditionalPasswordAuthState(isEnabled: Boolean) {
        settingsRepository.updateAdditionalPasswordAuthEnabled(isEnabled)
    }

    override suspend fun updateRemovePairedClientsIfBiometricEnvironmentChangedState(isEnabled: Boolean) {
        settingsRepository.updateRemovePairedClientsIfBiometricEnvironmentChanged(isEnabled)
    }

    override suspend fun updateUnsafeUnlockTypesState(newState: Boolean) {
        settingsRepository.updateUnsafeUnlockTypesDisabled(newState)
    }

    override suspend fun updateWatchDogState(isEnabled: Boolean) {
        settingsRepository.updateWatchDogEnabled(isEnabled)
    }

    override suspend fun updateThemeColor(newColor: Color?) {
        settingsRepository.updateThemeColor(newColor)
    }

    override suspend fun updateAppIcon(iconId: String) {
        settingsRepository.updateAppIconId(iconId)
    }

    override suspend fun setCustomAppIcon(imageUriString: String) {
        val savedPath = appIconManager.processAndSaveCustomIcon(imageUriString)
        if (savedPath != null) {
            settingsRepository.updateCustomAppIconPath(savedPath)
            settingsRepository.updateAppIconId("custom")
            appIconManager.pinCustomAppShortcut()
            toastManager.showToast(com.xxmrk888ytxx.settingsscreen.R.string.custom_icon_pinned_hint)
        } else {
            toastManager.showToast(com.xxmrk888ytxx.settingsscreen.R.string.failed_to_load_image)
        }
    }

    override suspend fun pinCustomAppIconShortcut() {
        val success = appIconManager.pinCustomAppShortcut()
        if (success) {
            toastManager.showToast(com.xxmrk888ytxx.settingsscreen.R.string.custom_icon_pinned_hint)
        }
    }

    override suspend fun updateUnlockScreenStyle(styleId: String) {
        settingsRepository.updateUnlockScreenStyle(styleId)
    }

    override suspend fun updateAutoBiometricOnNotificationState(isEnabled: Boolean) {
        settingsRepository.updateAutoBiometricOnNotificationEnabled(isEnabled)
    }

    override suspend fun updateBackgroundServiceState(isEnabled: Boolean) {
        settingsRepository.updateBackgroundServiceEnabled(isEnabled)
    }

    override suspend fun updateWakeLockState(isEnabled: Boolean) {
        settingsRepository.updateWakeLockEnabled(isEnabled)
    }

    override suspend fun updateWatchDogFastCheckState(isEnabled: Boolean) {
        settingsRepository.updateWatchDogFastCheckEnabled(isEnabled)
    }

    override suspend fun updateWatchDogAlarmClockState(isEnabled: Boolean) {
        settingsRepository.updateWatchDogAlarmClockEnabled(isEnabled)
    }

    override suspend fun updateType1UnlockTimeoutMinutes(minutes: Int) {
        settingsRepository.updateType1UnlockTimeoutMinutes(minutes)
    }

    override suspend fun updateType2IncomingConflictBehavior(behavior: String) {
        settingsRepository.updateType2IncomingConflictBehavior(behavior)
    }

    override suspend fun updateType1RetryIntervalSeconds(seconds: Int) {
        settingsRepository.updateType1RetryIntervalSeconds(seconds)
    }

    override suspend fun updateMixedUnlockState(isEnabled: Boolean) {
        settingsRepository.updateMixedUnlockEnabled(isEnabled)
    }
}
