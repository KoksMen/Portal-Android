package com.xxmrk888ytxx.portal.data

import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.portal.domain.AppIconManager
import com.xxmrk888ytxx.portal.domain.DeviceRepository
import com.xxmrk888ytxx.portal.domain.SettingsRepository
import com.xxmrk888ytxx.portal.domain.SettingsRepository.Companion.DEFAULT_VALUE
import com.xxmrk888ytxx.portal.domain.SettingsRepository.Companion.REMOVED_BY_SECURITY_SETTINGS_CHANGES
import com.xxmrk888ytxx.portal.domain.model.PortalSettings
import com.xxmrk888ytxx.preferencesstorage.PreferencesStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val preferencesStorage: PreferencesStorage,
    private val deviceRepository: DeviceRepository,
    private val appIconManager: AppIconManager
) : SettingsRepository {

    private val biometricAuthEnabled = booleanPreferencesKey("biometricAuthEnabled")
    private val additionalPasswordAuthEnabled =
        booleanPreferencesKey("additionalPasswordAuthEnabled")
    private val removePairedClientsIfBiometricEnvironmentChanged =
        booleanPreferencesKey("removePairedClientsIfBiometricEnvironmentChanged")
    private val pairedClientsWasRemovedBySecurityChanges =
        intPreferencesKey("pairedClientsWasRemovedBySecurityChanges")

    private val isUnsafeUnlockTypesDisabledKey =
        booleanPreferencesKey("isUnsafeUnlockTypesDisabledKey")
    private val isOnboardingPassedKey = booleanPreferencesKey("is_onboarding_passed")
    private val themeColorKey = longPreferencesKey("theme_color")
    private val watchDogEnabledKey = booleanPreferencesKey("watch_dog_enabled")
    private val appIconKey = stringPreferencesKey("app_icon_key")
    private val customAppIconPathKey = stringPreferencesKey("custom_app_icon_path")
    private val unlockScreenStyleKey = stringPreferencesKey("unlock_screen_style")
    private val autoBiometricOnNotificationKey = booleanPreferencesKey("auto_biometric_on_notification")
    private val backgroundServiceEnabledKey = booleanPreferencesKey("background_service_enabled")
    private val wakeLockEnabledKey = booleanPreferencesKey("wake_lock_enabled")
    private val watchDogFastCheckEnabledKey = booleanPreferencesKey("watch_dog_fast_check_enabled")
    private val watchDogAlarmClockEnabledKey = booleanPreferencesKey("watch_dog_alarm_clock_enabled")
    private val type1UnlockTimeoutMinutesKey = intPreferencesKey("type1_unlock_timeout_minutes")
    private val type2IncomingConflictBehaviorKey = stringPreferencesKey("type2_incoming_conflict_behavior")
    private val type1RetryIntervalSecondsKey = intPreferencesKey("type1_retry_interval_seconds")


    override val portalSettings: Flow<PortalSettings> = combine<Any?, PortalSettings>(
        preferencesStorage.getProperty(biometricAuthEnabled, false),
        preferencesStorage.getProperty(additionalPasswordAuthEnabled, false),
        preferencesStorage.getProperty(removePairedClientsIfBiometricEnvironmentChanged, false),
        preferencesStorage.getProperty(pairedClientsWasRemovedBySecurityChanges, DEFAULT_VALUE),
        preferencesStorage.getProperty(isUnsafeUnlockTypesDisabledKey, false),
        preferencesStorage.getProperty(isOnboardingPassedKey, false),
        preferencesStorage.getPropertyOrNull(themeColorKey),
        preferencesStorage.getProperty(watchDogEnabledKey, false),
        preferencesStorage.getProperty(appIconKey, "default"),
        preferencesStorage.getProperty(unlockScreenStyleKey, "classic"),
        preferencesStorage.getPropertyOrNull(customAppIconPathKey),
        preferencesStorage.getProperty(autoBiometricOnNotificationKey, false),
        preferencesStorage.getProperty(backgroundServiceEnabledKey, false),
        preferencesStorage.getProperty(wakeLockEnabledKey, false),
        preferencesStorage.getProperty(watchDogFastCheckEnabledKey, true),
        preferencesStorage.getProperty(watchDogAlarmClockEnabledKey, true),
        preferencesStorage.getProperty(type1UnlockTimeoutMinutesKey, 6),
        preferencesStorage.getProperty(type2IncomingConflictBehaviorKey, "defer"),
        preferencesStorage.getProperty(type1RetryIntervalSecondsKey, 5)
    ) { flowArray ->
        val biometricAuthEnabled = flowArray[0] as Boolean
        val additionalPasswordAuthEnabled = flowArray[1] as Boolean
        val removePairedClientsIfBiometricEnvironmentChanged = flowArray[2] as Boolean
        val pairedClientsWasRemovedBySecurityChanges = flowArray[3] as Int
        val isUnsafeUnlockTypesDisabled = flowArray[4] as Boolean
        val isOnboardingPassed = flowArray[5] as Boolean
        val themeColor = flowArray[6] as Long?
        val isWatchDogEnabled = flowArray[7] as Boolean
        val appIconId = flowArray[8] as String
        val unlockScreenStyle = flowArray[9] as String
        val customAppIconPath = flowArray[10] as String?
        val isAutoBiometricOnNotificationEnabled = flowArray[11] as Boolean
        val isBackgroundServiceEnabled = flowArray[12] as Boolean
        val isWakeLockEnabled = flowArray[13] as Boolean
        val isWatchDogFastCheckEnabled = flowArray[14] as Boolean
        val isWatchDogAlarmClockEnabled = flowArray[15] as Boolean
        val type1UnlockTimeoutMinutes = flowArray[16] as Int
        val type2IncomingConflictBehavior = flowArray[17] as String
        val type1RetryIntervalSeconds = flowArray[18] as Int

        PortalSettings(
            isBiometricAuthEnabled = biometricAuthEnabled,
            isAdditionalPasswordAuthEnabled = additionalPasswordAuthEnabled,
            isRemovePairedClientsIfBiometricEnvironmentChangedEnabled = removePairedClientsIfBiometricEnvironmentChanged,
            pairedClientsWasRemoveBySecurityChangesCode = pairedClientsWasRemovedBySecurityChanges,
            isUnsafeUnlockTypesDisabled = isUnsafeUnlockTypesDisabled,
            isOnboardingPassed = isOnboardingPassed,
            themeColor = if (themeColor == null) null else Color(value = themeColor.toULong()),
            isWatchDogEnabled = isWatchDogEnabled,
            appIconId = appIconId,
            customAppIconPath = customAppIconPath ?: appIconManager.getCustomAppIconFile()?.absolutePath,
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
    }

    override suspend fun updateBiometricAuthEnabled(isEnabled: Boolean) =
        withContext(Dispatchers.IO) {
            preferencesStorage.writeProperty(biometricAuthEnabled, isEnabled)
            if (!isEnabled) {
                updateAdditionalPasswordAuthEnabled(false)
                updateRemovePairedClientsIfBiometricEnvironmentChanged(false)
            }
        }

    override suspend fun updateAdditionalPasswordAuthEnabled(isEnabled: Boolean) =
        withContext(Dispatchers.IO) {
            preferencesStorage.writeProperty(additionalPasswordAuthEnabled, isEnabled)
        }

    override suspend fun updateRemovePairedClientsIfBiometricEnvironmentChanged(isEnabled: Boolean) =
        withContext(Dispatchers.IO) {
            preferencesStorage.writeProperty(
                removePairedClientsIfBiometricEnvironmentChanged,
                isEnabled
            )

            if (!isEnabled) {
                deviceRepository.removeAllDevices()
                updatePairedClientsWasRemoveBySecurityChangesCode(
                    REMOVED_BY_SECURITY_SETTINGS_CHANGES
                )
            }
        }

    override suspend fun updatePairedClientsWasRemoveBySecurityChangesCode(newCode: Int) =
        withContext(Dispatchers.IO) {
            preferencesStorage.writeProperty(pairedClientsWasRemovedBySecurityChanges, newCode)
        }

    override suspend fun updateUnsafeUnlockTypesDisabled(newState: Boolean) = withContext(
        Dispatchers.IO
    ) {
        preferencesStorage.writeProperty(isUnsafeUnlockTypesDisabledKey, newState)
        if (!newState) {
            deviceRepository.removeAllDevices()
            updatePairedClientsWasRemoveBySecurityChangesCode(
                REMOVED_BY_SECURITY_SETTINGS_CHANGES
            )
        }
    }

    override suspend fun markOnboardingAsPassed() = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(isOnboardingPassedKey, true)
    }

    override suspend fun updateThemeColor(newColor: Color?) = withContext(Dispatchers.IO) {
        fastDebugLog("updateThemeColor $newColor")
        if (newColor == null) {
            preferencesStorage.removeProperty(themeColorKey)
            return@withContext
        }
        preferencesStorage.writeProperty(themeColorKey, newColor.value.toLong())
    }

    override suspend fun updateWatchDogEnabled(isEnabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(watchDogEnabledKey, isEnabled)
    }

    override suspend fun updateAppIconId(iconId: String) = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(appIconKey, iconId)
        appIconManager.setAppIcon(iconId)
    }

    override suspend fun updateUnlockScreenStyle(styleId: String) = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(unlockScreenStyleKey, styleId)
    }

    override suspend fun updateAutoBiometricOnNotificationEnabled(isEnabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(autoBiometricOnNotificationKey, isEnabled)
    }

    override suspend fun updateCustomAppIconPath(path: String?) = withContext(Dispatchers.IO) {
        if (path == null) {
            preferencesStorage.removeProperty(customAppIconPathKey)
        } else {
            preferencesStorage.writeProperty(customAppIconPathKey, path)
        }
    }

    override suspend fun updateBackgroundServiceEnabled(isEnabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(backgroundServiceEnabledKey, isEnabled)
    }

    override suspend fun updateWakeLockEnabled(isEnabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(wakeLockEnabledKey, isEnabled)
    }

    override suspend fun updateWatchDogFastCheckEnabled(isEnabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(watchDogFastCheckEnabledKey, isEnabled)
    }

    override suspend fun updateWatchDogAlarmClockEnabled(isEnabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(watchDogAlarmClockEnabledKey, isEnabled)
    }

    override suspend fun updateType1UnlockTimeoutMinutes(minutes: Int) = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(type1UnlockTimeoutMinutesKey, minutes)
    }

    override suspend fun updateType2IncomingConflictBehavior(behavior: String) = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(type2IncomingConflictBehaviorKey, behavior)
    }

    override suspend fun updateType1RetryIntervalSeconds(seconds: Int) = withContext(Dispatchers.IO) {
        preferencesStorage.writeProperty(type1RetryIntervalSecondsKey, seconds)
    }
}
