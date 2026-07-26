package com.vadimtoptunov.chaosbank_android.features.markets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vadimtoptunov.chaosbank_android.R
import com.vadimtoptunov.chaosbank_android.app.LocalNavigator
import com.vadimtoptunov.chaosbank_android.app.Route
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.MoneyFormat
import com.vadimtoptunov.chaosbank_android.models.Asset
import com.vadimtoptunov.chaosbank_android.models.AssetKind
import com.vadimtoptunov.chaosbank_android.models.Quote
import com.vadimtoptunov.chaosbank_android.models.SeedData
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.SparklineView
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import java.math.BigDecimal

/**
 * The "views build" rendering of Markets — a faithful twin of the Compose
 * MarketsScreen: a segment bar over a RecyclerView whose rows carry a price
 * sparkline and price/%change, tapping through to the asset detail. Same data,
 * locators and defects; only the view layer is the Android View system.
 */
@Composable
fun ViewsMarketsScreen() {
    val nav = LocalNavigator.current
    val services = LocalAppServices.current
    AndroidView(
        modifier = Modifier.fillMaxSize().testTag(A11y.Markets.root),
        factory = { ctx ->
            services.startFeed()
            val root = LayoutInflater.from(ctx).inflate(R.layout.screen_markets_views, null)
            val list = root.findViewById<RecyclerView>(R.id.markets_list)
            list.layoutManager = LinearLayoutManager(ctx)
            list.contentDescription = A11y.Markets.list
            val adapter = MarketAdapter(
                quoteOf = { services.market.quote(it) },
                onTap = { symbol -> nav.push(Route.AssetDetail(symbol)) },
            )
            list.adapter = adapter

            fun assetsFor(segment: String): List<Asset> = when (segment) {
                "stocks" -> SeedData.assets.filter { it.kind == AssetKind.stock }
                "crypto" -> SeedData.assets.filter { it.kind == AssetKind.crypto }
                else -> SeedData.assets.filter { SeedData.watchlistSymbols.contains(it.symbol) }
            }
            adapter.submit(assetsFor("watchlist"))

            val segments = listOf(
                root.findViewById<Button>(R.id.seg_watchlist) to Pair("watchlist", A11y.Markets.segmentWatchlist),
                root.findViewById<Button>(R.id.seg_stocks) to Pair("stocks", A11y.Markets.segmentStocks),
                root.findViewById<Button>(R.id.seg_crypto) to Pair("crypto", A11y.Markets.segmentCrypto),
            )
            for ((button, meta) in segments) {
                button.contentDescription = meta.second
                // Correct: wire every segment's click. `controlActionNotWired`: leave
                // the listener off, so tapping does nothing and the list stays put.
                if (!Defects.isActive(DefectId.controlActionNotWired)) {
                    button.setOnClickListener {
                        val next = assetsFor(meta.first)
                        // Correct: replace. `listNotClearedOnReload`: append, so switching
                        // segments accumulates rows from every segment visited.
                        if (Defects.isActive(DefectId.listNotClearedOnReload)) adapter.append(next) else adapter.submit(next)
                    }
                }
            }
            root
        },
    )
}

private class MarketAdapter(
    private val quoteOf: (String) -> Quote?,
    private val onTap: (String) -> Unit,
) : RecyclerView.Adapter<MarketHolder>() {
    private var items: List<Asset> = emptyList()

    fun submit(list: List<Asset>) {
        items = list
        notifyDataSetChanged()
    }

    fun append(list: List<Asset>) {
        items = items + list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_market, parent, false)
        return MarketHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: MarketHolder, position: Int) {
        holder.bind(items[position], quoteOf(items[position].symbol))
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            // Correct: open the tapped row's asset. `rowTapOpensWrongItem` /
            // `assetRowOpensWrongDetail`: use the next row's index (off-by-one), so
            // tapping opens a neighbouring asset's detail.
            val wrong = Defects.isActive(DefectId.rowTapOpensWrongItem) ||
                Defects.isActive(DefectId.assetRowOpensWrongDetail)
            val i = if (wrong) (pos + 1) % items.size else pos
            onTap(items[i].symbol)
        }
    }
}

private class MarketHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val symbol: TextView = view.findViewById(R.id.market_symbol)
    private val name: TextView = view.findViewById(R.id.market_name)
    private val price: TextView = view.findViewById(R.id.market_price)
    private val change: TextView = view.findViewById(R.id.market_change)
    private val sparkline: SparklineView = view.findViewById(R.id.market_sparkline)

    fun bind(asset: Asset, quote: Quote?) {
        symbol.text = asset.symbol
        name.text = asset.name

        val p = quote?.price ?: asset.basePrice
        val changePct = quote?.changePct ?: BigDecimal.ZERO
        // `changePctSignFlipped`: the displayed % change is negated.
        val shownChange = if (Defects.isActive(DefectId.changePctSignFlipped)) changePct.negate() else changePct
        // `priceMissingDecimals`: render whole-dollar prices.
        val digits = if (Defects.isActive(DefectId.priceMissingDecimals)) 0 else 2
        price.text = "$" + MoneyFormat.price(p, digits)
        change.text = MoneyFormat.percent(shownChange)
        change.setTextColor(Palette.pnl(shownChange).toArgb())

        sparkline.symbol = asset.symbol
        sparkline.up = changePct.signum() >= 0
        // `sparklineHeavyPoints`: compute an absurd number of points.
        sparkline.pointCount = if (Defects.isActive(DefectId.sparklineHeavyPoints)) 4000 else 24

        // `duplicateAssetA11yId`: NVDA collides onto AAPL's identifier.
        val rowId = if (Defects.isActive(DefectId.duplicateAssetA11yId) && asset.symbol == "NVDA")
            A11y.Markets.asset("AAPL") else A11y.Markets.asset(asset.symbol)
        // `marketRowNoLabel`: strip the row's accessibility label.
        itemView.contentDescription = if (Defects.isActive(DefectId.marketRowNoLabel)) " " else rowId
    }
}
