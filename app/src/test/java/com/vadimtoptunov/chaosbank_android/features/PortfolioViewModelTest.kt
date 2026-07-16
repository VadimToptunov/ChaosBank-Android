package com.vadimtoptunov.chaosbank_android.features

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.features.portfolio.PortfolioViewModel
import com.vadimtoptunov.chaosbank_android.support.CoroutineTest
import com.vadimtoptunov.chaosbank_android.support.servicesWith
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PortfolioViewModelTest : CoroutineTest() {

    private fun vm(vararg d: DefectId) = PortfolioViewModel(servicesWith(*d))

    @Test fun loadsThreeHoldings() = runTest {
        val vm = vm(); vm.load()
        assertEquals(3, vm.holdings.size)
    }

    @Test fun totalCost_sumsCostBasis() = runTest {
        val vm = vm(); vm.load()
        // 12*150 + 8*280 + 3.5*2400 = 12440
        assertEquals(0, BigDecimal("12440.0").compareTo(vm.totalCost))
    }

    @Test fun tslaIsALoss() = runTest {
        val vm = vm(); vm.load()
        val tsla = vm.holdings.first { it.symbol == "TSLA" }
        assertTrue(vm.pnl(tsla).signum() < 0)
    }

    @Test fun pnlSign_flipsLossToGain() = runTest {
        val vm = vm(DefectId.pnlSign); vm.load()
        val tsla = vm.holdings.first { it.symbol == "TSLA" }
        assertTrue(vm.displayPnL(vm.pnl(tsla)).signum() > 0)
    }

    @Test fun totalValueOmitsHolding_dropsEth() = runTest {
        val clean = vm().apply { load() }.totalValue.amount
        val buggy = vm(DefectId.totalValueOmitsHolding).apply { load() }.totalValue.amount
        assertTrue(buggy < clean)
    }

    @Test fun holdingValueUsesCost_valuesAtCost() = runTest {
        val vm = vm(DefectId.holdingValueUsesCost); vm.load()
        val aapl = vm.holdings.first { it.symbol == "AAPL" }
        // cost basis 12*150 = 1800
        assertEquals(0, BigDecimal("1800.00").compareTo(vm.marketValue(aapl).amount))
    }

    @Test fun pnlPercentAbsOnly_isNonNegative() = runTest {
        val vm = vm(DefectId.pnlPercentAbsOnly); vm.load()
        assertTrue(vm.totalPnLPercent.signum() >= 0)
    }

    @Test fun allocationFraction_inUnitRange() = runTest {
        val vm = vm(); vm.load()
        vm.holdings.forEach {
            val f = vm.allocationFraction(it)
            assertTrue(f in 0.0..1.0)
        }
    }

    @Test fun name_resolvesFromSeed() = runTest {
        val vm = vm(); vm.load()
        assertEquals("Apple Inc.", vm.name("AAPL"))
        assertEquals("ZZZ", vm.name("ZZZ"))
    }
}
