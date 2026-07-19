package com.vadimtoptunov.chaosbank_android.core.backend

import com.vadimtoptunov.chaosbank_android.core.SeededRng
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.models.Account
import com.vadimtoptunov.chaosbank_android.models.Holding
import com.vadimtoptunov.chaosbank_android.models.Order
import com.vadimtoptunov.chaosbank_android.models.OrderSide
import com.vadimtoptunov.chaosbank_android.models.OrderStatus
import com.vadimtoptunov.chaosbank_android.models.SeedData
import com.vadimtoptunov.chaosbank_android.models.Transaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal

/**
 * In-memory authoritative state with configurable latency. All money math here is
 * correct (BigDecimal); defects live in callers' guarded injection points. Serialized
 * with a [Mutex] (the coroutine analogue of the iOS actor).
 */
class MockBackend(
    private val latencyMs: Long = 120,
    scenario: BackendScenario = BackendScenario(),
) {
    private val mutex = Mutex()

    private val accountsByCurrency: LinkedHashMap<Currency, Account> =
        LinkedHashMap(SeedData.accounts.associateBy { it.currency })
    private val offlineAccounts: Map<Currency, Account> = LinkedHashMap(accountsByCurrency)
    private var transactions: MutableList<Transaction> = SeedData.transactions.toMutableList()
    private val holdings: LinkedHashMap<String, Holding> =
        LinkedHashMap(SeedData.holdings.associateBy { it.symbol })
    private val offlineHoldings: Map<String, Holding> = LinkedHashMap(holdings)
    private val orders: MutableList<Order> = mutableListOf()
    private val assets = SeedData.assets.associateBy { it.symbol }
    private val processedKeys: MutableMap<String, Transaction> = mutableMapOf()
    private val cashCurrency = Currency.USD

    private var scenario: BackendScenario = scenario
    private var sequence = 0

    /** Simulated network environment (dev-menu / reliability cluster). */
    private var condition = NetworkCondition.normal
    private var flakySeq = 0L

    fun setScenario(scenario: BackendScenario) { this.scenario = scenario }

    fun setCondition(value: NetworkCondition) { condition = value }

    /** Convenience for the offline-only path (kept for existing callers/tests). */
    fun setOffline(value: Boolean) { condition = if (value) NetworkCondition.offline else NetworkCondition.normal }

    /** Offline blocks writes. */
    private fun requireOnline() { if (condition == NetworkCondition.offline) throw BackendException(BackendError.offline) }

    /** `flaky` fails writes transiently; the sequence is seeded so it reproduces. */
    private fun failIfFlaky() {
        if (condition != NetworkCondition.flaky) return
        if (SeededRng(flakySeq++).nextDouble() < 0.5) throw BackendException(BackendError.timeout)
    }

    private suspend fun delayNet(extraMs: Long = 0) {
        delay(latencyMs)
        if (condition == NetworkCondition.slow) delay(3_000)
        if (extraMs > 0) delay(extraMs)
    }

    private fun nextId(prefix: String): String { sequence += 1; return "$prefix-$sequence" }

    private fun zeroed(a: Account): Account =
        if (scenario.balanceReadReturnsZero) a.copy(balance = BigDecimal.ZERO) else a

    // Reads ------------------------------------------------------------------

    suspend fun fetchAccounts(): List<Account> = mutex.withLock {
        delayNet()
        val src = if (scenario.staleOfflineBalance) offlineAccounts else accountsByCurrency
        Currency.entries.mapNotNull { src[it] }.map { zeroed(it) }
    }

    suspend fun fetchAccount(currency: Currency, extraDelayMs: Long = 0): Account? = mutex.withLock {
        delayNet(extraDelayMs)
        val src = if (scenario.staleOfflineBalance) offlineAccounts else accountsByCurrency
        src[currency]?.let { zeroed(it) }
    }

    suspend fun fetchTransactions(): List<Transaction> = mutex.withLock {
        delayNet()
        if (scenario.transactionsDupOnFetch) transactions.flatMap { listOf(it, it) } else transactions.toList()
    }

    suspend fun fetchHoldings(): List<Holding> = mutex.withLock {
        delayNet()
        val src = if (scenario.staleHoldingsAfterOrder) offlineHoldings else holdings
        SeedData.assets.mapNotNull { src[it.symbol] }
    }

    suspend fun fetchOrders(): List<Order> = mutex.withLock { delayNet(); orders.toList() }

    // Mutations --------------------------------------------------------------

    suspend fun transfer(
        from: Currency, amount: BigDecimal, recipient: String, note: String, idempotencyKey: String,
    ): Transaction = mutex.withLock {
        delayNet()
        requireOnline()
        failIfFlaky()
        processedKeys[idempotencyKey]?.let { if (!scenario.retryDuplicate) return@withLock it }

        if (amount.signum() <= 0) throw BackendException(BackendError.invalidAmount)
        val account = accountsByCurrency[from] ?: throw BackendException(BackendError.unknownAccount)
        if (account.balance < amount) throw BackendException(BackendError.insufficientFunds)

        accountsByCurrency[from] = account.copy(balance = account.balance - amount)
        val title = if (recipient.isEmpty()) "Transfer" else "Transfer to $recipient"
        val tx = Transaction(nextId("tx"), title, "Transfer", now(), amount.negate(), from)
        transactions.add(0, tx)
        processedKeys[idempotencyKey] = tx
        if (scenario.timeoutAsSuccess) throw BackendException(BackendError.timeout)
        tx
    }

    suspend fun deposit(to: Currency, amount: BigDecimal, title: String = "Add money"): Transaction = mutex.withLock {
        delayNet()
        requireOnline()
        failIfFlaky()
        if (amount.signum() <= 0) throw BackendException(BackendError.invalidAmount)
        val account = accountsByCurrency[to] ?: throw BackendException(BackendError.unknownAccount)
        accountsByCurrency[to] = account.copy(balance = account.balance + amount)
        val tx = Transaction(nextId("dep"), title, "Top-up", now(), amount, to)
        transactions.add(0, tx)
        tx
    }

    suspend fun exchange(sell: Currency, get: Currency, debit: BigDecimal, credited: BigDecimal): Transaction =
        mutex.withLock {
            delayNet()
            requireOnline()
            failIfFlaky()
            if (debit.signum() <= 0) throw BackendException(BackendError.invalidAmount)
            val from = accountsByCurrency[sell] ?: throw BackendException(BackendError.unknownAccount)
            val to = accountsByCurrency[get] ?: throw BackendException(BackendError.unknownAccount)
            if (from.balance < debit) throw BackendException(BackendError.insufficientFunds)
            accountsByCurrency[sell] = from.copy(balance = from.balance - debit)
            accountsByCurrency[get] = to.copy(balance = to.balance + credited)
            val tx = Transaction(nextId("fx"), "Exchange ${sell.code} → ${get.code}", "Exchange", now(), credited, get)
            transactions.add(0, tx)
            tx
        }

    suspend fun placeOrder(order: Order): Order = mutex.withLock {
        delayNet()
        requireOnline()
        failIfFlaky()
        assets[order.symbol] ?: throw BackendException(BackendError.unknownAsset)
        if (order.quantity.signum() <= 0) throw BackendException(BackendError.invalidAmount)
        val cash = accountsByCurrency[cashCurrency] ?: throw BackendException(BackendError.unknownAccount)
        val total = order.quantity * order.executionPrice

        when (order.side) {
            OrderSide.buy -> {
                if (cash.balance < total) throw BackendException(BackendError.insufficientFunds)
                accountsByCurrency[cashCurrency] = cash.copy(balance = cash.balance - total)
                val existing = holdings[order.symbol]
                if (existing != null) {
                    val newQty = existing.quantity + order.quantity
                    val newCost = existing.costBasis + total
                    holdings[order.symbol] = existing.copy(
                        quantity = newQty,
                        avgCost = if (newQty.signum() == 0) BigDecimal.ZERO
                        else newCost.divide(newQty, 10, java.math.RoundingMode.HALF_EVEN),
                    )
                } else {
                    holdings[order.symbol] = Holding(order.symbol, order.quantity, order.executionPrice)
                }
            }
            OrderSide.sell -> {
                val existing = holdings[order.symbol]
                if (existing == null || existing.quantity < order.quantity)
                    throw BackendException(BackendError.insufficientHolding)
                accountsByCurrency[cashCurrency] = cash.copy(balance = cash.balance + total)
                val newQty = existing.quantity - order.quantity
                if (newQty.signum() == 0) holdings.remove(order.symbol)
                else holdings[order.symbol] = existing.copy(quantity = newQty)
            }
        }

        val filled = order.copy(status = OrderStatus.filled)
        orders.add(0, filled)
        val verb = if (order.side == OrderSide.buy) "Buy" else "Sell"
        val signed = if (order.side == OrderSide.buy) total.negate() else total
        transactions.add(0, Transaction(nextId("ord"), "$verb ${order.symbol}", "Trade", now(), signed, cashCurrency))
        filled
    }

    private fun now(): Long = System.currentTimeMillis() / 1000
}
