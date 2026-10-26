package com.vadimtoptunov.chaosbank_android.core.backend

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.models.Account
import com.vadimtoptunov.chaosbank_android.models.Holding
import com.vadimtoptunov.chaosbank_android.models.Order
import com.vadimtoptunov.chaosbank_android.models.Transaction
import java.math.BigDecimal

/**
 * What screens and view models talk to. Two implementations: [MockBackend], the embedded,
 * offline default, and [HttpBackend], ChaosBank-Server reached over HTTP when the app is
 * launched with `CHAOSBANK_BACKEND=<url>`. Reads never throw (a failed read serves the
 * last value it knows); writes throw [BackendException].
 */
interface Backend {
    suspend fun fetchAccounts(): List<Account>
    suspend fun fetchAccount(currency: Currency, extraDelayMs: Long = 0): Account?
    suspend fun fetchTransactions(): List<Transaction>
    suspend fun fetchHoldings(): List<Holding>
    suspend fun fetchOrders(): List<Order>

    suspend fun syncValue(): Int
    suspend fun syncSet(value: Int)
    suspend fun syncIncrement()
    suspend fun syncReset()

    suspend fun transfer(from: Currency, amount: BigDecimal, recipient: String, note: String, idempotencyKey: String): Transaction
    suspend fun deposit(to: Currency, amount: BigDecimal, title: String = "Add money"): Transaction
    suspend fun exchange(sell: Currency, get: Currency, debit: BigDecimal, credited: BigDecimal): Transaction
    suspend fun placeOrder(order: Order): Order

    fun setScenario(scenario: BackendScenario)
    fun setCondition(value: NetworkCondition)

    /** The active defects changed (dev menu). Only a server backend cares: it forwards the ones it knows. */
    fun setDefects(defects: Set<DefectId>) {}

    /** Where the data comes from, for the dev menu. */
    val label: String
}
