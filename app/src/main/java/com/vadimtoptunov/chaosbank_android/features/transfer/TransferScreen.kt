package com.vadimtoptunov.chaosbank_android.features.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.app.LocalNavigator
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.Money
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.components.LabeledField
import com.vadimtoptunov.chaosbank_android.ui.components.PrimaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.SecondaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.Toast
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen() {
    val services = LocalAppServices.current
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val vm = remember { TransferViewModel(services) }
    LaunchedEffect(Unit) { vm.load() }

    LaunchedEffect(vm.succeeded) {
        if (vm.succeeded) {
            // `successToastTooBrief`: dismiss almost immediately.
            delay(if (Defects.isActive(DefectId.successToastTooBrief)) 120 else 1400)
            nav.pop()
        }
    }

    Box(Modifier.fillMaxWidth()) {
        ChaosScreen("Transfer", A11y.Transfer.root, showBadge = false) {
            // Saved payment templates (banking-breadth). Tapping one prefills the form.
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).testTag(A11y.Transfer.templatesRow),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                services.templates.templates.forEach { t ->
                    androidx.compose.foundation.layout.Box(
                        Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(50)).background(Palette.surface2)
                            .clickable {
                                vm.recipient = t.recipient
                                vm.amountText = services.templates.prefillAmount(t).toPlainString()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp).testTag(A11y.Transfer.template(t.id)),
                    ) {
                        Text(t.name, color = Palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            CardSurface {
                LabeledField("Recipient", vm.recipient, { vm.recipient = it }, A11y.Transfer.recipientField, "Name or IBAN")
                HorizontalDivider(color = Palette.line, modifier = Modifier.padding(vertical = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Amount", color = Palette.muted, fontSize = 13.sp)
                    com.vadimtoptunov.chaosbank_android.ui.components.AmountInput(vm.fromCurrency.symbol, vm.amountText, { vm.amountText = it }, A11y.Transfer.amountField)
                }
                HorizontalDivider(color = Palette.line, modifier = Modifier.padding(vertical = 12.dp))
                LabeledField("Note", vm.note, { vm.note = it }, A11y.Transfer.noteField, "Optional")
            }

            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Balance after", color = Palette.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(
                    (vm.balanceAfter ?: Money(vm.fromBalance, vm.fromCurrency)).formatted,
                    color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag(A11y.Transfer.balanceAfter),
                )
            }

            vm.errorMessage?.let {
                Text(it, color = Palette.loss, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.testTag(A11y.Transfer.error))
            }

            // `disabledButtonTappable`: looks disabled when invalid but stays tappable.
            val fakeDisabled = Defects.isActive(DefectId.disabledButtonTappable)
            PrimaryButton(
                "Continue", Modifier.testTag(A11y.Transfer.continueButton),
                enabled = if (fakeDisabled) true else vm.canContinue,
                looksDisabled = !vm.canContinue,
            ) {
                vm.errorMessage = null
                vm.showConfirm = true
            }
        }

        // `successToastMissing`: no confirmation toast is shown.
        if (vm.succeeded && !Defects.isActive(DefectId.successToastMissing)) {
            Toast("Transfer sent", A11y.Transfer.successToast, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        }
    }

    if (vm.showConfirm) {
        ModalBottomSheet(
            onDismissRequest = { vm.showConfirm = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Palette.bg,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp).testTag(A11y.Transfer.confirmSheet),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("Confirm transfer", color = Palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                CardSurface {
                    ConfirmRow("To", vm.confirmRecipientText)
                    HorizontalDivider(color = Palette.line, modifier = Modifier.padding(vertical = 12.dp))
                    ConfirmRow("Amount", Money(vm.amount ?: BigDecimal.ZERO, vm.fromCurrency).formatted)
                    if (vm.note.isNotEmpty()) {
                        HorizontalDivider(color = Palette.line, modifier = Modifier.padding(vertical = 12.dp))
                        ConfirmRow("Note", vm.note)
                    }
                }
                vm.errorMessage?.let { Text(it, color = Palette.loss, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                Spacer(Modifier.height(4.dp))
                if (vm.canRetry) {
                    SecondaryButton("Retry", Modifier.testTag(A11y.Transfer.retryButton)) {
                        scope.launch { vm.retry(); if (vm.succeeded) vm.showConfirm = false }
                    }
                }
                // Not disabled while submitting: idempotency is in the view model,
                // which is what lets a double-tap exercise `doubleCharge`.
                PrimaryButton("Confirm", Modifier.testTag(A11y.Transfer.confirmButton)) {
                    scope.launch { vm.confirmTransfer(); if (vm.succeeded) vm.showConfirm = false }
                }
            }
        }
    }
}

@Composable
private fun ConfirmRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Palette.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
