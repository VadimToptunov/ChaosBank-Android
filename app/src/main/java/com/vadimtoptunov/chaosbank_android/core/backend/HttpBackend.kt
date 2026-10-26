package com.vadimtoptunov.chaosbank_android.core.backend

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.models.Account
import com.vadimtoptunov.chaosbank_android.models.Holding
import com.vadimtoptunov.chaosbank_android.models.Order
import com.vadimtoptunov.chaosbank_android.models.OrderSide
import com.vadimtoptunov.chaosbank_android.models.OrderStatus
import com.vadimtoptunov.chaosbank_android.models.OrderType
import com.vadimtoptunov.chaosbank_android.models.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * ChaosBank-Server over HTTP (`CHAOSBANK_BACKEND=<url>`). Each launch gets its own chaos
 * session on the server, so it starts from the seed data like the embedded mock. It signs in
 * on its own, with the demo OTP, and signs in again when a token expires.
 *
 * Defects: client-side ones stay in the app as always. The active defects the server also
 * knows (its `/chaos/catalog`) are switched on in the session. The network condition goes to
 * the session too, except `offline`, which stays client-side: writes fail before any request,
 * as in [MockBackend].
 *
 * The server prices exchanges itself, so a client defect that only changes the credited
 * amount sent to [exchange] has no effect here. An order goes up with the app's own
 * execution price (as a limit when it is a limit), so order defects carry over.
 */
class HttpBackend(
    baseUrl: String,
    private val seed: Int,
    defects: Set<DefectId>,
    private val username: String = "chaosbank-app",
    private val timeoutMs: Int = 15_000,
) : Backend {
    private val base = baseUrl.trimEnd('/')
    override val label: String = "Server $base"

    private val setup = Mutex()
    private var sessionId: String? = null
    private var token: String? = null
    private var serverDefects: Set<String>? = null

    @Volatile private var defects: Set<DefectId> = defects
    @Volatile private var condition = NetworkCondition.normal
    /** The session's defects / network need pushing before the next call. */
    @Volatile private var configDirty = false

    // Last good reads, served when a read fails.
    @Volatile private var accounts: List<Account> = emptyList()
    @Volatile private var transactions: List<Transaction> = emptyList()
    @Volatile private var holdings: List<Holding> = emptyList()
    @Volatile private var orders: List<Order> = emptyList()
    @Volatile private var counter = 0

    override fun setScenario(scenario: BackendScenario) { /* server-side scenarios follow the defects (setDefects) */ }

    override fun setCondition(value: NetworkCondition) { condition = value; configDirty = true }

    override fun setDefects(defects: Set<DefectId>) { this.defects = defects; configDirty = true }

    // Reads ------------------------------------------------------------------

    override suspend fun fetchAccounts(): List<Account> =
        read({ accounts }) { JSONArray(call("GET", "/v1/accounts")).objects().map(::account).also { accounts = it } }

    override suspend fun fetchAccount(currency: Currency, extraDelayMs: Long): Account? {
        if (extraDelayMs > 0) delay(extraDelayMs)
        return read({ accounts.firstOrNull { it.currency == currency } }) {
            try {
                account(JSONObject(call("GET", "/v1/accounts/${currency.code}")))
            } catch (e: BackendException) {
                if (e.error == BackendError.unknownAccount) null else throw e
            }
        }
    }

    override suspend fun fetchTransactions(): List<Transaction> =
        read({ transactions }) {
            JSONObject(call("GET", "/v1/transactions")).getJSONArray("items").objects().map(::transaction).also { transactions = it }
        }

    override suspend fun fetchHoldings(): List<Holding> =
        read({ holdings }) { JSONArray(call("GET", "/v1/holdings")).objects().map(::holding).also { holdings = it } }

    override suspend fun fetchOrders(): List<Order> =
        read({ orders }) { JSONArray(call("GET", "/v1/orders")).objects().map(::order).also { orders = it } }

    override suspend fun syncValue(): Int = read({ counter }) { value(call("GET", "/v1/sync/counter")) }

    override suspend fun syncSet(value: Int) { value(call("PUT", "/v1/sync/counter", JSONObject().put("value", value))) }

    override suspend fun syncIncrement() { value(call("POST", "/v1/sync/counter/increment")) }

    override suspend fun syncReset() { value(call("POST", "/v1/sync/counter/reset")) }

    // Writes -----------------------------------------------------------------

    override suspend fun transfer(
        from: Currency, amount: BigDecimal, recipient: String, note: String, idempotencyKey: String,
    ): Transaction = write("/v1/transfers", idempotencyKey) {
        put("from", from.code); put("amount", amount.toPlainString()); put("recipient", recipient); put("note", note)
    }.let(::transaction)

    override suspend fun deposit(to: Currency, amount: BigDecimal, title: String): Transaction =
        write("/v1/deposits") { put("to", to.code); put("amount", amount.toPlainString()); put("title", title) }.let(::transaction)

    override suspend fun exchange(sell: Currency, get: Currency, debit: BigDecimal, credited: BigDecimal): Transaction =
        write("/v1/exchanges") { put("sell", sell.code); put("get", get.code); put("amount", debit.toPlainString()) }
            .getJSONObject("transaction").let(::transaction)

    override suspend fun placeOrder(order: Order): Order = write("/v1/orders") {
        put("symbol", order.symbol); put("side", order.side.name); put("type", order.type.name)
        put("quantity", order.quantity.toPlainString())
        // The app's own execution price, so app-side pricing (and its defects) carry over.
        if (order.type == OrderType.limit) put("limitPrice", order.executionPrice.toPlainString())
        put("referencePrice", (if (order.type == OrderType.market) order.executionPrice else order.referencePrice).toPlainString())
    }.let(::order)

    private suspend fun write(path: String, key: String = UUID.randomUUID().toString(), body: JSONObject.() -> Unit): JSONObject {
        if (condition == NetworkCondition.offline) throw BackendException(BackendError.offline)
        return JSONObject(call("POST", path, JSONObject().apply(body), key))
    }

    private suspend fun <T> read(cached: () -> T, fetch: suspend () -> T): T =
        try {
            fetch()
        } catch (e: BackendException) {
            if (e.error == BackendError.timeout || e.error == BackendError.offline) cached() else throw e
        }

    private fun value(text: String): Int = JSONObject(text).getInt("value").also { counter = it }

    // HTTP -------------------------------------------------------------------

    /** One API call in the app's session, signed in; signs in again once on a 401. */
    private suspend fun call(method: String, path: String, body: JSONObject? = null, idempotencyKey: String? = null): String {
        ready()
        var r = send(method, path, body, idempotencyKey)
        if (r.status == 401) {
            setup.withLock { token = null }
            ready()
            r = send(method, path, body, idempotencyKey)
        }
        if (r.status >= 400) throw problem(r)
        return r.body
    }

    private suspend fun send(method: String, path: String, body: JSONObject?, idempotencyKey: String?): Reply {
        val headers = buildMap {
            sessionId?.let { put("X-Chaos-Session", it) }
            token?.let { put("Authorization", "Bearer $it") }
            idempotencyKey?.let { put("Idempotency-Key", it) }
        }
        return http(method, path, body, headers)
    }

    /** Creates the session and signs in on first use; pushes pending defect / network changes. */
    private suspend fun ready() = setup.withLock {
        if (serverDefects == null) {
            serverDefects = JSONObject(ok(http("GET", "/chaos/catalog"))).getJSONArray("defects").objects().map { it.getString("id") }.toSet()
        }
        if (sessionId == null) {
            sessionId = JSONObject(ok(http("POST", "/chaos/sessions", sessionBody().put("seed", seed)))).getString("id")
            configDirty = false
        } else if (configDirty) {
            configDirty = false
            ok(http("PUT", "/chaos/sessions/$sessionId", sessionBody()))
        }
        if (token == null) {
            val session = mapOf("X-Chaos-Session" to sessionId!!)
            val login = JSONObject().put("username", username).put("password", "chaosbank")
            val challenge = JSONObject(ok(http("POST", "/v1/auth/login", login, session))).getString("challengeId")
            val otp = JSONObject().put("challengeId", challenge).put("code", DEMO_OTP)
            token = JSONObject(ok(http("POST", "/v1/auth/otp", otp, session))).getString("accessToken")
        }
    }

    private fun sessionBody(): JSONObject {
        val known = serverDefects.orEmpty()
        val network = if (condition == NetworkCondition.offline) NetworkCondition.normal else condition
        return JSONObject()
            .put("defects", JSONArray(defects.map { it.name }.filter { it in known }.sorted()))
            .put("network", network.name)
    }

    private class Reply(val status: Int, val body: String)

    private fun ok(r: Reply): String = if (r.status >= 400) throw problem(r) else r.body

    private suspend fun http(method: String, path: String, body: JSONObject? = null, headers: Map<String, String> = emptyMap()): Reply =
        withContext(Dispatchers.IO) {
            try {
                val conn = URL(base + path).openConnection() as HttpURLConnection
                conn.requestMethod = method
                conn.connectTimeout = timeoutMs
                conn.readTimeout = timeoutMs
                conn.setRequestProperty("Accept", "application/json")
                headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toString().toByteArray()) }
                }
                val status = conn.responseCode
                val stream = if (status >= 400) conn.errorStream else conn.inputStream
                Reply(status, stream?.bufferedReader()?.use { it.readText() }.orEmpty()).also { conn.disconnect() }
            } catch (e: IOException) {
                throw BackendException(BackendError.timeout)
            }
        }

    companion object {
        /** The server's demo OTP (ChaosBank-Server `AuthRules.OTP_CODE`, the apps' `mockOtp`). */
        const val DEMO_OTP = "424242"

        /** A problem+json reply → the app's [BackendError], by `type` first, then by status. */
        private fun problem(r: Reply): BackendException {
            val type = runCatching { JSONObject(r.body).getString("type") }.getOrNull()
            val error = BackendError.entries.firstOrNull { it.name == type } ?: when (r.status) {
                404 -> BackendError.unknownAccount
                in 400..499 -> BackendError.invalidAmount
                else -> BackendError.timeout
            }
            return BackendException(error)
        }

        private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

        private fun money(o: JSONObject, key: String) = BigDecimal(o.getString(key))

        private fun account(o: JSONObject) = Account(o.getString("name"), Currency.valueOf(o.getString("currency")), money(o, "balance"))

        private fun transaction(o: JSONObject) = Transaction(
            o.getString("id"), o.getString("title"), o.getString("category"), o.getLong("date"),
            money(o, "amount"), Currency.valueOf(o.getString("currency")),
        )

        private fun holding(o: JSONObject) = Holding(o.getString("symbol"), money(o, "quantity"), money(o, "avgCost"))

        private fun order(o: JSONObject) = Order(
            id = o.getString("id"), symbol = o.getString("symbol"),
            side = OrderSide.valueOf(o.getString("side")), type = OrderType.valueOf(o.getString("type")),
            quantity = money(o, "quantity"),
            limitPrice = if (o.isNull("limitPrice")) null else money(o, "limitPrice"),
            referencePrice = money(o, "referencePrice"), executionPrice = money(o, "executionPrice"),
            status = OrderStatus.valueOf(o.getString("status")), placedAt = o.getLong("placedAt"),
        )
    }
}
