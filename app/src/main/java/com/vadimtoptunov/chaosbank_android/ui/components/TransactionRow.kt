package com.vadimtoptunov.chaosbank_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.models.Transaction
import com.vadimtoptunov.chaosbank_android.models.TransactionDirection
import com.vadimtoptunov.chaosbank_android.ui.format.TxFormat
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

@Composable
fun TransactionRow(tx: Transaction, tag: String) {
    val incoming = tx.direction == TransactionDirection.moneyIn
    Row(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(Palette.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(TxFormat.emoji(tx.category), fontSize = 16.sp)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(tx.title, color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                TxFormat.rowTime(tx.date, Defects.isActive(DefectId.dateTimezoneShift)),
                color = Palette.muted, fontSize = 12.sp,
            )
        }
        // `outgoingSignHidden`: outgoing amounts drop the leading minus.
        val text = if (tx.direction == TransactionDirection.moneyOut && Defects.isActive(DefectId.outgoingSignHidden))
            tx.money.formatted else tx.money.formattedSigned
        Text(
            text, color = if (incoming) Palette.gain else Palette.text,
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
        )
    }
}
