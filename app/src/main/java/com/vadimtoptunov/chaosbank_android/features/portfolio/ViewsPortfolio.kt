package com.vadimtoptunov.chaosbank_android.features.portfolio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vadimtoptunov.chaosbank_android.R
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.core.money.Money
import com.vadimtoptunov.chaosbank_android.models.Holding
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices

/**
 * The "views build" rendering of Portfolio: a header (total value + P&L) over a
 * RecyclerView of holdings, from an inflated XML layout hosted via AndroidView,
 * reusing PortfolioViewModel. Hosts defects characteristic of the Android View system.
 */
@Composable
fun ViewsPortfolioScreen() {
    val services = LocalAppServices.current
    val vm = remember { PortfolioViewModel(services) }
    var holdings by remember { mutableStateOf<List<Holding>>(emptyList()) }
    LaunchedEffect(Unit) {
        vm.load()
        holdings = vm.holdings
    }

    AndroidView(
        modifier = Modifier.fillMaxSize().testTag(A11y.Portfolio.root),
        factory = { ctx ->
            val root = LayoutInflater.from(ctx).inflate(R.layout.screen_portfolio_views, null)
            val list = root.findViewById<RecyclerView>(R.id.portfolio_list)
            list.layoutManager = LinearLayoutManager(ctx)
            list.contentDescription = A11y.Portfolio.list
            list.adapter = HoldingAdapter(vm)
            root
        },
        update = { root ->
            root.findViewById<TextView>(R.id.portfolio_total).apply {
                // Correct: render via the currency formatter. `labelNotFormatted`: bind the
                // raw amount, so the total shows with no symbol/grouping.
                text = if (Defects.isActive(DefectId.labelNotFormatted)) vm.totalValue.amount.toString()
                       else vm.totalValue.formatted
                contentDescription = A11y.Portfolio.totalValue
            }
            root.findViewById<TextView>(R.id.portfolio_pnl).apply {
                text = Money(vm.totalPnL, Currency.USD).formattedSigned
                contentDescription = A11y.Portfolio.pnl
            }
            (root.findViewById<RecyclerView>(R.id.portfolio_list).adapter as HoldingAdapter).submit(holdings)
        },
    )
}

private class HoldingAdapter(private val vm: PortfolioViewModel) : RecyclerView.Adapter<HoldingHolder>() {
    private var items: List<Holding> = emptyList()

    fun submit(list: List<Holding>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HoldingHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_holding, parent, false)
        return HoldingHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: HoldingHolder, position: Int) = holder.bind(items[position], vm)
}

private class HoldingHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val symbol: TextView = view.findViewById(R.id.holding_symbol)
    private val value: TextView = view.findViewById(R.id.holding_value)

    fun bind(h: Holding, vm: PortfolioViewModel) {
        symbol.text = h.symbol
        value.text = vm.marketValue(h).formatted
        // Correct: each row sets its locator. `rowLocatorMissing`: the id is never set,
        // so per-holding locators (portfolio.holding.<symbol>) don't exist.
        itemView.contentDescription =
            if (Defects.isActive(DefectId.rowLocatorMissing)) null else A11y.Portfolio.holding(h.symbol)
    }
}
