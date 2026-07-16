package com.vadimtoptunov.chaosbank_android.features

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.features.home.HomeViewModel
import com.vadimtoptunov.chaosbank_android.support.CoroutineTest
import com.vadimtoptunov.chaosbank_android.support.servicesWith
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest : CoroutineTest() {

    @Test fun clean_loadsFourRecentAndPositiveTotal() = runTest {
        val vm = HomeViewModel(servicesWith())
        vm.load()
        assertEquals(4, vm.recent.size)
        assertTrue(vm.totalBalance.amount.signum() > 0)
    }

    @Test fun recentActivityShowsTwo_trimsToTwo() = runTest {
        val vm = HomeViewModel(servicesWith(DefectId.recentActivityShowsTwo))
        vm.load()
        assertEquals(2, vm.recent.size)
    }

    @Test fun homeTotalOmitsAccount_lowersTotal() = runTest {
        val clean = HomeViewModel(servicesWith()).apply { load() }.totalBalance.amount
        val buggy = HomeViewModel(servicesWith(DefectId.homeTotalOmitsAccount)).apply { load() }.totalBalance.amount
        assertTrue(buggy < clean)
    }

    @Test fun balanceFloorRounded_hasNoFraction() = runTest {
        val vm = HomeViewModel(servicesWith(DefectId.balanceFloorRounded))
        vm.load()
        assertEquals(0, vm.totalBalance.amount.stripTrailingZeros().scale().coerceAtLeast(0))
    }

    @Test fun todayChangeSignFlipped_flipsSign() = runTest {
        val clean = HomeViewModel(servicesWith()).apply { load() }.todayChange.amount
        val buggy = HomeViewModel(servicesWith(DefectId.todayChangeSignFlipped)).apply { load() }.todayChange.amount
        assertTrue(clean.signum() > 0 && buggy.signum() < 0)
    }

    @Test fun balanceWrongCurrencySymbol_showsEuroForUsd() = runTest {
        val vm = HomeViewModel(servicesWith(DefectId.balanceWrongCurrencySymbol))
        vm.selectedCurrency = Currency.USD
        vm.load()
        assertTrue(vm.totalBalanceText.startsWith("€"))
    }

    @Test fun cleanText_matchesSelectedCurrencySymbol() = runTest {
        val vm = HomeViewModel(servicesWith())
        vm.selectedCurrency = Currency.USD
        vm.load()
        assertTrue(vm.totalBalanceText.startsWith("$"))
    }

    @Test fun staleBalance_skipsRefreshAfterMutation() = runTest {
        val services = servicesWith(DefectId.staleBalance)
        val vm = HomeViewModel(services)
        vm.load()
        val before = vm.accounts.first { it.currency == Currency.EUR }.balance
        services.backend.deposit(Currency.EUR, java.math.BigDecimal("500"))
        vm.refreshAfterMutation()
        assertEquals(before, vm.accounts.first { it.currency == Currency.EUR }.balance)
    }

    @Test fun clean_refreshAfterMutationReflectsChange() = runTest {
        val services = servicesWith()
        val vm = HomeViewModel(services)
        vm.load()
        val before = vm.accounts.first { it.currency == Currency.EUR }.balance
        services.backend.deposit(Currency.EUR, java.math.BigDecimal("500"))
        vm.refreshAfterMutation()
        assertTrue(vm.accounts.first { it.currency == Currency.EUR }.balance > before)
    }
}
