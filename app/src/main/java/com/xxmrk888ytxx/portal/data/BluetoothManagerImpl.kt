package com.xxmrk888ytxx.portal.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.core.content.getSystemService
import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.portal.data.connection.RfcommBluetoothConnection
import com.xxmrk888ytxx.portal.domain.BluetoothManager
import com.xxmrk888ytxx.portal.domain.MacAddress
import com.xxmrk888ytxx.portal.domain.PermissionManager
import com.xxmrk888ytxx.portal.domain.connection.BluetoothConnection
import com.xxmrk888ytxx.portal.domain.model.PairedBluetoothDevice
import com.xxmrk888ytxx.portal.exception.BluetoothDisabledException
import com.xxmrk888ytxx.portal.exception.BluetoothNotSupportedException
import com.xxmrk888ytxx.portal.exception.BluetoothPermissionNotGrantedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class BluetoothManagerImpl @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) : BluetoothManager {

    private val cashedConnectionMap: MutableStateFlow<Map<MacAddress, BluetoothConnection>> = MutableStateFlow(emptyMap())
    private val observeCloseCashedConnectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val openConnectionMutex = Mutex()

    val bluetoothManager: android.bluetooth.BluetoothManager by lazy {
        context.getSystemService<android.bluetooth.BluetoothManager>()
            ?: throw BluetoothNotSupportedException()
    }
    val bluetoothAdapter: BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter.isEnabled

    private val _pairedDevices = MutableStateFlow<Set<MacAddress>?>(null)

    override val pairedDeviceMacAddresses: Flow<Set<MacAddress>?> = _pairedDevices.asStateFlow()

    private val updatePairedDeviceMutex = Mutex()

    @SuppressLint("MissingPermission")
    override suspend fun getPairedDevices(): List<PairedBluetoothDevice> {
        fastDebugLog("getPairedDevices")
        checkBluetoothStateAndPermission()
        return bluetoothAdapter.bondedDevices.map {
            PairedBluetoothDevice(it.name, it.address)
        }.also { fastDebugLog("Paired devices: $it") }
    }

    override suspend fun openConnection(macAddress: String): BluetoothConnection = withContext(Dispatchers.IO) {
        checkBluetoothStateAndPermission()
        openConnectionMutex.withLock {
            val cachedConnection = getCashedConnection(macAddress)
            if (cachedConnection != null && !cachedConnection.isClosed.value) {
                cachedConnection.acquire()
                fastDebugLog("Reusing cached active Bluetooth connection for $macAddress")
                return@withContext cachedConnection
            } else if (cachedConnection != null) {
                cashedConnectionMap.update { it.toMutableMap().apply { remove(macAddress) } }
            }
        }

        val normalizedTargetMac = macAddress.replace(":", "")
        val androidBluetoothDevice =
            bluetoothAdapter.bondedDevices.firstOrNull { it.address.equals(macAddress, ignoreCase = true) }
                ?: bluetoothAdapter.bondedDevices.firstOrNull {
                    it.address.replace(":", "").equals(normalizedTargetMac, ignoreCase = true)
                }
                ?: throw IllegalArgumentException("Device $macAddress not paired")

        val socket = androidBluetoothDevice.createRfcommSocketToServiceRecord(
            UUID.fromString(PORTAL_BLUETOOTH_SERVICE_UUID)
        )
        val watchdogJob = CoroutineScope(Dispatchers.IO).launch {
            delay(CONNECT_TIMEOUT_MS)
            try {
                fastDebugLog("Bluetooth connect watchdog triggered after ${CONNECT_TIMEOUT_MS}ms for $macAddress")
                socket.close()
            } catch (_: Exception) {}
        }
        var isConnected = false
        val cancelHandler = coroutineContext[Job]?.invokeOnCompletion { cause ->
            if (cause != null && !isConnected) {
                try {
                    fastDebugLog("Bluetooth connection cancelled during connect for $macAddress")
                    socket.close()
                } catch (_: Exception) {}
            }
        }
        try {
            fastDebugLog("Connecting RFCOMM socket for ${androidBluetoothDevice.name} ($macAddress)...")
            socket.connect()
            isConnected = true
            cancelHandler?.dispose()
            watchdogJob.cancel()
            fastDebugLog("RFCOMM socket connected successfully for ${androidBluetoothDevice.name} ($macAddress)")

            val connection = RfcommBluetoothConnection(socket)
            openConnectionMutex.withLock {
                val existing = getCashedConnection(macAddress)
                if (existing != null && !existing.isClosed.value) {
                    existing.acquire()
                    connection.release()
                    return@withContext existing
                }
                addCashedConnection(macAddress, connection)
            }
            return@withContext connection
        } catch (e: Throwable) {
            cancelHandler?.dispose()
            watchdogJob.cancel()
            try { socket.close() } catch (_: Exception) {}
            fastDebugLog("Failed to connect RFCOMM socket for $macAddress: ${e.message}")
            throw e
        }

    }
    override suspend fun updatePairedDeviceMacAddresses() = withContext(Dispatchers.IO) {
        updatePairedDeviceMutex.withLock {
            try {
    
                checkBluetoothStateAndPermission()
                _pairedDevices.value = bluetoothAdapter.bondedDevices.map { it.address }.toSet()
            }catch (e: Exception) {
                fastDebugLog("Failed to update paired devices. Error: $e")
                _pairedDevices.value = null
            }
        }
    }

    private suspend fun getCashedConnection(macAddress: MacAddress): BluetoothConnection? {
        return cashedConnectionMap.value[macAddress]
    }

    private suspend fun addCashedConnection(
        macAddress: MacAddress,
        connection: BluetoothConnection
    ) {
        cashedConnectionMap.update { it.toMutableMap().apply { put(macAddress, connection)  } }
        observeCloseCashedConnectionScope.launch {
            connection.isClosed.first { isClosed -> isClosed }
            openConnectionMutex.withLock { cashedConnectionMap.update { it.toMutableMap().apply { remove(macAddress) } } }
        }
    }

    private fun checkBluetoothStateAndPermission() {
        if (!permissionManager.isNearbyDevicesPermissionGranted) throw BluetoothPermissionNotGrantedException()
        if (!isBluetoothEnabled) throw BluetoothDisabledException()
    }

    companion object {
        const val PORTAL_BLUETOOTH_SERVICE_UUID = "E0CBF06C-CD8B-4647-BB8A-263B43F0F974"
        const val CONNECT_TIMEOUT_MS = 3500L
    }

}
