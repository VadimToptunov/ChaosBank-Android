package com.vadimtoptunov.chaosbank_android.features.markets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.MoneyFormat
import com.vadimtoptunov.chaosbank_android.app.LocalNavigator
import com.vadimtoptunov.chaosbank_android.app.Route
import com.vadimtoptunov.chaosbank_android.models.Asset
import com.vadimtoptunov.chaosbank_android.models.AssetKind
import com.vadimtoptunov.chaosbank_android.models.Quote
import com.vadimtoptunov.chaosbank_android.models.SeedData
import com.vadimtoptunov.chaosbank_android.models.TickDirection
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.components.LiveTickerText
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentBar
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentItem
import com.vadimtoptunov.chaosbank_android.ui.components.Sparkline
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import java.math.BigDecimal

@Composable
fun MarketsScreen() {
    val services = LocalAppServices.current
    val nav = LocalNavigator.current
    var segment by remember { mutableStateOf("watchlist") }
    LaunchedEffect(Unit) { services.startFeed() }

    val active = { id: DefectId -> Defects.isActive(id) }

    val assets: List<Asset> = when (segment) {
        // `cryptoShownInStocks`: crypto leaks into the Stocks segment.
        "stocks" -> SeedData.assets.filter { it.kind == AssetKind.stock || (active(DefectId.cryptoShownInStocks) && it.kind == AssetKind.crypto) }
        "crypto" -> SeedData.assets.filter { it.kind == AssetKind.crypto }
        // `watchlistShowsAll`: the watchlist shows every asset.
        else -> if (active(DefectId.watchlistShowsAll)) SeedData.assets
        else SeedData.assets.filter { SeedData.watchlistSymbols.contains(it.symbol) }
    }

    // `duplicateAssetA11yId`: NVDA collides onto AAPL's identifier.
    fun rowTag(symbol: String): String =
        if (active(DefectId.duplicateAssetA11yId) && symbol == "NVDA") A11y.Markets.asset("AAPL")
        else A11y.Markets.asset(symbol)

    ChaosScreen("Markets", A11y.Markets.root) {
        if (services.market.source == com.vadimtoptunov.chaosbank_android.core.feed.PriceSourceKind.live) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag(A11y.Markets.liveBadge),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (services.market.liveConnected) Palette.gain else Palette.muted))
                Text(
                    if (services.market.liveConnected) "LIVE · Yahoo Finance" else "Connecting…",
                    color = if (services.market.liveConnected) Palette.gain else Palette.muted,
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
                )
            }
        }

        SegmentBar(
            items = listOf(
                SegmentItem("watchlist", "Watchlist", A11y.Markets.segmentWatchlist),
                SegmentItem("stocks", "Stocks", A11y.Markets.segmentStocks),
                SegmentItem("crypto", "Crypto", A11y.Markets.segmentCrypto),
            ),
            selected = segment,
        ) { segment = it }

        CardSurface(padding = 6.dp, modifier = Modifier.testTag(A11y.Markets.list)) {
            assets.forEachIndexed { index, asset ->
                // `assetRowOpensWrongDetail`: open the next row's asset.
                val target = if (active(DefectId.assetRowOpensWrongDetail)) assets[(index + 1) % assets.size].symbol else asset.symbol
                // `marketRowNoLabel`: strip the row's accessibility label.
                val label = if (active(DefectId.marketRowNoLabel)) " " else asset.symbol
                MarketRow(
                    asset = asset,
                    quote = services.market.quote(asset.symbol),
                    modifier = Modifier
                        .testTag(rowTag(asset.symbol))
                        .semantics { contentDescription = label }
                        .clickable { nav.push(Route.AssetDetail(target)) },
                )
                if (index < assets.size - 1) HorizontalDivider(color = Palette.line)
            }
        }
    }
}

@Composable
private fun MarketRow(asset: Asset, quote: Quote?, modifier: Modifier = Modifier) {
    val price = quote?.price ?: asset.basePrice
    val changePct = quote?.changePct ?: BigDecimal.ZERO
    // `changePctSignFlipped`: the displayed % change is negated.
    val shownChange = if (Defects.isActive(DefectId.changePctSignFlipped)) changePct.negate() else changePct
    val direction = quote?.lastDirection ?: TickDirection.flat
    // `priceMissingDecimals`: render whole-dollar prices.
    val priceText = "$" + MoneyFormat.price(price, if (Defects.isActive(DefectId.priceMissingDecimals)) 0 else 2)

    Row(
        modifier = modifier.padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.width(110.dp)) {
            Text(asset.symbol, color = Palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(asset.name, color = Palette.muted, fontSize = 12.sp)
        }
        // `sparklineHeavyPoints`: compute an absurd number of points.
        Sparkline(
            asset.symbol, up = changePct.signum() >= 0,
            modifier = Modifier.weight(1f).height(32.dp),
            pointCount = if (Defects.isActive(DefectId.sparklineHeavyPoints)) 4000 else 24,
        )
        Column(Modifier.width(96.dp), horizontalAlignment = Alignment.End) {
            LiveTickerText(priceText, direction, A11y.Markets.assetPrice(asset.symbol), size = 15)
            Text(
                MoneyFormat.percent(shownChange), color = Palette.pnl(shownChange),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag(A11y.Markets.assetChange(asset.symbol)),
            )
        }
    }
}
