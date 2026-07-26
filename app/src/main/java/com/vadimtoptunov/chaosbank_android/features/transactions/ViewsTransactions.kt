package com.vadimtoptunov.chaosbank_android.features.transactions

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
import com.vadimtoptunov.chaosbank_android.models.Transaction
import com.vadimtoptunov.chaosbank_android.models.TransactionDirection
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices

/**
 * The "views build" rendering of Transactions: a RecyclerView (XML rows) embedded
 * via AndroidView, reusing the exact same [TransactionsViewModel]. Only the view
 * layer differs from the Compose screen. Hosts defects characteristic of the
 * Android View system (RecyclerView recycling), gated the usual way.
 *
 * Locators use `contentDescription` (the View-system a11y surface) — the parallel
 * to the UIKit build's accessibilityIdentifier — so Espresso can drive it.
 */
@Composable
fun ViewsTransactionsScreen() {
    val services = LocalAppServices.current
    val vm = remember { TransactionsViewModel(services) }
    var rows by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    LaunchedEffect(Unit) {
        vm.load()
        rows = vm.filtered
    }

    AndroidView(
        modifier = Modifier.fillMaxSize().testTag(A11y.Transactions.root),
        factory = { ctx ->
            RecyclerView(ctx).apply {
                layoutManager = LinearLayoutManager(ctx)
                adapter = TxAdapter()
                contentDescription = A11y.Transactions.list
            }
        },
        update = { rv -> (rv.adapter as TxAdapter).submit(rows) },
    )
}

private class TxAdapter : RecyclerView.Adapter<TxViewHolder>() {
    private var items: List<Transaction> = emptyList()

    fun submit(list: List<Transaction>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TxViewHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: TxViewHolder, position: Int) = holder.bind(items[position])
}

private class TxViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.tx_title)
    private val amount: TextView = view.findViewById(R.id.tx_amount)

    fun bind(tx: Transaction) {
        title.text = tx.title
        itemView.contentDescription = A11y.Transactions.row(tx.id)

        val text = tx.money.formattedSigned
        // Correct: every recycled holder resets its amount, so a scrolled row never
        // shows another row's value. `listCellReuseBleed`: the money-out branch is
        // skipped on reuse, so a recycled money-in holder keeps its "+…" amount on a
        // money-out row — the classic RecyclerView onBindViewHolder recycle bleed.
        if (tx.direction == TransactionDirection.moneyIn) {
            amount.text = text
        } else if (!Defects.isActive(DefectId.listCellReuseBleed)) {
            amount.text = text
        }
    }
}
