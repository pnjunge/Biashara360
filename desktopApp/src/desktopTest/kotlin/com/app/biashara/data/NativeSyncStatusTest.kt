package com.app.biashara.data

import com.app.biashara.data.remote.NativeSyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class NativeSyncStatusTest {
    @BeforeTest fun reset() = NativeSyncStatus.reset()

    @Test fun failurePersistsUntilThatResourceRecovers() = runBlocking {
        assertTrue(NativeSyncStatus.track<Unit>("Orders") { error("offline") }.isFailure)
        NativeSyncStatus.track("Products") { 1 }
        assertEquals(setOf("Orders"), NativeSyncStatus.state.value.failures)
        assertNotNull(NativeSyncStatus.state.value.lastSuccess)
        NativeSyncStatus.track("Orders") {
            assertEquals(1, NativeSyncStatus.state.value.active)
        }
        assertTrue(NativeSyncStatus.state.value.failures.isEmpty())
        assertEquals(0, NativeSyncStatus.state.value.active)
    }

    @Test fun previousSessionCannotOverwriteNewSession() = runBlocking {
        NativeSyncStatus.track("Orders") { NativeSyncStatus.reset() }
        assertNull(NativeSyncStatus.state.value.lastSuccess)
        assertEquals(0, NativeSyncStatus.state.value.active)
    }

    @Test fun cancellationDoesNotReportFailureOrSuccess() = runBlocking {
        assertFailsWith<CancellationException> {
            NativeSyncStatus.track<Unit>("Orders") { throw CancellationException() }
        }
        assertEquals(0, NativeSyncStatus.state.value.active)
        assertTrue(NativeSyncStatus.state.value.failures.isEmpty())
        assertNull(NativeSyncStatus.state.value.lastSuccess)
    }
}
