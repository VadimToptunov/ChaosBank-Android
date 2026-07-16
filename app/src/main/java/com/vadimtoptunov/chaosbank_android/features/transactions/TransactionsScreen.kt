package com.vadimtoptunov.chaosbank_android.features.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.components.SecondaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentBar
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentItem
import com.vadimtoptunov.chaosbank_android.ui.components.TransactionRow
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

@Composable
fun TransactionsScreen() {
    val services = LocalAppServices.current
    val vm = remember { TransactionsViewModel(services) }
    LaunchedEffect(Unit) { vm.load() }

    ChaosScreen("Transactions", A11y.Transactions.root, showBadge = false) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.surface)
                .border(1.dp, Palette.line, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("🔍", color = Palette.muted, fontSize = 15.sp)
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
                if (vm.search.isEmpty()) Text("Search", color = Palette.muted, fontSize = 16.sp)
                BasicTextField(
                    value = vm.search, onValueChange = { vm.updateSearch(it) }, singleLine = true,
                    textStyle = TextStyle(color = Palette.text, fontSize = 16.sp),
                    cursorBrush = SolidColor(Palette.sand),
                    modifier = Modifier.fillMaxWidth().testTag(A11y.Transactions.searchField),
                )
            }
        }

        SegmentBar(
            items = listOf(
                SegmentItem(TxFilter.all.name, "All", A11y.Transactions.filterAll),
                SegmentItem(TxFilter.moneyIn.name, "Money in", A11y.Transactions.filterIn),
                SegmentItem(TxFilter.moneyOut.name, "Money out", A11y.Transactions.filterOut),
            ),
            selected = vm.filter.name,
        ) { vm.updateFilter(TxFilter.valueOf(it)) }

        // `transactionCountWrong`: report the visible count, not the filtered total.
        val count = if (Defects.isActive(DefectId.transactionCountWrong)) vm.visible.size else vm.filtered.size
        Text("$count transactions", color = Palette.muted, fontSize = 13.sp, modifier = Modifier.testTag(A11y.Transactions.count))

        Column(Modifier.fillMaxWidth().testTag(A11y.Transactions.list), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            vm.grouped.forEach { (key, rows) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(key, color = Palette.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    CardSurface(padding = 8.dp) {
                        rows.forEachIndexed { index, tx ->
                            TransactionRow(tx, A11y.Transactions.row(tx.id))
                            if (index < rows.size - 1) HorizontalDivider(color = Palette.line)
                        }
                    }
                }
            }
        }

        if (vm.canLoadMore) {
            SecondaryButton("Load more", Modifier.testTag(A11y.Transactions.loadMore)) { vm.loadMore() }
        }
    }
}
