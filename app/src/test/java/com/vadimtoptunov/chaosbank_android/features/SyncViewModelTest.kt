package com.vadimtoptunov.chaosbank_android.features

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.features.sync.SyncViewModel
import com.vadimtoptunov.chaosbank_android.support.CoroutineTest
import com.vadimtoptunov.chaosbank_android.support.servicesWith
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncViewModelTest : CoroutineTest() {

    @Test fun clean_noUpdatesLost() = runTest {
        val vm = SyncViewModel(servicesWith())
        vm.reset()
        vm.runConcurrent()
        assertEquals(vm.concurrency, vm.counter)
    }

    @Test fun syncLostUpdate_losesUpdates() = runTest {
        val vm = SyncViewModel(servicesWith(DefectId.syncLostUpdate))
        vm.reset()
        vm.runConcurrent()
        assertTrue("expected lost updates under the race", vm.counter < vm.concurrency)
        assertTrue(vm.counter >= 1)
    }

    @Test fun reset_zeroesCounter() = runTest {
        val vm = SyncViewModel(servicesWith())
        vm.runConcurrent()
        vm.reset()
        assertEquals(0, vm.counter)
    }
}
