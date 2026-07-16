package com.vadimtoptunov.chaosbank_android.features

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.features.order.OrderViewModel
import com.vadimtoptunov.chaosbank_android.models.OrderRequest
import com.vadimtoptunov.chaosbank_android.models.OrderSide
import com.vadimtoptunov.chaosbank_android.models.OrderStatus
import com.vadimtoptunov.chaosbank_android.models.OrderType
import com.vadimtoptunov.chaosbank_android.support.CoroutineTest
import com.vadimtoptunov.chaosbank_android.support.servicesWith
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class OrderViewModelTest : CoroutineTest() {

    private fun buy(vararg d: DefectId) = OrderViewModel(OrderRequest("AAPL", OrderSide.buy, BigDecimal("189.50")), servicesWith(*d))
    private fun sell(vararg d: DefectId) = OrderViewModel(OrderRequest("AAPL", OrderSide.sell, BigDecimal("189.50")), servicesWith(*d))

    @Test fun defaultQuantityIsOne() = runTest {
        val vm = buy(); vm.load()
        assertEquals(0, BigDecimal.ONE.compareTo(vm.quantity))
    }

    @Test fun orderQtyDefaultsZero_startsInvalid() = runTest {
        val vm = buy(DefectId.orderQtyDefaultsZero); vm.load()
        assertEquals(0, BigDecimal.ZERO.compareTo(vm.quantity))
        assertFalse(vm.isValid)
    }

    @Test fun increment_stepsByOne() = runTest {
        val vm = buy(); vm.load(); vm.increment()
        assertEquals(0, BigDecimal("2").compareTo(vm.quantity))
    }

    @Test fun qtyIncrementByTwo_stepsByTwo() = runTest {
        val vm = buy(DefectId.qtyIncrementByTwo); vm.load(); vm.increment()
        assertEquals(0, BigDecimal("3").compareTo(vm.quantity))
    }

    @Test fun decrement_flooredAtZero() = runTest {
        val vm = buy(); vm.load(); vm.decrement(); vm.decrement()
        assertEquals(0, BigDecimal.ZERO.compareTo(vm.quantity))
    }

    @Test fun estTotal_isQtyTimesPrice() = runTest {
        val vm = buy(); vm.load(); vm.increment() // qty 2
        assertEquals(0, BigDecimal("379.00").compareTo(vm.estTotal.amount))
    }

    @Test fun estTotalIgnoresQty_dropsQuantity() = runTest {
        val vm = buy(DefectId.estTotalIgnoresQty); vm.load(); vm.increment(); vm.increment() // qty 3
        assertEquals(0, BigDecimal("189.50").compareTo(vm.estTotal.amount))
    }

    @Test fun referencePrice_isCapturedPrice() = runTest {
        val vm = buy(); vm.load()
        assertEquals(0, BigDecimal("189.50").compareTo(vm.referencePrice))
    }

    @Test fun sellBeyondHolding_invalidByDefault() = runTest {
        val vm = sell(); vm.load()
        repeat(200) { vm.increment() } // far beyond the 12-share position
        assertFalse(vm.isValid)
    }

    @Test fun sellWithoutHoldingReviewable_allowsOversell() = runTest {
        val vm = sell(DefectId.sellWithoutHoldingReviewable); vm.load()
        repeat(200) { vm.increment() }
        assertTrue(vm.isValid)
    }

    @Test fun place_buyFillsAndFlagsPlaced() = runTest {
        val vm = buy(); vm.load()
        vm.place()
        assertTrue(vm.placed)
        assertEquals(OrderStatus.filled, vm.status)
    }

    @Test fun orderStuckPending_reportsPending() = runTest {
        val vm = buy(DefectId.orderStuckPending); vm.load()
        vm.place()
        assertEquals(OrderStatus.pending, vm.status)
    }

    @Test fun limitExecutesAtMarket_usesReferencePrice() = runTest {
        val vm = buy(DefectId.limitExecutesAtMarket); vm.load()
        vm.type = OrderType.limit; vm.limitPriceText = "250"
        assertEquals(0, BigDecimal("189.50").compareTo(vm.executionPrice))
    }

    @Test fun cleanLimit_usesLimitPrice() = runTest {
        val vm = buy(); vm.load()
        vm.type = OrderType.limit; vm.limitPriceText = "250"
        assertEquals(0, BigDecimal("250").compareTo(vm.executionPrice))
    }
}
