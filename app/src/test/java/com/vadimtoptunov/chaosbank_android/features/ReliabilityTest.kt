package com.vadimtoptunov.chaosbank_android.features

import com.vadimtoptunov.chaosbank_android.core.backend.BackendError
import com.vadimtoptunov.chaosbank_android.core.backend.BackendException
import com.vadimtoptunov.chaosbank_android.core.backend.MockBackend
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.DefectRegistry
import com.vadimtoptunov.chaosbank_android.core.exercises.Exercises
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.support.CoroutineTest
import com.vadimtoptunov.chaosbank_android.support.servicesWith
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class OfflineBackendTest {
    private fun backend() = MockBackend(latencyMs = 0)

    @Test fun offlineWrites_throwOffline() = runTest {
        val b = backend()
        b.setOffline(true)
        val e = runCatching { b.transfer(Currency.EUR, BigDecimal("10"), "Alex", "", "k") }.exceptionOrNull()
        assertTrue(e is BackendException && e.error == BackendError.offline)
    }

    @Test fun offlineReads_stillServeCachedData() = runTest {
        val b = backend()
        b.setOffline(true)
        assertEquals(3, b.fetchAccounts().size)
        assertTrue(b.fetchTransactions().isNotEmpty())
    }

    @Test fun backOnline_restoresWrites() = runTest {
        val b = backend()
        b.setOffline(true)
        b.setOffline(false)
        val tx = b.transfer(Currency.EUR, BigDecimal("10"), "Alex", "", "k")
        assertNotNull(tx)
    }

    @Test fun offlineBlocks_depositExchangeOrder() = runTest {
        val b = backend(); b.setOffline(true)
        assertTrue(runCatching { b.deposit(Currency.EUR, BigDecimal("5")) }.exceptionOrNull() is BackendException)
        assertTrue(runCatching { b.exchange(Currency.EUR, Currency.USD, BigDecimal("5"), BigDecimal("5")) }.exceptionOrNull() is BackendException)
    }
}

class OfflineServicesTest : CoroutineTest() {
    @Test fun setOffline_flipsFlagAndBackend() = runTest {
        val services = servicesWith()
        assertFalse(services.offline)
        services.enableOffline(true)
        assertTrue(services.offline)
        assertTrue(runCatching { services.backend.transfer(Currency.EUR, BigDecimal("10"), "A", "", "k") }.exceptionOrNull() is BackendException)
    }
}

class ReliabilityCatalogTest {
    @Test fun newDefects_areRegistered() {
        assertNotNull(DefectRegistry.defect(DefectId.flakyAnimation))
        assertNotNull(DefectRegistry.defect(DefectId.offlineBannerMissing))
    }

    @Test fun newDefects_haveExercises() {
        val ids = Exercises.all.flatMap { it.defects }.toSet()
        assertTrue(DefectId.flakyAnimation.name in ids)
        assertTrue(DefectId.offlineBannerMissing.name in ids)
    }
}
