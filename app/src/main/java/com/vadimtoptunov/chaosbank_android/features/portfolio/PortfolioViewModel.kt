package com.vadimtoptunov.chaosbank_android.features.portfolio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.app.AppServices
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.core.money.Money
import com.vadimtoptunov.chaosbank_android.models.Holding
import com.vadimtoptunov.chaosbank_android.models.SeedData
import java.math.BigDecimal
import java.math.RoundingMode

class PortfolioViewModel(private val services: AppServices) {
    var holdings by mutableStateOf<List<Holding>>(emptyList()); private set

    private fun active(id: DefectId) = Defects.isActive(id)

    suspend fun load() {
        // `mainThreadStall`: blocking work on the main thread hangs the UI on open.
        if (active(DefectId.mainThreadStall)) Thread.sleep(1200)
        holdings = services.backend.fetchHoldings()
    }

    private fun price(symbol: String): BigDecimal = services.market.price(symbol)

    // `holdingValueUsesCost`: value positions at cost basis instead of live price.
    private fun valuationPrice(h: Holding): BigDecimal = if (active(DefectId.holdingValueUsesCost)) h.avgCost else price(h.symbol)

    // `totalValueOmitsHolding`: drops ETH from the total.
    private val countedHoldings: List<Holding>
        get() = if (active(DefectId.totalValueOmitsHolding)) holdings.filter { it.symbol != "ETH" } else holdings

    val totalValue: Money
        get() = Money(countedHoldings.fold(BigDecimal.ZERO) { acc, h -> acc + h.marketValue(valuationPrice(h)) }, Currency.USD)

    val totalCost: BigDecimal get() = holdings.fold(BigDecimal.ZERO) { acc, h -> acc + h.costBasis }

    val totalPnL: BigDecimal get() = holdings.fold(BigDecimal.ZERO) { acc, h -> acc + h.pnl(price(h.symbol)) }

    val totalPnLPercent: BigDecimal
        get() {
            // `pnlPercentVsValue`: divide by current value instead of cost basis.
            val denom = if (active(DefectId.pnlPercentVsValue)) totalValue.amount else totalCost
            if (denom.signum() == 0) return BigDecimal.ZERO
            val pct = totalPnL.divide(denom, 6, RoundingMode.HALF_EVEN) * BigDecimal(100)
            // `pnlPercentAbsOnly`: drops the sign.
            return if (active(DefectId.pnlPercentAbsOnly)) pct.abs() else pct
        }

    fun marketValue(h: Holding): Money = Money(h.marketValue(valuationPrice(h)), Currency.USD)
    fun pnl(h: Holding): BigDecimal = h.pnl(price(h.symbol))

    fun allocationFraction(h: Holding): Double {
        val total = totalValue.amount
        if (total.signum() == 0) return 0.0
        return h.marketValue(price(h.symbol)).divide(total, 10, RoundingMode.HALF_EVEN).toDouble()
    }

    // `pnlSign`: shows a loss as a gain by taking the absolute value.
    fun displayPnL(value: BigDecimal): BigDecimal = if (active(DefectId.pnlSign)) value.abs() else value

    fun name(symbol: String): String = SeedData.assets.firstOrNull { it.symbol == symbol }?.name ?: symbol
}
