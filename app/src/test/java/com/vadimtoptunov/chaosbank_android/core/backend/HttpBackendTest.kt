package com.vadimtoptunov.chaosbank_android.core.backend

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.models.Order
import com.vadimtoptunov.chaosbank_android.models.OrderSide
import com.vadimtoptunov.chaosbank_android.models.OrderStatus
import com.vadimtoptunov.chaosbank_android.models.OrderType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * [HttpBackend] against a running ChaosBank-Server. Skipped unless
 * `CHAOSBANK_TEST_SERVER` (e.g. http://localhost:8080) is set; CI starts one.
 * The point: on a clean build both backends answer the same.
 */
class HttpBackendTest {
    private val url: String? = System.getenv("CHAOSBANK_TEST_SERVER")?.takeIf { it.isNotBlank() }

    @Before
    fun needsServer() = assumeTrue("set CHAOSBANK_TEST_SERVER to run", url != null)

    private fun http(defects: Set<DefectId> = emptySet()) = HttpBackend(url!!, seed = 0, defects = defects)
    private fun mock() = MockBackend(latencyMs = 0)

    @Test
    fun cleanReadsMatchTheEmbeddedMock() = runTest {
        val http = http(); val mock = mock()
        assertEquals(mock.fetchAccounts(), http.fetchAccounts())
        assertEquals(mock.fetchTransactions(), http.fetchTransactions())
        assertEquals(mock.fetchHoldings(), http.fetchHoldings())
        assertEquals(mock.fetchAccount(Currency.GBP, 0), http.fetchAccount(Currency.GBP, 0))
    }

    @Test
    fun eachLaunchStartsFromTheSeed() = runTest {
        http().transfer(Currency.EUR, BigDecimal("100.00"), "Alex", "", "k")
        assertEquals(BigDecimal("4820.55"), http().fetchAccount(Currency.EUR, 0)!!.balance)
    }

    @Test
    fun aTransferDebitsOnceForOneKey() = runTest {
        val http = http()
        val first = http.transfer(Currency.EUR, BigDecimal("100.00"), "Alex", "", "same-key")
        val again = http.transfer(Currency.EUR, BigDecimal("100.00"), "Alex", "", "same-key")
        assertEquals(first, again)
        assertEquals(BigDecimal("4720.55"), http.fetchAccount(Currency.EUR, 0)!!.balance)
        assertEquals(first.id, http.fetchTransactions().first().id)
    }

    @Test
    fun theServerPricesAnExchangeLikeTheApp() = runTest {
        val http = http()
        val tx = http.exchange(Currency.EUR, Currency.USD, BigDecimal("100.00"), credited = BigDecimal("1.00"))
        assertEquals(BigDecimal("107.46"), tx.amount)
        assertEquals(BigDecimal("12857.46"), http.fetchAccount(Currency.USD, 0)!!.balance)
    }

    @Test
    fun anOrderFillsAtTheAppsExecutionPrice() = runTest {
        val http = http()
        val order = Order(
            "x", "AAPL", OrderSide.buy, OrderType.limit, BigDecimal("2"), BigDecimal("180.00"),
            referencePrice = BigDecimal("189.50"), executionPrice = BigDecimal("189.50"), OrderStatus.pending, 0,
        )
        val filled = http.placeOrder(order)
        assertEquals(OrderStatus.filled, filled.status)
        assertEquals(BigDecimal("189.50"), filled.executionPrice)
        assertEquals(BigDecimal("14"), http.fetchHoldings().first { it.symbol == "AAPL" }.quantity)
    }

    @Test
    fun businessErrorsKeepTheirNames() = runTest {
        val http = http()
        val e = runCatching { http.transfer(Currency.EUR, BigDecimal("99999.00"), "", "", "k") }.exceptionOrNull()
        assertEquals(BackendError.insufficientFunds, (e as BackendException).error)
    }

    @Test
    fun offlineWritesFailWithoutAskingTheServer() = runTest {
        val http = http()
        http.setCondition(NetworkCondition.offline)
        val e = runCatching { http.deposit(Currency.EUR, BigDecimal("1.00"), "Add money") }.exceptionOrNull()
        assertEquals(BackendError.offline, (e as BackendException).error)
        assertEquals("reads still work", BigDecimal("4820.55"), http.fetchAccount(Currency.EUR, 0)!!.balance)
    }

    @Test
    fun activeDefectsTheServerKnowsAreSwitchedOnThere() = runTest {
        val http = http(setOf(DefectId.balanceReadReturnsZero, DefectId.pnlSign))   // pnlSign is client-only
        assertTrue(http.fetchAccounts().all { it.balance.signum() == 0 })
        http.setDefects(emptySet())
        assertEquals(BigDecimal("4820.55"), http.fetchAccount(Currency.EUR, 0)!!.balance)
    }

    @Test
    fun theSyncCounterLivesOnTheServer() = runTest {
        val http = http()
        repeat(3) { http.syncIncrement() }
        assertEquals(3, http.syncValue())
        http.syncReset()
        assertEquals(0, http.syncValue())
    }

    @Test
    fun anUnreachableServerServesTheLastGoodRead() = runTest {
        val dead = HttpBackend("http://127.0.0.1:1", seed = 0, defects = emptySet(), timeoutMs = 500)
        assertEquals(emptyList<Any>(), dead.fetchAccounts())
        val e = runCatching { dead.deposit(Currency.EUR, BigDecimal("1.00"), "Add money") }.exceptionOrNull()
        assertEquals(BackendError.timeout, (e as BackendException).error)
    }
}
