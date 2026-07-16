package com.vadimtoptunov.chaosbank_android.features.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.core.money.Money
import com.vadimtoptunov.chaosbank_android.core.money.MoneyFormat
import com.vadimtoptunov.chaosbank_android.models.Holding
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import java.math.BigDecimal

private val allocationColors = listOf(
    Palette.sand, Palette.gain, Color(0xFF6EA8FE), Color(0xFFC792EA), Palette.loss, Palette.muted,
)

@Composable
fun PortfolioScreen() {
    val services = LocalAppServices.current
    val vm = remember { PortfolioViewModel(services) }
    LaunchedEffect(Unit) { services.startFeed(); vm.load() }
    LaunchedEffect(services.dataVersion) { vm.load() }

    ChaosScreen("Portfolio", A11y.Portfolio.root) {
        CardSurface {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Invested value", color = Palette.muted, fontSize = 13.sp)
                Text(
                    vm.totalValue.formatted, color = Palette.text, fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.testTag(A11y.Portfolio.totalValue),
                )
                val shownPnL = vm.displayPnL(vm.totalPnL)
                Text(
                    "${Money(shownPnL, Currency.USD).formattedSigned} · ${MoneyFormat.percent(vm.displayPnL(vm.totalPnLPercent))} all-time",
                    color = Palette.pnl(shownPnL), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag(A11y.Portfolio.pnl),
                )
            }
        }

        if (vm.holdings.isEmpty()) {
            Text("No holdings yet", color = Palette.muted, fontSize = 15.sp, modifier = Modifier.testTag(A11y.Portfolio.empty))
        } else {
            Row(
                Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).testTag(A11y.Portfolio.allocationBar),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                vm.holdings.forEachIndexed { index, h ->
                    val frac = vm.allocationFraction(h).toFloat().coerceAtLeast(0.001f)
                    Row(Modifier.weight(frac).fillMaxWidth().height(10.dp).background(allocationColors[index % allocationColors.size])) {}
                }
            }

            CardSurface(padding = 6.dp, modifier = Modifier.testTag(A11y.Portfolio.list)) {
                vm.holdings.forEachIndexed { index, h ->
                    HoldingRow(vm, h)
                    if (index < vm.holdings.size - 1) HorizontalDivider(color = Palette.line)
                }
            }
        }
    }
}

@Composable
private fun HoldingRow(vm: PortfolioViewModel, h: Holding) {
    val pnl = vm.displayPnL(vm.pnl(h))
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp).testTag(A11y.Portfolio.holding(h.symbol)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(h.symbol, color = Palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${qtyString(h.quantity)} @ $${MoneyFormat.price(h.avgCost)}", color = Palette.muted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                vm.marketValue(h).formatted, color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace, modifier = Modifier.testTag(A11y.Portfolio.holdingValue(h.symbol)),
            )
            Text(
                Money(pnl, Currency.USD).formattedSigned, color = Palette.pnl(pnl), fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag(A11y.Portfolio.holdingPnl(h.symbol)),
            )
        }
    }
}

private fun qtyString(q: BigDecimal): String {
    val stripped = q.stripTrailingZeros()
    return if (stripped.scale() <= 0) stripped.toBigInteger().toString() else MoneyFormat.decimal(q, 4)
}
