package com.xxmrk888ytxx.portal.providedContract.deviceConfigurationScreen

import com.xxmrk888ytxx.deviceconfigurationscreen.contract.ProvideDeviceInfoContract
import com.xxmrk888ytxx.deviceconfigurationscreen.exception.DeviceNotFoundException
import com.xxmrk888ytxx.deviceconfigurationscreen.model.BluetoothDeviceOption
import com.xxmrk888ytxx.deviceconfigurationscreen.model.Device
import com.xxmrk888ytxx.portal.domain.BluetoothDeviceRepository
import com.xxmrk888ytxx.portal.domain.BluetoothManager
import com.xxmrk888ytxx.portal.domain.CertificateManager
import com.xxmrk888ytxx.portal.domain.DeviceSettingsRepository
import com.xxmrk888ytxx.portal.domain.SettingsRepository
import com.xxmrk888ytxx.portal.domain.WifiDeviceRepository
import com.xxmrk888ytxx.portal.domain.model.BluetoothDevice
import com.xxmrk888ytxx.portal.domain.model.DeviceSettings
import com.xxmrk888ytxx.portal.domain.model.PortalSettings
import com.xxmrk888ytxx.portal.domain.model.UnlockMethod
import com.xxmrk888ytxx.portal.domain.model.WifiDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import com.xxmrk888ytxx.deviceconfigurationscreen.model.UnlockMethod as DeviceConfigurationUnlockMethod

class ProvideDeviceInfoContractImpl @Inject constructor(
    private val wifiDeviceRepository: WifiDeviceRepository,
    private val certificateRepository: CertificateManager,
    private val deviceSettingsRepository: DeviceSettingsRepository,
    private val bluetoothDeviceRepository: BluetoothDeviceRepository,
    private val bluetoothManager: BluetoothManager,
    private val settingsRepository: SettingsRepository
) : ProvideDeviceInfoContract {
    override suspend fun provideDeviceInfo(clientId: String): Flow<Device> {
        val wifiDevice = wifiDeviceRepository.getDeviceById(clientId)
        val bluetoothDevice = bluetoothDeviceRepository.getDeviceById(clientId)
        val deviceSetting = deviceSettingsRepository.getDeviceSettingsByDeviceId(clientId)
        val allBtDevices = bluetoothDeviceRepository.devices
        val portalSettings = settingsRepository.portalSettings

        return combine<Any?, Device>(
            wifiDevice,
            bluetoothDevice,
            deviceSetting,
            bluetoothManager.pairedDeviceMacAddresses,
            allBtDevices,
            portalSettings
        ) { array ->
            @Suppress("UNCHECKED_CAST")
            val currentWifiDevice = array[0] as WifiDevice?
            @Suppress("UNCHECKED_CAST")
            val currentBluetoothDevice = array[1] as BluetoothDevice?
            val deviceSettings = (array[2] as DeviceSettings?) ?: throw DeviceNotFoundException(clientId)
            @Suppress("UNCHECKED_CAST")
            val pairedDeviceMacAddresses = array[3] as Set<String>?
            @Suppress("UNCHECKED_CAST")
            val allBluetoothDevices = (array[4] as? List<BluetoothDevice>) ?: emptyList()
            val settings = array[5] as PortalSettings

            when {
                currentWifiDevice != null -> {
                    val availableBtOptions = listOf(
                        BluetoothDeviceOption(clientId = null, name = "Автоматически по имени", macAddress = null)
                    ) + allBluetoothDevices.map {
                        BluetoothDeviceOption(clientId = it.clientId, name = it.name, macAddress = it.macAddress)
                    }

                    val linkedBtName = if (!deviceSettings.linkedBluetoothDeviceId.isNullOrEmpty()) {
                        allBluetoothDevices.firstOrNull { it.clientId == deviceSettings.linkedBluetoothDeviceId }?.name
                            ?: "Устройство не найдено"
                    } else {
                        val matchingByName = allBluetoothDevices.firstOrNull {
                            it.name.equals(currentWifiDevice.deviceName, ignoreCase = true)
                        } ?: if (allBluetoothDevices.size == 1) allBluetoothDevices.first() else null
                        if (matchingByName != null) "Автоматически (${matchingByName.name})"
                        else null
                    }

                    Device.WifiDevice(
                        clientId = currentWifiDevice.clientId,
                        deviceName = currentWifiDevice.deviceName,
                        host = currentWifiDevice.host,
                        clientCertificateFingerprint = certificateRepository.getX509CertificateFingerprint(
                            currentWifiDevice.clientCertificate.x509Certificate
                        ),
                        awaitUnlockRequests = deviceSettings.awaitUnlockRequests,
                        serverCertificateFingerprint = currentWifiDevice.serverCertificateFingerprint,
                        searchIpDynamically = deviceSettings.searchIpDynamically,
                        unlockMethod = deviceSettings.unlockMethod.toDeviceConfigurationUnlockMethod(),
                        wolMacAddress = currentWifiDevice.wolMacAddress?.filter { it != ':' },
                        showUnlockScreenOrUnlockOnlyWhenScreenUnlocked = deviceSettings.showUnlockScreenOrUnlockOnlyWhenScreenUnlocked,
                        forwardUnlockRequestsToWear = deviceSettings.forwardUnlockRequestsToWear,
                        isMixedUnlockEnabled = deviceSettings.isMixedUnlockEnabled,
                        linkedBluetoothDeviceId = deviceSettings.linkedBluetoothDeviceId,
                        linkedBluetoothDeviceName = linkedBtName,
                        isMixedUnlockGloballyEnabled = settings.isMixedUnlockEnabled,
                        availableBluetoothDevices = availableBtOptions
                    )
                }

                currentBluetoothDevice != null -> Device.BluetoothDevice(
                    clientId = currentBluetoothDevice.clientId,
                    deviceName = currentBluetoothDevice.name,
                    macAddress = currentBluetoothDevice.macAddress,
                    awaitUnlockRequests = deviceSettings.awaitUnlockRequests,
                    unlockMethod = deviceSettings.unlockMethod.toDeviceConfigurationUnlockMethod(),
                    isPaired = pairedDeviceMacAddresses?.contains(currentBluetoothDevice.macAddress) ?: true,
                    showUnlockScreenOrUnlockOnlyWhenScreenUnlocked = deviceSettings.showUnlockScreenOrUnlockOnlyWhenScreenUnlocked,
                    forwardUnlockRequestsToWear = deviceSettings.forwardUnlockRequestsToWear
                )

                else -> throw DeviceNotFoundException(clientId)
            }
        }
    }

    private fun UnlockMethod.toDeviceConfigurationUnlockMethod(): DeviceConfigurationUnlockMethod {
        return when (this) {
            is UnlockMethod.Automatic -> DeviceConfigurationUnlockMethod.Automatic
            is UnlockMethod.ConfirmationScreen -> DeviceConfigurationUnlockMethod.ConfirmationScreen
            is UnlockMethod.Notification -> DeviceConfigurationUnlockMethod.Notification
        }
    }
}
