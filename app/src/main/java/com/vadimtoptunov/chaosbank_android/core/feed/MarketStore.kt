package com.vadimtoptunov.chaosbank_android.core.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.roundedScale
import com.vadimtoptunov.chaosbank_android.models.Asset
import com.vadimtoptunov.chaosbank_android.models.Quote
import com.vadimtoptunov.chaosbank_android.models.TickDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** Observable view of the price feed: seeded simulation (default) or live Yahoo. */
class MarketStore(
    seed: Int,
    val assets: List<Asset>,
    initialSource: PriceSourceKind = PriceSourceKind.simulated,
    private val intervalMs: Long = 700,
) {
    var quotes by mutableStateOf(
        LinkedHashMap<String, Quote>().apply {
            assets.forEach { put(it.symbol, Quote(it.symbol, it.basePrice, it.basePrice, it.basePrice, it.basePrice, TickDirection.flat)) }
        } as Map<String, Quote>
    )
        private set

    var source by mutableStateOf(initialSource)
        private set

    var liveConnected by mutableStateOf(false)
        private set

    private val feed = PriceFeed(seed, assets)
    private val live = LivePriceService()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch { run() }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun switchSource(kind: PriceSourceKind) {
        if (kind == source) return
        stop()
        source = kind
        liveConnected = false
        start()
    }

    private suspend fun run() {
        when (source) {
            PriceSourceKind.simulated -> while (true) { delay(intervalMs); quotes = feed.step() }
            PriceSourceKind.live -> while (true) {
                val ticks = live.fetch(assets.map { it.symbol })
                if (ticks.isNotEmpty()) { apply(ticks); liveConnected = true }
                delay(if (Defects.isActive(DefectId.feedPollsTooOften)) 300 else 3_000)
            }
        }
    }

    private fun apply(ticks: Map<String, LiveTick>) {
        val updated = LinkedHashMap(quotes)
        for ((symbol, t) in ticks) {
            val previous = quotes[symbol]?.price ?: BigDecimal(t.price)
            val newPrice = BigDecimal(t.price).roundedScale(2)
            val dir = when {
                newPrice > previous -> TickDirection.up
                newPrice < previous -> TickDirection.down
                else -> TickDirection.flat
            }
            updated[symbol] = Quote(
                symbol, newPrice,
                BigDecimal(t.previousClose).roundedScale(2),
                BigDecimal(t.dayHigh).roundedScale(2),
                BigDecimal(t.dayLow).roundedScale(2),
                dir,
            )
        }
        quotes = updated
    }

    fun quote(symbol: String): Quote? = quotes[symbol]
    fun price(symbol: String): BigDecimal = quotes[symbol]?.price ?: asset(symbol)?.basePrice ?: BigDecimal.ZERO
    fun asset(symbol: String): Asset? = assets.firstOrNull { it.symbol == symbol }
}
