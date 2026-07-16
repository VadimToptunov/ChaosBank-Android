package com.vadimtoptunov.chaosbank_android.core.backend

import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.models.Order
import com.vadimtoptunov.chaosbank_android.models.OrderSide
import com.vadimtoptunov.chaosbank_android.models.OrderStatus
import com.vadimtoptunov.chaosbank_android.models.OrderType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MockBackendTest {
    private fun backend(scenario: BackendScenario = BackendScenario()) = MockBackend(latencyMs = 0, scenario = scenario)

    private fun order(side: OrderSide, symbol: String, qty: String, price: String) = Order(
        "o", symbol, side, OrderType.market, BigDecimal(qty), null, BigDecimal(price), BigDecimal(price), OrderStatus.pending, 0,
    )

    @Test fun fetchAccounts_returnsThree() = runTest {
        assertEquals(3, backend().fetchAccounts().size)
    }

    @Test fun transfer_debitsBalanceOnce() = runTest {
        val b = backend()
        val before = b.fetchAccount(Currency.EUR)!!.balance
        val tx = b.transfer(Currency.EUR, BigDecimal("100"), "Alex", "", "key-1")
        assertEquals(0, BigDecimal("-100").compareTo(tx.amount))
        assertEquals(0, before.minus(BigDecimal("100")).compareTo(b.fetchAccount(Currency.EUR)!!.balance))
    }

    @Test fun transfer_isIdempotentPerKey() = runTest {
        val b = backend()
        val before = b.fetchAccount(Currency.EUR)!!.balance
        val t1 = b.transfer(Currency.EUR, BigDecimal("50"), "Alex", "", "same")
        val t2 = b.transfer(Currency.EUR, BigDecimal("50"), "Alex", "", "same")
        assertEquals(t1.id, t2.id)
        assertEquals(0, before.minus(BigDecimal("50")).compareTo(b.fetchAccount(Currency.EUR)!!.balance))
    }

    @Test(expected = BackendException::class)
    fun transfer_insufficientFundsThrows() = runTest {
        backend().transfer(Currency.EUR, BigDecimal("9999999"), "Alex", "", "k")
    }

    @Test fun deposit_creditsBalance() = runTest {
        val b = backend()
        val before = b.fetchAccount(Currency.USD)!!.balance
        b.deposit(Currency.USD, BigDecimal("200"))
        assertEquals(0, before.plus(BigDecimal("200")).compareTo(b.fetchAccount(Currency.USD)!!.balance))
    }

    @Test fun exchange_debitsAndCredits() = runTest {
        val b = backend()
        val eur = b.fetchAccount(Currency.EUR)!!.balance
        val usd = b.fetchAccount(Currency.USD)!!.balance
        b.exchange(Currency.EUR, Currency.USD, BigDecimal("100"), BigDecimal("107"))
        assertEquals(0, eur.minus(BigDecimal("100")).compareTo(b.fetchAccount(Currency.EUR)!!.balance))
        assertEquals(0, usd.plus(BigDecimal("107")).compareTo(b.fetchAccount(Currency.USD)!!.balance))
    }

    @Test fun placeOrder_buyFills() = runTest {
        val filled = backend().placeOrder(order(OrderSide.buy, "AAPL", "1", "189.50"))
        assertEquals(OrderStatus.filled, filled.status)
    }

    @Test(expected = BackendException::class)
    fun placeOrder_sellBeyondHoldingThrows() = runTest {
        backend().placeOrder(order(OrderSide.sell, "AAPL", "100", "189.50"))
    }

    @Test fun placeOrder_sellWithinHoldingFills() = runTest {
        val filled = backend().placeOrder(order(OrderSide.sell, "AAPL", "5", "189.50"))
        assertEquals(OrderStatus.filled, filled.status)
    }

    @Test fun scenario_balanceReadReturnsZero() = runTest {
        val b = backend(BackendScenario(balanceReadReturnsZero = true))
        assertEquals(0, BigDecimal.ZERO.compareTo(b.fetchAccount(Currency.EUR)!!.balance))
    }

    @Test fun scenario_transactionsDupOnFetch() = runTest {
        val plain = backend().fetchTransactions().size
        val dup = backend(BackendScenario(transactionsDupOnFetch = true)).fetchTransactions().size
        assertEquals(plain * 2, dup)
    }

    @Test fun scenario_retryDuplicate_postsTwice() = runTest {
        val b = backend(BackendScenario(retryDuplicate = true))
        val before = b.fetchAccount(Currency.EUR)!!.balance
        b.transfer(Currency.EUR, BigDecimal("30"), "Alex", "", "same")
        b.transfer(Currency.EUR, BigDecimal("30"), "Alex", "", "same")
        assertEquals(0, before.minus(BigDecimal("60")).compareTo(b.fetchAccount(Currency.EUR)!!.balance))
    }

    @Test(expected = BackendException::class)
    fun scenario_timeoutAsSuccess_stillThrowsTimeout() = runTest {
        backend(BackendScenario(timeoutAsSuccess = true)).transfer(Currency.EUR, BigDecimal("10"), "A", "", "k")
    }

    @Test fun fetchHoldings_andOrders() = runTest {
        val b = backend()
        assertTrue(b.fetchHoldings().isNotEmpty())
        assertTrue(b.fetchOrders().isEmpty())
    }
}
