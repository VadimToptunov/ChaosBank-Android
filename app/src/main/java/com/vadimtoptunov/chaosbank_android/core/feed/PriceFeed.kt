package com.vadimtoptunov.chaosbank_android.core.feed

import com.vadimtoptunov.chaosbank_android.core.SeededRng
import com.vadimtoptunov.chaosbank_android.core.money.roundedScale
import com.vadimtoptunov.chaosbank_android.models.Asset
import com.vadimtoptunov.chaosbank_android.models.Quote
import com.vadimtoptunov.chaosbank_android.models.TickDirection
import java.math.BigDecimal

/** Seeded random-walk price feed. Two runs with the same build seed reproduce. */
class PriceFeed(seed: Int, private val assets: List<Asset>) {
    private val rng = SeededRng(seed.toLong() + 0x5A5A_5A5AL)
    private val quotes = LinkedHashMap<String, Quote>().apply {
        assets.forEach {
            put(it.symbol, Quote(it.symbol, it.basePrice, it.basePrice, it.basePrice, it.basePrice, TickDirection.flat))
        }
    }

    fun snapshot(): Map<String, Quote> = LinkedHashMap(quotes)

    fun step(): Map<String, Quote> {
        val floor = BigDecimal("0.01")
        for (a in assets) {
            val q = quotes[a.symbol] ?: continue
            val r = BigDecimal(rng.nextInRange(-1.0, 1.0))
            var newPrice = (q.price + q.price * a.volatility * r).roundedScale(2)
            if (newPrice < floor) newPrice = floor
            val dir = when {
                newPrice > q.price -> TickDirection.up
                newPrice < q.price -> TickDirection.down
                else -> TickDirection.flat
            }
            quotes[a.symbol] = q.copy(
                price = newPrice,
                lastDirection = dir,
                dayHigh = if (newPrice > q.dayHigh) newPrice else q.dayHigh,
                dayLow = if (newPrice < q.dayLow) newPrice else q.dayLow,
            )
        }
        return LinkedHashMap(quotes)
    }
}
