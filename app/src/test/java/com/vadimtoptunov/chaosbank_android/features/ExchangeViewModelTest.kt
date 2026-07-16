package com.vadimtoptunov.chaosbank_android.features

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.features.exchange.ExchangeViewModel
import com.vadimtoptunov.chaosbank_android.support.CoroutineTest
import com.vadimtoptunov.chaosbank_android.support.servicesWith
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ExchangeViewModelTest : CoroutineTest() {

    private fun vm(vararg d: DefectId) = ExchangeViewModel(servicesWith(*d))

    @Test fun clean_rateIsEurToUsd() = runTest {
        val vm = vm(); vm.load()
        assertEquals(0, BigDecimal("1.08").compareTo(vm.rate.stripTrailingZeros()))
    }

    @Test fun exchangeInverseRate_usesWrongDirection() = runTest {
        val vm = vm(DefectId.exchangeInverseRate); vm.load()
        assertTrue(vm.rate < BigDecimal.ONE) // USD->EUR < 1
    }

    @Test fun fee_isHalfPercent() = runTest {
        val vm = vm(); vm.load(); vm.amountText = "100"
        assertEquals(0, BigDecimal("0.50").compareTo(vm.fee.amount))
    }

    @Test fun clean_youGetIsNetTimesRate() = runTest {
        val vm = vm(); vm.load(); vm.amountText = "100"
        assertEquals(0, BigDecimal("107.46").compareTo(vm.youGet.amount))
    }

    @Test fun youGetShowsGross_skipsFee() = runTest {
        val vm = vm(DefectId.youGetShowsGross); vm.load(); vm.amountText = "100"
        assertEquals(0, BigDecimal("108.00").compareTo(vm.youGet.amount))
    }

    @Test fun canExecute_requiresDifferentCurrencies() = runTest {
        val vm = vm(); vm.load(); vm.amountText = "100"; vm.get = Currency.EUR
        assertFalse(vm.canExecute)
    }

    @Test fun exchangeSameCurrencyAllowed_permitsEqual() = runTest {
        val vm = vm(DefectId.exchangeSameCurrencyAllowed); vm.load(); vm.amountText = "100"; vm.get = Currency.EUR
        assertTrue(vm.canExecute)
    }

    @Test fun execute_debitsAndCredits() = runTest {
        val services = servicesWith()
        val vm = ExchangeViewModel(services); vm.load(); vm.amountText = "100"
        val eur = services.backend.fetchAccount(Currency.EUR)!!.balance
        vm.execute()
        assertTrue(vm.succeeded)
        assertEquals(0, eur.minus(BigDecimal("100")).compareTo(services.backend.fetchAccount(Currency.EUR)!!.balance))
    }

    @Test fun canExecute_falseWithoutAmount() = runTest {
        val vm = vm(); vm.load()
        assertFalse(vm.canExecute)
    }
}
