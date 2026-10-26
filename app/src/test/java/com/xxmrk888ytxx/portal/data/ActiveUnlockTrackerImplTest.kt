package com.xxmrk888ytxx.portal.data

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
}
