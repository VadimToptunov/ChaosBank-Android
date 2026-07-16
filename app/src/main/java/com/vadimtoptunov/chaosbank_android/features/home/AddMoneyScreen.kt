package com.vadimtoptunov.chaosbank_android.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.app.LocalNavigator
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.backend.BackendException
import com.vadimtoptunov.chaosbank_android.core.money.AmountParser
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.AmountInput
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.components.PrimaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentBar
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentItem
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import kotlinx.coroutines.launch

/** A minimal top-up screen. No planted defect — a straightforward correct credit. */
@Composable
fun AddMoneyScreen() {
    val services = LocalAppServices.current
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(Currency.EUR) }
    var submitting by remember { mutableStateOf(false) }

    val amount = AmountParser.parse(amountText)?.takeIf { it.signum() > 0 }

    ChaosScreen("Add money", "addMoney.root", showBadge = false) {
        SegmentBar(
            items = Currency.entries.map { SegmentItem(it.code, it.code, "addMoney.currency.${it.code}") },
            selected = currency.code,
        ) { currency = Currency.valueOf(it) }

        CardSurface {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Amount", color = Palette.muted, fontSize = 13.sp)
                AmountInput(currency.symbol, amountText, { amountText = it }, "addMoney.amountField")
            }
        }

        PrimaryButton("Add money", Modifier.testTag("addMoney.confirmButton"), enabled = amount != null && !submitting) {
            val a = amount ?: return@PrimaryButton
            scope.launch {
                submitting = true
                try {
                    services.backend.deposit(currency, a)
                    services.bumpData()
                    nav.pop()
                } catch (_: BackendException) {
                } finally {
                    submitting = false
                }
            }
        }
    }
}
