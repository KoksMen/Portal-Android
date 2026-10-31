package com.xxmrk888ytxx.portal.domain

import com.xxmrk888ytxx.portal.domain.model.BluetoothDevice
import com.xxmrk888ytxx.portal.domain.model.BluetoothPairResult
import com.xxmrk888ytxx.portal.domain.model.PairedBluetoothDevice

interface BluetoothPortalApi {
    suspend fun pair(pairedBluetoothDevice: PairedBluetoothDevice, pairCode: String): BluetoothPairResult
    suspend fun unlock(bluetoothDevice: BluetoothDevice, timeoutMs: Long = DEFAULT_TIMEOUT): Boolean

    companion object {
        const val DEFAULT_TIMEOUT = 6000L
    }
}
