package com.xxmrk888ytxx.portal.domain

import com.xxmrk888ytxx.portal.domain.model.UnlockServiceRequest
import kotlinx.coroutines.flow.SharedFlow

interface ActiveUnlockTracker {
    val unlockSuccessEvents: SharedFlow<String>
    fun notifyUnlockSuccess(clientId: String)
    fun registerType1Unlock(clientId: String)
    fun unregisterType1Unlock(clientId: String, wasSuccessful: Boolean)
    fun clearAllType1Unlocks()
    fun isType1UnlockActive(clientId: String): Boolean
    fun isAnyType1UnlockActive(): Boolean = false
    fun deferType2Request(clientId: String, request: UnlockServiceRequest)
    fun pollDeferredType2Request(clientId: String): UnlockServiceRequest?
    fun pollAllDeferredType2Requests(): Map<String, UnlockServiceRequest>
}
