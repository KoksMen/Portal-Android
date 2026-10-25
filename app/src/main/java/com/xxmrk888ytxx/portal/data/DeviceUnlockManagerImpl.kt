package com.xxmrk888ytxx.portal.data

import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.coreandroid.findIpInArpTable
import com.xxmrk888ytxx.coreandroid.runCatching
import com.xxmrk888ytxx.portal.domain.BluetoothPortalApi
import com.xxmrk888ytxx.portal.domain.DeviceSettingsRepository
import com.xxmrk888ytxx.portal.domain.DeviceUnlockManager
import com.xxmrk888ytxx.portal.domain.MdnsManager
import com.xxmrk888ytxx.portal.domain.WifiDeviceRepository
import com.xxmrk888ytxx.portal.domain.WifiPortalApi
import com.xxmrk888ytxx.portal.domain.model.BluetoothDevice
import com.xxmrk888ytxx.portal.domain.model.WifiDevice
import com.xxmrk888ytxx.portal.exception.UnlockRequestRejectedException
import com.xxmrk888ytxx.portal.utils.waitHostForClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeviceUnlockManagerImpl @Inject constructor(
    private val wifiPortalApi: WifiPortalApi,
    private val mdnsManager: MdnsManager,
    private val deviceSettingsRepository: DeviceSettingsRepository,
    private val wifiDeviceRepository: WifiDeviceRepository,
    private val bluetoothPortalApi: BluetoothPortalApi
) : DeviceUnlockManager {
    override suspend fun unlockWifiDevice(wifiDevice: WifiDevice): Result<Unit> =
        runCatching(Dispatchers.IO) {
            val settings =
                deviceSettingsRepository.getDeviceSettingsByDeviceId(wifiDevice.clientId).first()
                    ?: throw IllegalStateException("Device haven't settings")

            val dynamicHost = if (settings.searchIpDynamically) {
                val cached = mdnsManager.getCachedHost(wifiDevice.clientId)
                if (cached != null) {
                    fastDebugLog("In unlockWifiDevice cached mDNS host: $cached")
                    cached
                } else {
                    val arpHost = wifiDevice.wolMacAddress?.let { findIpInArpTable(it) }
                    if (arpHost != null) {
                        fastDebugLog("In unlockWifiDevice ARP resolved host: $arpHost for MAC ${wifiDevice.wolMacAddress}")
                        arpHost
                    } else if (wifiDevice.host.isEmpty()) {
                        mdnsManager.waitHostForClient(wifiDevice.clientId, timeoutMs = 1000L)
                    } else null
                }
            } else null

            val primaryHost = dynamicHost ?: wifiDevice.host
            fastDebugLog("In unlockWifiDevice attempting primary host: $primaryHost (dynamic: $dynamicHost, saved: ${wifiDevice.host})")

            val primaryResult = wifiPortalApi.unlock(
                host = primaryHost,
                clientId = wifiDevice.clientId,
                serverCertificateHash = wifiDevice.serverCertificateFingerprint,
                clientCertificate = wifiDevice.clientCertificate
            )

            if (primaryResult.isSuccess) {
                if (dynamicHost != null && dynamicHost != wifiDevice.host) {
                    fastDebugLog("Updating saved host from ${wifiDevice.host} to $dynamicHost")
                    wifiDeviceRepository.updateHost(wifiDevice.clientId, dynamicHost)
                }
                return@runCatching primaryResult.getOrThrow()
            }

            val fallbackHost = if (primaryHost == dynamicHost && wifiDevice.host.isNotEmpty() && wifiDevice.host != dynamicHost) {
                wifiDevice.host
            } else if (primaryHost == wifiDevice.host && dynamicHost != null && dynamicHost != wifiDevice.host) {
                dynamicHost
            } else null

            if (fallbackHost != null) {
                fastDebugLog("Primary host $primaryHost failed (${primaryResult.exceptionOrNull()?.message}). Trying fallback host: $fallbackHost")
                val fallbackResult = wifiPortalApi.unlock(
                    host = fallbackHost,
                    clientId = wifiDevice.clientId,
                    serverCertificateHash = wifiDevice.serverCertificateFingerprint,
                    clientCertificate = wifiDevice.clientCertificate
                )
                if (fallbackResult.isSuccess) {
                    if (fallbackHost == dynamicHost) {
                        fastDebugLog("Updating saved host from ${wifiDevice.host} to $dynamicHost")
                        wifiDeviceRepository.updateHost(wifiDevice.clientId, dynamicHost)
                    }
                    return@runCatching fallbackResult.getOrThrow()
                }
            }

            primaryResult.getOrThrow()
        }

    override suspend fun unlockBluetoothDevice(bluetoothDevice: BluetoothDevice): Result<Unit> =
        runCatching(
            Dispatchers.IO
        ) {
            if (!bluetoothPortalApi.unlock(bluetoothDevice)) {
                throw UnlockRequestRejectedException()
            }
        }
}
