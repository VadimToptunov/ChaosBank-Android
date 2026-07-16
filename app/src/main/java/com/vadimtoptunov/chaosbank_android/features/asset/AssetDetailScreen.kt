package com.vadimtoptunov.chaosbank_android.features.asset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.vadimtoptunov.chaosbank_android.models.TickDirection
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.components.LiveTickerText
import com.vadimtoptunov.chaosbank_android.ui.components.PrimaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.SecondaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentBar
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentItem
import com.vadimtoptunov.chaosbank_android.ui.components.Sparkline
import com.vadimtoptunov.chaosbank_android.ui.components.StatTile
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import java.math.BigDecimal

@Composable
fun AssetDetailScreen(symbol: String) {
    val services = LocalAppServices.current
    val nav = LocalNavigator.current
    var timeframe by remember { mutableStateOf("1D") }
    LaunchedEffect(Unit) { services.startFeed() }

    val asset = SeedData.assets.firstOrNull { it.symbol == symbol } ?: return
    val quote = services.market.quote(symbol)
    val active = { id: DefectId -> Defects.isActive(id) }

    val base = quote?.price ?: asset.basePrice
    // `detailPriceOffset`: the detail price drifts from the market price.
    val price = if (active(DefectId.detailPriceOffset)) base + BigDecimal("5") else base
    // `detailChangeWrongBase`: measure change vs the anchor base, not day open.
    val changePct = if (active(DefectId.detailChangeWrongBase) && asset.basePrice.signum() != 0)
        (price - asset.basePrice).divide(asset.basePrice, 6, java.math.RoundingMode.HALF_EVEN) * BigDecimal(100)
    else quote?.changePct ?: BigDecimal.ZERO

    ChaosScreen(symbol, A11y.Asset.root, showBadge = false) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(asset.name, color = Palette.muted, fontSize = 14.sp, modifier = Modifier.testTag(A11y.Asset.symbol))
            LiveTickerText("$" + MoneyFormat.price(price), quote?.lastDirection ?: TickDirection.flat, A11y.Asset.price, size = 40, weight = FontWeight.Bold)
            Text(
                "${MoneyFormat.percent(changePct)} today", color = Palette.pnl(changePct),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag(A11y.Asset.change),
            )
        }

        SegmentBar(
            items = listOf("1D", "1W", "1M", "1Y").map { SegmentItem(it, it, A11y.Asset.timeframe(it)) },
            selected = timeframe,
        ) { timeframe = it }

        Sparkline(symbol, up = changePct.signum() >= 0, modifier = Modifier.fillMaxWidth().height(140.dp).padding(vertical = 8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Market cap", marketCap(asset, price), A11y.Asset.statMarketCap, Modifier.weight(1f))
            StatTile("Volume", volume(asset), A11y.Asset.statVolume, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // `detailStatHighLowSwapped`: the high and low values are swapped.
            val high = (if (active(DefectId.detailStatHighLowSwapped)) quote?.dayLow else quote?.dayHigh) ?: price
            val low = (if (active(DefectId.detailStatHighLowSwapped)) quote?.dayHigh else quote?.dayLow) ?: price
            StatTile("Day high", "$" + MoneyFormat.price(high), A11y.Asset.statHigh, Modifier.weight(1f))
            StatTile("Day low", "$" + MoneyFormat.price(low), A11y.Asset.statLow, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Sell", Modifier.weight(1f).testTag(A11y.Asset.sellButton)) {
                nav.push(Route.OrderTicket(OrderRequest(symbol, OrderSide.sell, price)))
            }
            // `wrongA11yLabel`: the Buy button announces itself as "Sell".
            val buyLabel = if (active(DefectId.wrongA11yLabel)) "Sell" else "Buy"
            PrimaryButton(
                "Buy",
                Modifier.weight(1f).testTag(A11y.Asset.buyButton).semantics { contentDescription = buyLabel },
            ) {
                // `buyButtonPlacesSell`: the Buy button starts a sell ticket.
                val side = if (active(DefectId.buyButtonPlacesSell)) OrderSide.sell else OrderSide.buy
                nav.push(Route.OrderTicket(OrderRequest(symbol, side, price)))
            }
        }
    }
}

private fun marketCap(asset: Asset, price: BigDecimal): String {
    val shares = BigDecimal((1_000_000_000uL + StableHash.of(asset.symbol) % 4_000_000_000uL).toLong())
    val cap = (price * shares).divide(BigDecimal(1_000_000_000), 4, java.math.RoundingMode.HALF_EVEN)
    return "$" + MoneyFormat.decimal(cap.roundedScale(1), 1) + "B"
}

private fun volume(asset: Asset): String {
    val vol = BigDecimal((10_000_000uL + StableHash.of(asset.symbol + "v") % 90_000_000uL).toLong())
    return "$" + MoneyFormat.decimal(vol.divide(BigDecimal(1_000_000), 4, java.math.RoundingMode.HALF_EVEN).roundedScale(1), 1) + "M"
}
