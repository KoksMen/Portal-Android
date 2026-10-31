package com.xxmrk888ytxx.deviceconfigurationscreen.model

sealed interface BottomSheetDialogState {
    data object None: BottomSheetDialogState
    data object DeleteDevice: BottomSheetDialogState
    data class SelectLinkedBluetoothDevice(
        val currentSelectedId: String?,
        val devices: List<BluetoothDeviceOption>
    ) : BottomSheetDialogState
}
