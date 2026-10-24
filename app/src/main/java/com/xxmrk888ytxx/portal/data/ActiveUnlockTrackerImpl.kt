package com.xxmrk888ytxx.portal.data

import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.portal.domain.ActiveUnlockTracker
import com.xxmrk888ytxx.portal.domain.model.UnlockServiceRequest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class ActiveUnlockTrackerImpl @Inject constructor() : ActiveUnlockTracker {

    private val activeType1Unlocks = ConcurrentHashMap.newKeySet<String>()
    private val deferredType2Requests = ConcurrentHashMap<String, UnlockServiceRequest>()

    override fun registerType1Unlock(clientId: String) {
        activeType1Unlocks.add(clientId)
        fastDebugLog("ActiveUnlockTracker: Registered Type 1 unlock for $clientId")
    }

    override fun unregisterType1Unlock(clientId: String, wasSuccessful: Boolean) {
        activeType1Unlocks.remove(clientId)
        fastDebugLog("ActiveUnlockTracker: Unregistered Type 1 unlock for $clientId, wasSuccessful=$wasSuccessful")
        if (wasSuccessful) {
            // Drop deferred requests if Type 1 succeeded!
            deferredType2Requests.remove(clientId)
        }
    }

    override fun isType1UnlockActive(clientId: String): Boolean {
        return activeType1Unlocks.contains(clientId)
    }

    override fun deferType2Request(clientId: String, request: UnlockServiceRequest) {
        deferredType2Requests[clientId] = request
        fastDebugLog("ActiveUnlockTracker: Deferred Type 2 request for $clientId: $request")
    }

    override fun pollDeferredType2Request(clientId: String): UnlockServiceRequest? {
        return deferredType2Requests.remove(clientId)
    }
}
