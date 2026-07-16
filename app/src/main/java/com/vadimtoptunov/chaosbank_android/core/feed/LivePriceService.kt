package com.vadimtoptunov.chaosbank_android.core.feed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class LiveTick(val price: Double, val previousClose: Double, val dayHigh: Double, val dayLow: Double)

/**
 * Real market data from the public Yahoo Finance chart endpoint (no API key). The
 * deliberately non-deterministic "chaos" data source; default stays simulated.
 */
class LivePriceService {

    private fun yahoo(appSymbol: String): String = when (appSymbol) {
        "BTC" -> "BTC-USD"
        "ETH" -> "ETH-USD"
        else -> appSymbol
    }

    suspend fun fetch(appSymbols: List<String>): Map<String, LiveTick> = withContext(Dispatchers.IO) {
        coroutineScope {
            appSymbols.map { async { fetchOne(it) } }.awaitAll().filterNotNull().toMap()
        }
    }

    private fun fetchOne(appSymbol: String): Pair<String, LiveTick>? {
        val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/${yahoo(appSymbol)}?interval=1m&range=1d")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("User-Agent", "Mozilla/5.0 (ChaosBank)")
            connectTimeout = 12_000
            readTimeout = 12_000
        }
        return try {
            if (conn.responseCode != 200) return null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val meta = JSONObject(text).getJSONObject("chart")
                .getJSONArray("result").getJSONObject(0).getJSONObject("meta")
            val price = meta.optDouble("regularMarketPrice", Double.NaN)
            if (price.isNaN()) return null
            val prev = meta.optDouble("previousClose", meta.optDouble("chartPreviousClose", price))
            val high = meta.optDouble("regularMarketDayHigh", maxOf(price, prev))
            val low = meta.optDouble("regularMarketDayLow", minOf(price, prev))
            appSymbol to LiveTick(price, prev, high, low)
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
