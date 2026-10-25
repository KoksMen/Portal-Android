package com.xxmrk888ytxx.settingsscreen

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.xxmrk888ytxx.coreandroid.SideEffectPortalViewModel
import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.settingsscreen.contract.BiometricProtectionAvailableStateProvider
import com.xxmrk888ytxx.settingsscreen.contract.ChangeSettingsContract
import com.xxmrk888ytxx.settingsscreen.contract.OpenLinkContract
import com.xxmrk888ytxx.settingsscreen.contract.ProvideSettingsState
import com.xxmrk888ytxx.settingsscreen.model.BottomSheetState
import com.xxmrk888ytxx.settingsscreen.model.ScreenState
import com.xxmrk888ytxx.settingsscreen.model.SettingsScreenEvent
import com.xxmrk888ytxx.settingsscreen.model.SettingsScreenSideEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val provideSettingsState: ProvideSettingsState,
    private val changeSettingsContract: ChangeSettingsContract,
    private val biometricProtectionAvailableStateProvider: BiometricProtectionAvailableStateProvider,
    private val openLinkContract: OpenLinkContract
) : SideEffectPortalViewModel<ScreenState, SettingsScreenEvent>(
    ScreenState()
) {

    private val bottomSheetState = MutableStateFlow<BottomSheetState>(BottomSheetState.None)

    override val state: StateFlow<ScreenState> = combine<Any?, ScreenState>(
        provideSettingsState.isBiometricProtectionEnabled,
        provideSettingsState.appVersion,
        provideSettingsState.isAdditionalPasswordAuthEnabled,
        provideSettingsState.isRemovePairedClientsIfBiometricEnvironmentChangedEnabled,
        bottomSheetState,
        provideSettingsState.isUnsafeUnlockTypesDisabled,
        biometricProtectionAvailableStateProvider.isAvailable,
        provideSettingsState.isWatchDogEnabled,
        provideSettingsState.appIconId,
        provideSettingsState.customAppIconPath,
        provideSettingsState.unlockScreenStyle,
        provideSettingsState.isAutoBiometricOnNotificationEnabled,
        provideSettingsState.isBackgroundServiceEnabled,
        provideSettingsState.isWakeLockEnabled,
        provideSettingsState.isWatchDogFastCheckEnabled,
        provideSettingsState.isWatchDogAlarmClockEnabled,
        provideSettingsState.type1UnlockTimeoutMinutes,
        provideSettingsState.type2IncomingConflictBehavior,
        provideSettingsState.type1RetryIntervalSeconds
    ) { flowArray ->
        val isBiometricProtectionEnabled = flowArray[0] as Boolean
        val appVersion = flowArray[1] as String
        val isAdditionalPasswordAuthEnabled = flowArray[2] as Boolean
        val isRemovePairedClientsIfBiometricEnvironmentChangedEnabled = flowArray[3] as Boolean
        val bottomSheetState = flowArray[4] as BottomSheetState
        val isUnsafeUnlockTypesDisabled = flowArray[5] as Boolean
        val isBiometricAuthAvailable = flowArray[6] as Boolean
        val isWatchDogEnabled = flowArray[7] as Boolean
        val currentAppIconId = flowArray[8] as String
        val customAppIconPath = flowArray[9] as? String
        val unlockScreenStyle = flowArray[10] as String
        val isAutoBiometricOnNotificationEnabled = flowArray[11] as Boolean
        val isBackgroundServiceEnabled = flowArray[12] as Boolean
        val isWakeLockEnabled = flowArray[13] as Boolean
        val isWatchDogFastCheckEnabled = flowArray[14] as Boolean
        val isWatchDogAlarmClockEnabled = flowArray[15] as Boolean
        val type1UnlockTimeoutMinutes = flowArray[16] as Int
        val type2IncomingConflictBehavior = flowArray[17] as String
        val type1RetryIntervalSeconds = flowArray[18] as Int

        ScreenState(
            bottomSheetState = bottomSheetState,
            isBiometricProtectionEnabled = isBiometricProtectionEnabled,
            appVersion = appVersion,
            isAdditionalPasswordAuthEnabled = isAdditionalPasswordAuthEnabled,
            isRemovePairedClientsIfBiometricEnvironmentChangedEnabled = isRemovePairedClientsIfBiometricEnvironmentChangedEnabled,
            isUnsafeUnlockTypesDisabled = isUnsafeUnlockTypesDisabled,
            isWatchDogEnabled = isWatchDogEnabled,
            isBiometricAuthAvailable = isBiometricAuthAvailable,
            currentAppIconId = currentAppIconId,
            customAppIconPath = customAppIconPath,
            unlockScreenStyle = unlockScreenStyle,
            isAutoBiometricOnNotificationEnabled = isAutoBiometricOnNotificationEnabled,
            isBackgroundServiceEnabled = isBackgroundServiceEnabled,
            isWakeLockEnabled = isWakeLockEnabled,
            isWatchDogFastCheckEnabled = isWatchDogFastCheckEnabled,
            isWatchDogAlarmClockEnabled = isWatchDogAlarmClockEnabled,
            type1UnlockTimeoutMinutes = type1UnlockTimeoutMinutes,
            type2IncomingConflictBehavior = type2IncomingConflictBehavior,
            type1RetryIntervalSeconds = type1RetryIntervalSeconds
        )
    }.stateWhileSubscribed()

    override fun handleEvent(event: SettingsScreenEvent) {
        when (event) {
            SettingsScreenEvent.OnLogsClick -> sendNavigationAction { fromSettingsScreenToLogsScreen() }

            is SettingsScreenEvent.OnBiometricProtectionStateChanged -> {
                bottomSheetState.value = BottomSheetState.ConfirmSecurityChangesDialog(isForEnablingSetting = event.newState) {
                    changeBiometricAuthState(event.newState)
                }
            }

            is SettingsScreenEvent.OnAutoBiometricOnNotificationChanged -> changeAutoBiometricOnNotificationState(
                event.newState
            )

            is SettingsScreenEvent.OnAdditionalPasswordAuthStateChanged -> changeAdditionalPasswordAuthState(
                event.newState
            )

            is SettingsScreenEvent.OnRemovePairedClientsIfBiometricEnvironmentStateChanged -> bottomSheetState.value = BottomSheetState.ConfirmSecurityChangesDialog(isForEnablingSetting = event.newState) {
                changeRemovePairedClientsIfBiometricEnvironmentChangedState(event.newState)
            }

            is SettingsScreenEvent.ConfirmSecurityChanges -> event.actionAfterConfirm()
            SettingsScreenEvent.HideBottomSheet -> bottomSheetState.value = BottomSheetState.None
            is SettingsScreenEvent.OnChangeUnsafeUnlockTypesState -> bottomSheetState.value = BottomSheetState.ConfirmSecurityChangesDialog(isForEnablingSetting = event.newState) {
                changeUnsafeUnlockTypesState(event.newState)
            }

            is SettingsScreenEvent.OnWatchDogStateChanged -> changeWatchDogState(event.newState)

            SettingsScreenEvent.OnAndroidDeveloperClick -> openLink { openLinkContract.openAndroidDeveloperLink() }
            SettingsScreenEvent.OnPCDeveloperClicked -> openLink { openLinkContract.openPCDeveloperLink()}
            SettingsScreenEvent.OnPrivacyClicked -> openLink { openLinkContract.openPrivacyPolicyLink() }
            SettingsScreenEvent.OnAndroidSourceCodeClick -> openLink { openLinkContract.openAndroidSourceCodeLink() }
            SettingsScreenEvent.OnTermsClicked -> openLink { openLinkContract.openTermsOfUseLink() }
            SettingsScreenEvent.OnPCSourceCodeClick -> openLink { openLinkContract.openPCSourceCodeLink() }
            SettingsScreenEvent.OpenOpenSourceLicenses -> sideEffect.tryEmit(
                SettingsScreenSideEffect.OpenOpenSourceLicenses)
            SettingsScreenEvent.OnThemeClicked -> showSelectThemeDialog()
            is SettingsScreenEvent.OnThemeColorSelected -> changeThemeColor(event.newColor)
            SettingsScreenEvent.OnAppIconClicked -> showSelectAppIconDialog()
            is SettingsScreenEvent.OnAppIconSelected -> changeAppIcon(event.newIconId)
            is SettingsScreenEvent.OnCustomAppIconPicked -> setCustomAppIcon(event.uriString)
            SettingsScreenEvent.OnPinCustomAppIconShortcut -> pinCustomAppIconShortcut()
            SettingsScreenEvent.OnUnlockScreenStyleClicked -> showSelectUnlockScreenStyleDialog()
            is SettingsScreenEvent.OnUnlockScreenStyleSelected -> changeUnlockScreenStyle(event.newStyleId)
            SettingsScreenEvent.OnDownloadPcClientClicked -> openLink { openLinkContract.openPCClientDownloadLink() }
            SettingsScreenEvent.OpenTroubleshootingLink -> openLink { openLinkContract.openTroubleshootingLink() }

            is SettingsScreenEvent.OnBackgroundServiceStateChanged -> changeBackgroundServiceState(event.newState)
            is SettingsScreenEvent.OnWakeLockStateChanged -> changeWakeLockState(event.newState)
            is SettingsScreenEvent.OnWatchDogFastCheckStateChanged -> changeWatchDogFastCheckState(event.newState)
            is SettingsScreenEvent.OnWatchDogAlarmClockStateChanged -> changeWatchDogAlarmClockState(event.newState)
            SettingsScreenEvent.OnType1TimeoutClicked -> showSelectType1TimeoutDialog()
            is SettingsScreenEvent.OnType1TimeoutSelected -> changeType1Timeout(event.minutes)
            SettingsScreenEvent.OnType2ConflictClicked -> showSelectType2ConflictDialog()
            is SettingsScreenEvent.OnType2ConflictSelected -> changeType2Conflict(event.behavior)
            SettingsScreenEvent.OnType1RetryIntervalClicked -> showSelectType1RetryIntervalDialog()
            is SettingsScreenEvent.OnType1RetryIntervalSelected -> changeType1RetryInterval(event.seconds)
        }
    }

    private fun changeBackgroundServiceState(isEnabled: Boolean) = viewModelScope.launch {
        changeSettingsContract.updateBackgroundServiceState(isEnabled)
    }

    private fun changeWakeLockState(isEnabled: Boolean) = viewModelScope.launch {
        changeSettingsContract.updateWakeLockState(isEnabled)
    }

    private fun changeWatchDogFastCheckState(isEnabled: Boolean) = viewModelScope.launch {
        changeSettingsContract.updateWatchDogFastCheckState(isEnabled)
    }

    private fun changeWatchDogAlarmClockState(isEnabled: Boolean) = viewModelScope.launch {
        changeSettingsContract.updateWatchDogAlarmClockState(isEnabled)
    }

    private fun showSelectType1TimeoutDialog() = viewModelScope.launch {
        bottomSheetState.value = BottomSheetState.SelectType1TimeoutDialog(
            currentMinutes = provideSettingsState.type1UnlockTimeoutMinutes.first()
        )
    }

    private fun changeType1Timeout(minutes: Int) = viewModelScope.launch {
        changeSettingsContract.updateType1UnlockTimeoutMinutes(minutes)
        bottomSheetState.value = BottomSheetState.None
    }

    private fun showSelectType2ConflictDialog() = viewModelScope.launch {
        bottomSheetState.value = BottomSheetState.SelectType2ConflictDialog(
            currentBehavior = provideSettingsState.type2IncomingConflictBehavior.first()
        )
    }

    private fun changeType2Conflict(behavior: String) = viewModelScope.launch {
        changeSettingsContract.updateType2IncomingConflictBehavior(behavior)
        bottomSheetState.value = BottomSheetState.None
    }

    private fun showSelectType1RetryIntervalDialog() = viewModelScope.launch {
        bottomSheetState.value = BottomSheetState.SelectType1RetryIntervalDialog(
            currentSeconds = provideSettingsState.type1RetryIntervalSeconds.first()
        )
    }

    private fun changeType1RetryInterval(seconds: Int) = viewModelScope.launch {
        changeSettingsContract.updateType1RetryIntervalSeconds(seconds)
        bottomSheetState.value = BottomSheetState.None
    }

    private fun changeThemeColor(newColor: Color?) = viewModelScope.launch {
        changeSettingsContract.updateThemeColor(newColor)
    }

    private fun showSelectThemeDialog() = viewModelScope.launch {
        fastDebugLog(provideSettingsState.themeColor.first())
        bottomSheetState.value = BottomSheetState.SelectThemeDialog(provideSettingsState.themeColor.first())
    }

    private fun showSelectAppIconDialog() = viewModelScope.launch {
        bottomSheetState.value = BottomSheetState.SelectAppIconDialog(
            currentIconId = provideSettingsState.appIconId.first()
        )
    }

    private fun showSelectUnlockScreenStyleDialog() = viewModelScope.launch {
        bottomSheetState.value = BottomSheetState.SelectUnlockScreenStyleDialog(provideSettingsState.unlockScreenStyle.first())
    }

    private fun changeUnlockScreenStyle(styleId: String) = viewModelScope.launch {
        changeSettingsContract.updateUnlockScreenStyle(styleId)
        bottomSheetState.value = BottomSheetState.None
    }

    private fun changeAppIcon(iconId: String) = viewModelScope.launch {
        changeSettingsContract.updateAppIcon(iconId)
        bottomSheetState.value = BottomSheetState.None
    }

    private fun setCustomAppIcon(uriString: String) = viewModelScope.launch {
        changeSettingsContract.setCustomAppIcon(uriString)
        bottomSheetState.value = BottomSheetState.None
    }

    private fun pinCustomAppIconShortcut() = viewModelScope.launch {
        changeSettingsContract.pinCustomAppIconShortcut()
    }

    private fun openLink(block: suspend () -> Unit) = viewModelScope.launch {
        block()
    }

    private fun changeUnsafeUnlockTypesState(newState: Boolean) = viewModelScope.launch {
        changeSettingsContract.updateUnsafeUnlockTypesState(newState)
    }

    private fun changeWatchDogState(newState: Boolean) = viewModelScope.launch {
        changeSettingsContract.updateWatchDogState(newState)
    }

    private fun changeBiometricAuthState(isEnabled: Boolean) = viewModelScope.launch {
        changeSettingsContract.updateBiometricProtectionState(isEnabled)
    }

    private fun changeAutoBiometricOnNotificationState(isEnabled: Boolean) = viewModelScope.launch {
        changeSettingsContract.updateAutoBiometricOnNotificationState(isEnabled)
    }

    private fun changeAdditionalPasswordAuthState(isEnabled: Boolean) = viewModelScope.launch {
        changeSettingsContract.updateAdditionalPasswordAuthState(isEnabled)
    }

    private fun changeRemovePairedClientsIfBiometricEnvironmentChangedState(isEnabled: Boolean) =
        viewModelScope.launch {
            changeSettingsContract.updateRemovePairedClientsIfBiometricEnvironmentChangedState(
                isEnabled
            )
        }
}
