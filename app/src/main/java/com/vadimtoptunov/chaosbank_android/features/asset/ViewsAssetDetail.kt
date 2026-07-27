package com.vadimtoptunov.chaosbank_android.features.asset

import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.vadimtoptunov.chaosbank_android.R
import com.vadimtoptunov.chaosbank_android.app.LocalNavigator
import com.vadimtoptunov.chaosbank_android.app.Route
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.StableHash
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.MoneyFormat
import com.vadimtoptunov.chaosbank_android.core.money.roundedScale
import com.vadimtoptunov.chaosbank_android.models.Asset
import com.vadimtoptunov.chaosbank_android.models.OrderRequest
import com.vadimtoptunov.chaosbank_android.models.OrderSide
import com.vadimtoptunov.chaosbank_android.models.SeedData
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.SparklineView
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The "views build" rendering of the asset detail — a faithful twin of the
 * Compose [AssetDetailScreen], inflated from XML and hosting the same
 * SparklineView, stats, and Sell/Buy (which push the order ticket). Same data,
 * locators and defects.
 */
@Composable
fun ViewsAssetDetailScreen(symbol: String) {
    val services = LocalAppServices.current
    val nav = LocalNavigator.current
    val asset = SeedData.assets.firstOrNull { it.symbol == symbol } ?: return
    AndroidView(
        modifier = Modifier.fillMaxSize().testTag(A11y.Asset.root),
        factory = { ctx ->
            services.startFeed()
            val root = LayoutInflater.from(ctx).inflate(R.layout.screen_asset_detail_views, null)
            val active = { id: DefectId -> Defects.isActive(id) }
            val quote = services.market.quote(symbol)

            val base = quote?.price ?: asset.basePrice
            // `detailPriceOffset`: the detail price drifts from the market price.
            val price = if (active(DefectId.detailPriceOffset)) base + BigDecimal("5") else base
            // `detailChangeWrongBase`: measure change vs the anchor base, not day open.
            val changePct = if (active(DefectId.detailChangeWrongBase) && asset.basePrice.signum() != 0)
                (price - asset.basePrice).divide(asset.basePrice, 6, RoundingMode.HALF_EVEN) * BigDecimal(100)
            else quote?.changePct ?: BigDecimal.ZERO

            root.findViewById<TextView>(R.id.asset_name).apply {
                text = asset.name; contentDescription = A11y.Asset.symbol
            }
            root.findViewById<TextView>(R.id.asset_price).apply {
                text = "$" + MoneyFormat.price(price); contentDescription = A11y.Asset.price
            }
            root.findViewById<TextView>(R.id.asset_change).apply {
                text = "${MoneyFormat.percent(changePct)} today"
                setTextColor(Palette.pnl(changePct).toArgb())
                contentDescription = A11y.Asset.change
            }

            listOf(
                R.id.tf_1d to "1D", R.id.tf_1w to "1W", R.id.tf_1m to "1M", R.id.tf_1y to "1Y",
            ).forEach { (id, tf) -> root.findViewById<Button>(id).contentDescription = A11y.Asset.timeframe(tf) }

            root.findViewById<SparklineView>(R.id.asset_sparkline).apply {
                this.symbol = symbol; up = changePct.signum() >= 0
            }

            // `detailStatHighLowSwapped`: the high and low values are swapped.
            val high = (if (active(DefectId.detailStatHighLowSwapped)) quote?.dayLow else quote?.dayHigh) ?: price
            val low = (if (active(DefectId.detailStatHighLowSwapped)) quote?.dayHigh else quote?.dayLow) ?: price
            root.findViewById<TextView>(R.id.stat_marketcap).apply {
                text = marketCap(asset, price); contentDescription = A11y.Asset.statMarketCap
            }
            root.findViewById<TextView>(R.id.stat_volume).apply {
                text = volume(asset); contentDescription = A11y.Asset.statVolume
            }
            root.findViewById<TextView>(R.id.stat_high).apply {
                text = "$" + MoneyFormat.price(high); contentDescription = A11y.Asset.statHigh
            }
            root.findViewById<TextView>(R.id.stat_low).apply {
                text = "$" + MoneyFormat.price(low); contentDescription = A11y.Asset.statLow
            }

            root.findViewById<Button>(R.id.asset_sell).apply {
                contentDescription = A11y.Asset.sellButton
                setOnClickListener { nav.push(Route.OrderTicket(OrderRequest(symbol, OrderSide.sell, price))) }
            }
            root.findViewById<Button>(R.id.asset_buy).apply {
                // `wrongA11yLabel`: the Buy button announces itself as "Sell".
                contentDescription = if (active(DefectId.wrongA11yLabel)) "Sell" else A11y.Asset.buyButton
                setOnClickListener {
                    // `buyButtonPlacesSell`: the Buy button starts a sell ticket.
                    val side = if (active(DefectId.buyButtonPlacesSell)) OrderSide.sell else OrderSide.buy
                    nav.push(Route.OrderTicket(OrderRequest(symbol, side, price)))
                }
            }
            root
        },
    )
}

private fun marketCap(asset: Asset, price: BigDecimal): String {
    val shares = BigDecimal((1_000_000_000uL + StableHash.of(asset.symbol) % 4_000_000_000uL).toLong())
    val cap = (price * shares).divide(BigDecimal(1_000_000_000), 4, RoundingMode.HALF_EVEN)
    return "$" + MoneyFormat.decimal(cap.roundedScale(1), 1) + "B"
}

private fun volume(asset: Asset): String {
    val vol = BigDecimal((10_000_000uL + StableHash.of(asset.symbol + "v") % 90_000_000uL).toLong())
    return "$" + MoneyFormat.decimal(vol.divide(BigDecimal(1_000_000), 4, RoundingMode.HALF_EVEN).roundedScale(1), 1) + "M"
}
