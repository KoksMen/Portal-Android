package com.xxmrk888ytxx.settingsscreen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity
import com.xxmrk888ytxx.coreandroid.AvatarLink
import com.xxmrk888ytxx.coreandroid.mvi.SideEffect
import com.xxmrk888ytxx.corecompose.HandleSideEffect
import com.xxmrk888ytxx.corecompose.theme.AppSeedColors
import com.xxmrk888ytxx.settingsscreen.model.AppIcon
import com.xxmrk888ytxx.settingsscreen.model.UnlockScreenStyle
import com.xxmrk888ytxx.settingsscreen.model.BottomSheetState
import com.xxmrk888ytxx.settingsscreen.model.ScreenState
import com.xxmrk888ytxx.settingsscreen.model.SettingsScreenEvent
import com.xxmrk888ytxx.settingsscreen.model.SettingsScreenEvent.ConfirmSecurityChanges
import com.xxmrk888ytxx.settingsscreen.model.SettingsScreenSideEffect
import java.io.File
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    screenState: ScreenState,
    onEvent: (SettingsScreenEvent) -> Unit,
    sideEffect: Flow<SideEffect>
) {
    val context = LocalContext.current
    HandleSideEffect<SettingsScreenSideEffect>(sideEffect) { effect ->
        when(effect) {
            SettingsScreenSideEffect.OpenOpenSourceLicenses -> {
                val intent = Intent(context, OssLicensesMenuActivity::class.java)
                context.startActivity(intent)
            }
        }
    }
    var isLogsVisible by rememberSaveable {
        mutableStateOf(false)
    }
    var versionClickCounter by rememberSaveable {
        mutableIntStateOf(0)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            SettingsSection(stringResource(R.string.pc_client)) {
                SettingsItem(
                    title = stringResource(R.string.download_for_pc),
                    iconRes = R.drawable.portal,
                    useImageInsteadIcon = true,
                    subtitle = stringResource(R.string.install_the_app_on_your_computer_to_pair_your_device)
                ) {
                   onEvent(SettingsScreenEvent.OnDownloadPcClientClicked)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Security Section
            SettingsSection(title = stringResource(R.string.security)) {

                SettingsSwitchItem(
                    title = stringResource(R.string.biometric_unlock),
                    subtitle = stringResource(R.string.use_fingerprint_or_face_to_unlock),
                    iconRes = R.drawable.fingerprint,
                    checked = screenState.isBiometricProtectionEnabled,
                    enabled = screenState.isBiometricProtectionEnabled || screenState.isBiometricAuthAvailable,
                    errorText = if (!screenState.isBiometricAuthAvailable) stringResource(R.string.biometrics_is_not_configured_or_is_unavailable) else null,
                    onCheckedChange = { isChecked ->
                        onEvent(SettingsScreenEvent.OnBiometricProtectionStateChanged(isChecked))
                    }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.auto_biometric_on_notification_title),
                    subtitle = stringResource(R.string.auto_biometric_on_notification_desc),
                    iconRes = R.drawable.fingerprint,
                    checked = screenState.isAutoBiometricOnNotificationEnabled,
                    enabled = screenState.isBiometricProtectionEnabled,
                    onCheckedChange = { isChecked ->
                        onEvent(SettingsScreenEvent.OnAutoBiometricOnNotificationChanged(isChecked))
                    }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.prohibit_insecure_unlock_methods),
                    subtitle = stringResource(R.string.the_automatically_unlock_method_will_be_disabled_for_devices_where_this_unlock_method_is_currently_set_it_will_be_changed_to_notification),
                    iconRes = R.drawable.insecure,
                    checked = screenState.isUnsafeUnlockTypesDisabled,
                    isExpandable = true,
                    onCheckedChange = { isChecked ->
                        onEvent(SettingsScreenEvent.OnChangeUnsafeUnlockTypesState(isChecked))
                    }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.unpair_devices_if_the_biometric_environment_has_changed),
                    subtitle = stringResource(R.string.if_a_new_fingerprint_is_added_or_an_old_one_is_deleted_all_paired_devices_will_be_removed),
                    iconRes = R.drawable.encrypted,
                    checked = screenState.isRemovePairedClientsIfBiometricEnvironmentChangedEnabled,
                    enabled = screenState.isBiometricProtectionEnabled,
                    onCheckedChange = { isChecked ->
                        onEvent(SettingsScreenEvent.OnRemovePairedClientsIfBiometricEnvironmentStateChanged(isChecked))
                    }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.allow_password_unlock),
                    subtitle = stringResource(R.string.can_be_used_as_an_alternative_to_biometric_authentication),
                    iconRes = R.drawable.password,
                    checked = screenState.isAdditionalPasswordAuthEnabled,
                    enabled = screenState.isBiometricProtectionEnabled,
                    onCheckedChange = { isChecked ->
                        onEvent(SettingsScreenEvent.OnAdditionalPasswordAuthStateChanged(isChecked))
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSection(stringResource(R.string.customization)) {
                SettingsItem(
                    title = stringResource(R.string.theme),
                    iconRes = R.drawable.palette
                ) {
                    onEvent(SettingsScreenEvent.OnThemeClicked)
                }

                val currentAppIcon = AppIcon.fromId(screenState.currentAppIconId)
                SettingsItem(
                    title = stringResource(R.string.app_icon),
                    subtitle = stringResource(currentAppIcon.titleRes),
                    iconRes = R.drawable.portal
                ) {
                    onEvent(SettingsScreenEvent.OnAppIconClicked)
                }

                val customShortcutPhotoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        onEvent(SettingsScreenEvent.OnCustomAppIconPicked(uri.toString()))
                    }
                }

                SettingsItem(
                    title = stringResource(R.string.app_shortcut_custom_title),
                    subtitle = stringResource(R.string.app_shortcut_custom_desc),
                    iconRes = R.drawable.push_pin
                ) {
                    customShortcutPhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                val currentUnlockScreenStyle = UnlockScreenStyle.fromId(screenState.unlockScreenStyle)
                SettingsItem(
                    title = stringResource(R.string.unlock_screen_style),
                    subtitle = stringResource(currentUnlockScreenStyle.titleRes),
                    iconRes = R.drawable.lock_open
                ) {
                    onEvent(SettingsScreenEvent.OnUnlockScreenStyleClicked)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(stringResource(R.string.background_and_reliability)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.background_service_title),
                    subtitle = stringResource(R.string.background_service_desc),
                    iconRes = R.drawable.bolt,
                    checked = screenState.isBackgroundServiceEnabled,
                    onCheckedChange = { isChecked ->
                        onEvent(SettingsScreenEvent.OnBackgroundServiceStateChanged(isChecked))
                    }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.wakelock_title),
                    subtitle = stringResource(R.string.wakelock_desc),
                    iconRes = R.drawable.memory,
                    checked = screenState.isWakeLockEnabled,
                    onCheckedChange = { isChecked ->
                        onEvent(SettingsScreenEvent.OnWakeLockStateChanged(isChecked))
                    }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.watch_dog),
                    subtitle = stringResource(R.string.watch_dog_description),
                    iconRes = R.drawable.visibility,
                    checked = screenState.isWatchDogEnabled,
                    isExpandable = true,
                    onCheckedChange = { isChecked ->
                        onEvent(SettingsScreenEvent.OnWatchDogStateChanged(isChecked))
                    }
                )

                AnimatedVisibility(visible = screenState.isWatchDogEnabled) {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        SettingsSwitchItem(
                            title = stringResource(R.string.watchdog_fast_check_title),
                            subtitle = stringResource(R.string.watchdog_fast_check_desc),
                            iconRes = R.drawable.timer,
                            checked = screenState.isWatchDogFastCheckEnabled,
                            onCheckedChange = { isChecked ->
                                onEvent(SettingsScreenEvent.OnWatchDogFastCheckStateChanged(isChecked))
                            }
                        )

                        SettingsSwitchItem(
                            title = stringResource(R.string.watchdog_alarm_clock_title),
                            subtitle = stringResource(R.string.watchdog_alarm_clock_desc),
                            iconRes = R.drawable.visibility,
                            checked = screenState.isWatchDogAlarmClockEnabled,
                            onCheckedChange = { isChecked ->
                                onEvent(SettingsScreenEvent.OnWatchDogAlarmClockStateChanged(isChecked))
                            }
                        )
                    }
                }

                SettingsItem(
                    title = stringResource(R.string.type1_timeout_title),
                    subtitle = stringResource(R.string.type1_timeout_desc, screenState.type1UnlockTimeoutMinutes),
                    iconRes = R.drawable.timer,
                    onClick = {
                        onEvent(SettingsScreenEvent.OnType1TimeoutClicked)
                    }
                )

                SettingsItem(
                    title = stringResource(R.string.type1_retry_interval_title),
                    subtitle = stringResource(R.string.type1_retry_interval_desc, screenState.type1RetryIntervalSeconds),
                    iconRes = R.drawable.replay,
                    onClick = {
                        onEvent(SettingsScreenEvent.OnType1RetryIntervalClicked)
                    }
                )

                val conflictSubtitle = when (screenState.type2IncomingConflictBehavior) {
                    "defer" -> stringResource(R.string.conflict_behavior_defer_title)
                    "reject" -> stringResource(R.string.conflict_behavior_reject_title)
                    else -> stringResource(R.string.conflict_behavior_show_title)
                }
                SettingsItem(
                    title = stringResource(R.string.type2_conflict_title),
                    subtitle = conflictSubtitle,
                    iconRes = R.drawable.swap_calls,
                    onClick = {
                        onEvent(SettingsScreenEvent.OnType2ConflictClicked)
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // About App Section
            SettingsSection(title = stringResource(R.string.about_app)) {
                SettingsItem(
                    title = stringResource(R.string.troubleshooting),
                    iconRes = R.drawable.troubleshoot,
                    subtitle = stringResource(R.string.solutions_for_common_issues),
                    onClick = {
                        onEvent(SettingsScreenEvent.OpenTroubleshootingLink)
                    }
                )
                SettingsItem(
                    title = stringResource(R.string.terms_of_use),
                    iconRes = R.drawable.ic_terms,
                    onClick = { onEvent(SettingsScreenEvent.OnTermsClicked) }
                )
                SettingsItem(
                    title = stringResource(R.string.privacy_policy),
                    iconRes = R.drawable.ic_privacy,
                    onClick = { onEvent(SettingsScreenEvent.OnPrivacyClicked) }
                )
                SettingsItem(
                    title = stringResource(R.string.source_code_android),
                    subtitle = stringResource(R.string.github_repository),
                    iconRes = R.drawable.android,
                    onClick = { onEvent(SettingsScreenEvent.OnAndroidSourceCodeClick) }
                )
                SettingsItem(
                    title = stringResource(R.string.source_code_pc),
                    subtitle = stringResource(R.string.github_repository),
                    iconRes = R.drawable.desktop,
                    onClick = { onEvent(SettingsScreenEvent.OnPCSourceCodeClick) }
                )
                GithubProfileItem(
                    title = stringResource(R.string.android_app_developer),
                    subtitle = stringResource(R.string.xxmrk888ytxx),
                    avatarUrl = AvatarLink.ANDROID_DEVELOPER_LINK,
                    onClick = { onEvent(SettingsScreenEvent.OnAndroidDeveloperClick) }
                )
                GithubProfileItem(
                    title = stringResource(R.string.pc_client_developer),
                    subtitle = stringResource(R.string.xxkoksmenxx),
                    avatarUrl = AvatarLink.PC_DEVELOPER_LINK,
                    onClick = { onEvent(SettingsScreenEvent.OnPCDeveloperClicked) }
                )
                SettingsItem(
                    title = stringResource(R.string.open_source_licenses),
                    iconRes = R.drawable.code,
                    onClick = { onEvent(SettingsScreenEvent.OpenOpenSourceLicenses) }
                )
                SettingsItem(
                    title = stringResource(R.string.app_version),
                    subtitle = screenState.appVersion,
                    iconRes = R.drawable.ic_version,
                    onClick = {
                        versionClickCounter += 1
                        if (versionClickCounter == 5) {
                            isLogsVisible = true
                        }
                    }
                )
                AnimatedVisibility(isLogsVisible) {
                    SettingsItem(
                        title = stringResource(R.string.app_logs),
                        iconRes = R.drawable.ic_version,
                        onClick = { onEvent(SettingsScreenEvent.OnLogsClick) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    when(screenState.bottomSheetState) {
        is BottomSheetState.ConfirmSecurityChangesDialog -> ConfirmSecurityChangesDialog(
            isForEnablingSetting = screenState.bottomSheetState.isForEnablingSetting,
            onDismiss = { onEvent(SettingsScreenEvent.HideBottomSheet) },
            onConfirm = { onEvent(ConfirmSecurityChanges(screenState.bottomSheetState.actionAfterConfirm)) }
        )
        BottomSheetState.None -> {}
        is BottomSheetState.SelectThemeDialog -> SelectThemeDialog(
            onDismiss = { onEvent(SettingsScreenEvent.HideBottomSheet) },
            onThemeSelected = {
                onEvent(SettingsScreenEvent.OnThemeColorSelected(it))
            },
            selectedColor = screenState.bottomSheetState.selectedThemeColor
        )
        is BottomSheetState.SelectAppIconDialog -> SelectAppIconDialog(
            currentIconId = screenState.bottomSheetState.currentIconId,
            onDismiss = { onEvent(SettingsScreenEvent.HideBottomSheet) },
            onIconSelected = { onEvent(SettingsScreenEvent.OnAppIconSelected(it)) }
        )
        is BottomSheetState.SelectUnlockScreenStyleDialog -> SelectUnlockScreenStyleDialog(
            currentStyleId = screenState.bottomSheetState.currentStyleId,
            onDismiss = { onEvent(SettingsScreenEvent.HideBottomSheet) },
            onStyleSelected = { onEvent(SettingsScreenEvent.OnUnlockScreenStyleSelected(it)) }
        )
        is BottomSheetState.SelectType1TimeoutDialog -> SelectType1TimeoutDialog(
            currentMinutes = screenState.bottomSheetState.currentMinutes,
            onDismiss = { onEvent(SettingsScreenEvent.HideBottomSheet) },
            onTimeoutSelected = { onEvent(SettingsScreenEvent.OnType1TimeoutSelected(it)) }
        )
        is BottomSheetState.SelectType1RetryIntervalDialog -> SelectType1RetryIntervalDialog(
            currentSeconds = screenState.bottomSheetState.currentSeconds,
            onDismiss = { onEvent(SettingsScreenEvent.HideBottomSheet) },
            onIntervalSelected = { onEvent(SettingsScreenEvent.OnType1RetryIntervalSelected(it)) }
        )
        is BottomSheetState.SelectType2ConflictDialog -> SelectType2ConflictDialog(
            currentBehavior = screenState.bottomSheetState.currentBehavior,
            onDismiss = { onEvent(SettingsScreenEvent.HideBottomSheet) },
            onBehaviorSelected = { onEvent(SettingsScreenEvent.OnType2ConflictSelected(it)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelectThemeDialog(
    selectedColor: Color?, // null = System, Color.Unspecified = Random
    onDismiss: () -> Unit,
    onThemeSelected: (Color?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.choose_a_theme),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Standard Options: System, Default (Portal), and Random
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // System Button
                val isSystemSelected = selectedColor == null
                val systemContainerColor = if (isSystemSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                val systemContentColor = if (isSystemSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary

                OutlinedButton(
                    onClick = {
                        onThemeSelected(null)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = systemContainerColor,
                        contentColor = systemContentColor
                    ),
                    border = if (isSystemSelected) null else ButtonDefaults.outlinedButtonBorder()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.android),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.system),
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }

                // Standard / Default Portal Theme Button
                val isStandardSelected = selectedColor == AppSeedColors.DeepAmethyst
                val standardContainerColor = if (isStandardSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                val standardContentColor = if (isStandardSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary

                OutlinedButton(
                    onClick = {
                        onThemeSelected(AppSeedColors.DeepAmethyst)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = standardContainerColor,
                        contentColor = standardContentColor
                    ),
                    border = if (isStandardSelected) null else ButtonDefaults.outlinedButtonBorder()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.portal),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.standard),
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }

                // Random Button
                val isRandomSelected = selectedColor == Color.Unspecified
                val randomContainerColor = if (isRandomSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                val randomContentColor = if (isRandomSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary

                OutlinedButton(
                    onClick = {
                        onThemeSelected(Color.Unspecified)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = randomContainerColor,
                        contentColor = randomContentColor
                    ),
                    border = if (isRandomSelected) null else ButtonDefaults.outlinedButtonBorder()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.random),
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.colors),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Colors Grid using FlowRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppSeedColors.allColors.forEach { color ->
                    val isColorSelected = selectedColor == color

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable {
                                onThemeSelected(color)
                                onDismiss()
                            }
                            .border(
                                width = if (isColorSelected) 2.dp else 1.dp,
                                color = if (isColorSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.1f
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isColorSelected) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}


@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun GithubProfileItem(
    title: String,
    avatarUrl: String?,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val placeholder = painterResource(id = R.drawable.ic_developer)

        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Github Avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = placeholder,
                error = placeholder,
                fallback = placeholder
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    @DrawableRes iconRes: Int,
    subtitle: String? = null,
    useImageInsteadIcon: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (useImageInsteadIcon) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    @DrawableRes iconRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isExpandable: Boolean = false,
    collapsedMaxLines: Int = 2,
    errorText: String? = null,
    enabled: Boolean = true
) {
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.38f,
        label = "alpha_anim"
    )

    val isError = remember(errorText) { errorText != null }

    var isExpanded by remember { mutableStateOf(false) }
    var hasVisualOverflow by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(alpha)
                .animateContentSize()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpandable && !isExpanded) collapsedMaxLines else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        // Проверяем, обрезался ли текст
                        if (isExpandable && !isExpanded) {
                            hasVisualOverflow = textLayoutResult.hasVisualOverflow
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )

                if (isExpandable && (hasVisualOverflow || isExpanded)) {
                    Text(
                        text = if (isExpanded) stringResource(R.string.show_less) else stringResource(
                            R.string.read_more
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 4.dp, end = 8.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isExpanded = !isExpanded }
                            )
                            .padding(vertical = 4.dp)
                    )
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSecurityChangesDialog(
    isForEnablingSetting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.security),
                contentDescription = null,
                tint = if (isForEnablingSetting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.attention),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isForEnablingSetting)
                    stringResource(R.string.if_you_disable_this_setting_in_future_all_your_paired_devices_will_be_deleted_do_you_still_want_to_enable_it_note_enabling_this_setting_will_not_affect_your_paired_devices)
                    else stringResource(R.string.for_security_reasons_disabling_this_setting_will_result_in_the_removal_of_all_paired_devices_do_you_still_want_to_disable_it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isForEnablingSetting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    contentColor = if (isForEnablingSetting) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
                )
            ) {
                Text(if (isForEnablingSetting) stringResource(R.string.enable) else stringResource(R.string.disable))
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAppIconDialog(
    currentIconId: String,
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.choose_app_icon),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            AppIcon.entries.forEach { icon ->
                val isSelected = icon.id == currentIconId
                val containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
                val borderColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            onIconSelected(icon.id)
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = containerColor,
                    border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1F000000))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(icon.previewRes),
                                    contentDescription = stringResource(icon.titleRes),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(icon.titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(icon.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectUnlockScreenStyleDialog(
    currentStyleId: String,
    onDismiss: () -> Unit,
    onStyleSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.choose_unlock_screen_style),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            UnlockScreenStyle.entries.forEach { style ->
                val isSelected = style.id == currentStyleId
                val containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
                val borderColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            onStyleSelected(style.id)
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = containerColor,
                    border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (style) {
                                            UnlockScreenStyle.CLASSIC -> R.drawable.portal
                                            UnlockScreenStyle.FULLSCREEN -> R.drawable.lock_open
                                            UnlockScreenStyle.BOTTOM_SHEET -> R.drawable.desktop
                                        }
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(style.titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(style.descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        RadioButton(
                            selected = isSelected,
                            onClick = { onStyleSelected(style.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectType1TimeoutDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onTimeoutSelected: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val options = listOf(
        Pair(3, Pair(R.string.timeout_3_minutes, R.string.timeout_3_minutes_desc)),
        Pair(6, Pair(R.string.timeout_6_minutes, R.string.timeout_6_minutes_desc)),
        Pair(10, Pair(R.string.timeout_10_minutes, R.string.timeout_10_minutes_desc)),
        Pair(15, Pair(R.string.timeout_15_minutes, R.string.timeout_15_minutes_desc))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.choose_type1_timeout),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            options.forEach { (minutes, textPair) ->
                val (titleRes, descRes) = textPair
                val isSelected = minutes == currentMinutes
                val containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
                val borderColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTimeoutSelected(minutes) },
                    shape = RoundedCornerShape(20.dp),
                    color = containerColor,
                    border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.timer),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        RadioButton(
                            selected = isSelected,
                            onClick = { onTimeoutSelected(minutes) }
                        )
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectType1RetryIntervalDialog(
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onIntervalSelected: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val options = listOf(
        Pair(1, Pair(R.string.interval_1_sec, R.string.interval_1_sec_desc)),
        Pair(2, Pair(R.string.interval_2_sec, R.string.interval_2_sec_desc)),
        Pair(3, Pair(R.string.interval_3_sec, R.string.interval_3_sec_desc)),
        Pair(5, Pair(R.string.interval_5_sec, R.string.interval_5_sec_desc)),
        Pair(10, Pair(R.string.interval_10_sec, R.string.interval_10_sec_desc)),
        Pair(15, Pair(R.string.interval_15_sec, R.string.interval_15_sec_desc)),
        Pair(30, Pair(R.string.interval_30_sec, R.string.interval_30_sec_desc)),
        Pair(60, Pair(R.string.interval_60_sec, R.string.interval_60_sec_desc))
    )

    var customSecondsText by remember { mutableStateOf("") }
    val isCustomActive = options.none { it.first == currentSeconds }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.choose_type1_retry_interval),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            options.forEach { (seconds, textPair) ->
                val (titleRes, descRes) = textPair
                val isSelected = seconds == currentSeconds
                val containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
                val borderColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onIntervalSelected(seconds) },
                    shape = RoundedCornerShape(20.dp),
                    color = containerColor,
                    border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.replay),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        RadioButton(
                            selected = isSelected,
                            onClick = { onIntervalSelected(seconds) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom interval section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (isCustomActive) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    if (isCustomActive) 2.dp else 1.dp,
                    if (isCustomActive) MaterialTheme.colorScheme.primary else Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.replay),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.custom_interval),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.enter_seconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = customSecondsText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 4) {
                                    customSecondsText = input
                                }
                            },
                            placeholder = { Text(if (isCustomActive) "$currentSeconds" else "5") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val entered = customSecondsText.toIntOrNull()
                                if (entered != null && entered in 1..300) {
                                    onIntervalSelected(entered)
                                }
                            },
                            enabled = customSecondsText.toIntOrNull()?.let { it in 1..300 } == true,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.apply))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectType2ConflictDialog(
    currentBehavior: String,
    onDismiss: () -> Unit,
    onBehaviorSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val options = listOf(
        Triple("show", R.string.conflict_behavior_show_title, R.string.conflict_behavior_show_desc),
        Triple("defer", R.string.conflict_behavior_defer_title, R.string.conflict_behavior_defer_desc),
        Triple("reject", R.string.conflict_behavior_reject_title, R.string.conflict_behavior_reject_desc)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.choose_type2_conflict),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            options.forEach { (behavior, titleRes, descRes) ->
                val isSelected = behavior == currentBehavior
                val containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
                val borderColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onBehaviorSelected(behavior) },
                    shape = RoundedCornerShape(20.dp),
                    color = containerColor,
                    border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (behavior) {
                                            "defer" -> R.drawable.timer
                                            "reject" -> R.drawable.security
                                            else -> R.drawable.swap_calls
                                        }
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        RadioButton(
                            selected = isSelected,
                            onClick = { onBehaviorSelected(behavior) }
                        )
                    }
                }
            }
        }
    }
}
