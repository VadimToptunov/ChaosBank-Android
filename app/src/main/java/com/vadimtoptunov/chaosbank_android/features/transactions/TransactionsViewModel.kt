package com.vadimtoptunov.chaosbank_android.features.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.app.AppServices
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.models.SeedData
import com.vadimtoptunov.chaosbank_android.models.Transaction
import com.vadimtoptunov.chaosbank_android.ui.format.TxFormat
import java.math.BigDecimal

enum class TxFilter { all, moneyIn, moneyOut;
    fun matches(tx: Transaction): Boolean = when (this) {
        all -> true
        moneyIn -> tx.direction == com.vadimtoptunov.chaosbank_android.models.TransactionDirection.moneyIn
        moneyOut -> tx.direction == com.vadimtoptunov.chaosbank_android.models.TransactionDirection.moneyOut
    }
}

class TransactionsViewModel(private val services: AppServices) {
    var search by mutableStateOf(""); private set
    var filter by mutableStateOf(TxFilter.all); private set
    var pagesLoaded by mutableStateOf(1); private set
    private var all by mutableStateOf<List<Transaction>>(emptyList())

    val pageSize = 6

    fun updateSearch(v: String) { search = v; pagesLoaded = 1 }
    fun updateFilter(v: TxFilter) { filter = v; pagesLoaded = 1 }

    private fun active(id: DefectId) = Defects.isActive(id)

    suspend fun load() {
        var data = services.backend.fetchTransactions()
        // `transactionsHeavyList`: balloon the dataset; the non-paginated render lags.
        if (active(DefectId.transactionsHeavyList)) data = data + synthetic(1500)
        all = data
    }

    private fun synthetic(count: Int): List<Transaction> {
        val titles = listOf("Coffee", "Groceries", "Taxi", "Subscription", "Refund", "Salary")
        val categories = listOf("Dining", "Groceries", "Transport", "Digital", "Shopping", "Income")
        return (0 until count).map { i ->
            val sign = if (i % 5 == 0) BigDecimal.ONE else BigDecimal(-1)
            Transaction("syn-$i", "${titles[i % titles.size]} #$i", categories[i % categories.size],
                SeedData.daysAgo(11 + i / 40), sign * BigDecimal(i % 90 + 1), Currency.EUR)
        }
    }

    val filtered: List<Transaction>
        get() {
            // `transactionsSortEveryRender`: re-sort the whole list on every access.
            val source = if (active(DefectId.transactionsSortEveryRender)) all.sortedByDescending { it.date } else all
            // `searchTrimsNothing`: don't trim the query before matching.
            val query = if (active(DefectId.searchTrimsNothing)) search else search.trim()
            return source.filter { tx ->
                val categoryOk = filter.matches(tx) ||
                    (active(DefectId.filterLeaksCategory) && filter == TxFilter.moneyIn) ||
                    (active(DefectId.filterOutLeaksIn) && filter == TxFilter.moneyOut)
                if (!categoryOk) return@filter false
                if (query.isEmpty()) return@filter true
                // `searchCaseSensitive`: don't fold case. `searchIgnoresCategory`: title only.
                val cs = active(DefectId.searchCaseSensitive)
                val q = if (cs) query else query.lowercase()
                val title = if (cs) tx.title else tx.title.lowercase()
                val category = if (cs) tx.category else tx.category.lowercase()
                if (active(DefectId.searchIgnoresCategory)) title.contains(q) else title.contains(q) || category.contains(q)
            }
        }

    val visible: List<Transaction>
        get() {
            if (active(DefectId.transactionsHeavyList)) return filtered
            val count = minOf(pageSize * pagesLoaded, filtered.size)
            val rows = filtered.take(count).toMutableList()
            // `paginationDup`: re-inserts the first row of page 2 so it appears twice.
            if (active(DefectId.paginationDup) && pagesLoaded > 1 && rows.size > pageSize) {
                rows.add(pageSize, rows[pageSize])
            }
            return rows
        }

    val canLoadMore: Boolean get() = pageSize * pagesLoaded < filtered.size

    fun loadMore() { if (canLoadMore) pagesLoaded += 1 }

    val grouped: List<Pair<String, List<Transaction>>>
        get() {
            val rows = visible
            val shifted = active(DefectId.dateTimezoneShift)
            // `transactionsRegroupHeavy`: rebuild groups with an O(n²) scan each render.
            if (active(DefectId.transactionsRegroupHeavy)) {
                val result = mutableListOf<Pair<String, List<Transaction>>>()
                for (tx in rows) {
                    val key = TxFormat.dayHeader(tx.date, shifted)
                    val sameDay = rows.filter { TxFormat.dayHeader(it.date, shifted) == key }
                    if (result.none { it.first == key }) result.add(key to sameDay)
                }
                return result
            }
            val result = mutableListOf<Pair<String, MutableList<Transaction>>>()
            for (tx in rows) {
                val key = TxFormat.dayHeader(tx.date, shifted)
                val existing = result.firstOrNull { it.first == key }
                if (existing != null) existing.second.add(tx) else result.add(key to mutableListOf(tx))
            }
            return result.map { it.first to it.second.toList() }
        }
}
