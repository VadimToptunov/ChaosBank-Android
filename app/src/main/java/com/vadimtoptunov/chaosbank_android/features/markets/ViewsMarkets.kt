package com.vadimtoptunov.chaosbank_android.features.markets

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vadimtoptunov.chaosbank_android.R
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.Money
import com.vadimtoptunov.chaosbank_android.models.Asset
import com.vadimtoptunov.chaosbank_android.models.AssetKind
import com.vadimtoptunov.chaosbank_android.models.SeedData

/**
 * The "views build" rendering of Markets: a segment bar (Button row) over a
 * RecyclerView, from an inflated XML layout hosted via AndroidView. Hosts defects
 * characteristic of the Android View system.
 */
@Composable
fun ViewsMarketsScreen() {
    AndroidView(
        modifier = Modifier.fillMaxSize().testTag(A11y.Markets.root),
        factory = { ctx ->
            val root = LayoutInflater.from(ctx).inflate(R.layout.screen_markets_views, null)
            val list = root.findViewById<RecyclerView>(R.id.markets_list)
            list.layoutManager = LinearLayoutManager(ctx)
            list.contentDescription = A11y.Markets.list
            val adapter = MarketAdapter { asset ->
                AlertDialog.Builder(ctx).setTitle("Order")
                    .setMessage("Buy ${asset.symbol} at ${Money(asset.basePrice, asset.currency).formatted}?")
                    .setPositiveButton("Buy", null).setNegativeButton("Cancel", null).show()
            }
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

private class MarketAdapter(private val onTap: (Asset) -> Unit) : RecyclerView.Adapter<MarketHolder>() {
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
        holder.bind(items[position])
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            // Correct: open the tapped row's asset. `rowTapOpensWrongItem`: use the next
            // row's index (off-by-one), so tapping opens a neighbouring asset.
            val i = if (Defects.isActive(DefectId.rowTapOpensWrongItem)) (pos + 1) % items.size else pos
            onTap(items[i])
        }
    }
}

private class MarketHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val symbol: TextView = view.findViewById(R.id.market_symbol)
    private val name: TextView = view.findViewById(R.id.market_name)
    private val price: TextView = view.findViewById(R.id.market_price)

    fun bind(asset: Asset) {
        symbol.text = asset.symbol
        name.text = asset.name
        price.text = Money(asset.basePrice, asset.currency).formatted
        itemView.contentDescription = A11y.Markets.asset(asset.symbol)
    }
}
