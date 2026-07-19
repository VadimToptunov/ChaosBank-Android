package com.vadimtoptunov.chaosbank_android.features.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.components.PrimaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.SecondaryButton
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

@Composable
fun CardScreen() {
    val vm = remember { CardViewModel() }
    var showPin by remember { mutableStateOf(false) }
    var virtualCreated by remember { mutableStateOf(false) }

    ChaosScreen("Card", A11y.Card.root) {
        CardVisual(vm)

        CardSurface {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SettingLabel("❄️", "Freeze card", Modifier.weight(1f))
                // `freezeToggleNoLabel`: strip the toggle's accessibility label.
                val freezeLabel = if (Defects.isActive(DefectId.freezeToggleNoLabel)) " " else "Freeze card"
                Switch(
                    checked = vm.frozen, onCheckedChange = { vm.frozen = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Palette.sand),
                    modifier = Modifier.testTag(A11y.Card.freezeToggle).semantics { contentDescription = freezeLabel },
                )
            }
            HorizontalDivider(color = Palette.line)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SettingLabel("🌐", "Online payments", Modifier.weight(1f))
                Switch(
                    checked = vm.onlinePayments, onCheckedChange = { vm.onlinePayments = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Palette.sand),
                    modifier = Modifier.testTag(A11y.Card.onlinePaymentsToggle),
                )
            }
            HorizontalDivider(color = Palette.line)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SettingLabel("⏱", "Monthly limit", Modifier.weight(1f))
                Text("$", color = Palette.sand, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                BasicTextField(
                    value = vm.monthlyLimitText, onValueChange = { vm.monthlyLimitText = it }, singleLine = true,
                    textStyle = TextStyle(color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End),
                    cursorBrush = SolidColor(Palette.sand),
                    modifier = Modifier.width(80.dp).testTag(A11y.Card.limitField),
                )
            }
            vm.limitError?.let {
                Text(
                    it, color = Palette.loss, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().testTag(A11y.Card.limitError), textAlign = TextAlign.End,
                )
            }
        }

        SecondaryButton("Show PIN", Modifier.testTag(A11y.Card.pinButton)) { showPin = true }

        // Virtual card issuance (banking-breadth). Should reveal a distinct number.
        if (virtualCreated) {
            CardSurface {
                Text("Virtual card", color = Palette.muted, fontSize = 12.sp)
                Text(
                    vm.virtualCardNumber, color = Palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.testTag(A11y.Card.virtualNumber),
                )
            }
        }
        SecondaryButton("Create virtual card", Modifier.testTag(A11y.Card.virtualButton)) { virtualCreated = true }

        PrimaryButton("Order physical card", Modifier.testTag(A11y.Card.orderPhysicalButton)) {}
    }

    if (showPin) {
        AlertDialog(
            onDismissRequest = { showPin = false },
            confirmButton = { TextButton(onClick = { showPin = false }) { Text("Done", color = Palette.sand) } },
            title = { Text("Card PIN") },
            text = { Text("Your PIN is ${vm.pinText}") },
            containerColor = Palette.surface,
        )
    }
}

@Composable
private fun CardVisual(vm: CardViewModel) {
    Box(
        Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Palette.surface2, Palette.bg)))
            .border(1.dp, Palette.line, RoundedCornerShape(20.dp))
            .alpha(if (vm.frozen) 0.55f else 1f)
            .testTag(A11y.Card.visual),
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ChaosBank", color = Palette.sand, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (vm.frozen) {
                    Text(
                        "FROZEN", color = Palette.bg, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Palette.loss).padding(horizontal = 8.dp, vertical = 4.dp).testTag(A11y.Card.frozenBadge),
                    )
                }
            }
            Box(Modifier.weight(1f))
            Text(
                vm.displayedPAN, color = Palette.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace, modifier = Modifier.testTag(A11y.Card.number),
            )
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(vm.holder, color = Palette.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                vm.visibleCVV?.let {
                    Text("CVV $it", color = Palette.loss, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(end = 10.dp).testTag(A11y.Card.cvv))
                }
                Text(vm.expiry, color = Palette.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun SettingLabel(glyph: String, title: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(glyph, fontSize = 15.sp)
        Text(title, color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
