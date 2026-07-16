package com.vadimtoptunov.chaosbank_android.features.exchange

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.app.LocalNavigator
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.core.money.Money
import com.vadimtoptunov.chaosbank_android.core.money.MoneyFormat
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.AmountInput
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.components.PrimaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.Toast
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExchangeScreen() {
    val services = LocalAppServices.current
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val vm = remember { ExchangeViewModel(services) }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(vm.succeeded) {
        if (vm.succeeded) { delay(1400); nav.pop() }
    }

    Box(Modifier.fillMaxWidth()) {
        ChaosScreen("Exchange", A11y.Exchange.root, showBadge = false) {
            CardSurface {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sell", color = Palette.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    CurrencyMenu(vm.sell, A11y.Exchange.sellCurrency) { vm.selectSell(it) }
                }
                AmountInput(vm.sell.symbol, vm.amountText, { vm.amountText = it }, A11y.Exchange.amountField)
                Text("Balance ${Money(vm.sellBalance, vm.sell).formatted}", color = Palette.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }

            Box(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Palette.sand).clickable { vm.swapDirection() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⇅", color = Palette.bg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            CardSurface {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Get", color = Palette.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    CurrencyMenu(vm.get, A11y.Exchange.getCurrency) { vm.selectGet(it) }
                }
                Text(
                    vm.youGet.formatted, color = Palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp).testTag(A11y.Exchange.youGet),
                )
            }

            InfoRow("Rate", "1 ${vm.sell.code} = ${MoneyFormat.decimal(vm.rate, 4)} ${vm.get.code}", A11y.Exchange.rate)
            InfoRow("Fee (0.5%)", vm.fee.formatted, A11y.Exchange.fee)

            vm.errorMessage?.let { Text(it, color = Palette.loss, fontSize = 14.sp, fontWeight = FontWeight.Medium) }

            // Not disabled while submitting: idempotency lives in the view model so a
            // double-tap can exercise `exchangeDoubleSubmit`.
            PrimaryButton("Exchange", Modifier.testTag(A11y.Exchange.executeButton), enabled = vm.canExecute) {
                scope.launch { vm.execute() }
            }
        }

        if (vm.succeeded) {
            Toast("Exchanged ${vm.youGet.formatted}", A11y.Exchange.successToast, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        }
    }
}

@Composable
private fun CurrencyMenu(selected: Currency, tag: String, onSelect: (Currency) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clickable { open = true }.testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(selected.code, color = Palette.sand, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("▾", color = Palette.sand, fontSize = 12.sp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Currency.entries.forEach { c ->
                DropdownMenuItem(text = { Text(c.code) }, onClick = { onSelect(c); open = false })
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, tag: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Palette.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = Palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, modifier = Modifier.testTag(tag))
    }
}
