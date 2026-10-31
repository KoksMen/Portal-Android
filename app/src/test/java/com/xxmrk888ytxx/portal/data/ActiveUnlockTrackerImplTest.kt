package com.xxmrk888ytxx.portal.data

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.xxmrk888ytxx.portal.domain.model.UnlockServiceRequest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveUnlockTrackerImplTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
    }

    @Test
    fun notifyUnlockSuccess_emitsClientId() = runTest {
        val tracker = ActiveUnlockTrackerImpl()
        val received = mutableListOf<String>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            tracker.unlockSuccessEvents.collect { received.add(it) }
        }

        tracker.notifyUnlockSuccess("test-client-1")
        assertEquals(listOf("test-client-1"), received)
        job.cancel()
    }

    @Test
    fun unregisterType1Unlock_whenSuccessful_emitsUnlockSuccess() = runTest {
        val tracker = ActiveUnlockTrackerImpl()
        val received = mutableListOf<String>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            tracker.unlockSuccessEvents.collect { received.add(it) }
        }

        tracker.registerType1Unlock("device-123")
        assertTrue(tracker.isType1UnlockActive("device-123"))

        tracker.unregisterType1Unlock("device-123", wasSuccessful = true)
        assertEquals(listOf("device-123"), received)
        job.cancel()
    }

    @Test
    fun clearAllType1Unlocks_clearsAllActiveUnlocks() = runTest {
        val tracker = ActiveUnlockTrackerImpl()
        tracker.registerType1Unlock("device-1")
        tracker.registerType1Unlock("device-2")

        assertTrue(tracker.isType1UnlockActive("device-1"))
        assertTrue(tracker.isType1UnlockActive("device-2"))
        assertTrue(tracker.isAnyType1UnlockActive())

        tracker.clearAllType1Unlocks()

        assertFalse(tracker.isType1UnlockActive("device-1"))
        assertFalse(tracker.isType1UnlockActive("device-2"))
        assertFalse(tracker.isAnyType1UnlockActive())
    }

    @Test
    fun pollAllDeferredType2Requests_returnsAllAndClears() = runTest {
        val tracker = ActiveUnlockTrackerImpl()
        val req1 = UnlockServiceRequest.Auth(requestId = "req-1")
        val req2 = UnlockServiceRequest.Auth(requestId = "req-2")

        tracker.deferType2Request("client-1", req1)
        tracker.deferType2Request("client-2", req2)

        val polled = tracker.pollAllDeferredType2Requests()
        assertEquals(2, polled.size)
        assertEquals(req1, polled["client-1"])
        assertEquals(req2, polled["client-2"])

        // Second poll should return empty
        val polledAgain = tracker.pollAllDeferredType2Requests()
        assertTrue(polledAgain.isEmpty())
    }
}
