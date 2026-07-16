package com.vadimtoptunov.chaosbank_android.features.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
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
import com.vadimtoptunov.chaosbank_android.app.LocalNavigator
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.MoneyFormat
import com.vadimtoptunov.chaosbank_android.models.OrderRequest
import com.vadimtoptunov.chaosbank_android.models.OrderSide
import com.vadimtoptunov.chaosbank_android.models.OrderStatus
import com.vadimtoptunov.chaosbank_android.models.OrderType
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.components.PrimaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.Toast
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentBar
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentItem
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(request: OrderRequest) {
    val services = LocalAppServices.current
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val vm = remember { OrderViewModel(request, services) }
    LaunchedEffect(Unit) { vm.load() }

    // Auto-dismiss shortly after a fill.
    LaunchedEffect(vm.placed, vm.status) {
        if (vm.placed && vm.status == OrderStatus.filled) {
            delay(1400)
            nav.pop()
        }
    }

    Box(Modifier.fillMaxWidth()) {
        ChaosScreen("Order · ${request.symbol}", A11y.Order.root, showBadge = false) {
            SegmentBar(
                items = listOf(
                    SegmentItem(OrderSide.buy.name, "Buy", A11y.Order.sideBuy),
                    SegmentItem(OrderSide.sell.name, "Sell", A11y.Order.sideSell),
                ),
                selected = vm.side.name,
            ) { vm.side = OrderSide.valueOf(it) }

            SegmentBar(
                items = listOf(
                    SegmentItem(OrderType.market.name, "Market", A11y.Order.typeMarket),
                    SegmentItem(OrderType.limit.name, "Limit", A11y.Order.typeLimit),
                ),
                selected = vm.type.name,
            ) { vm.type = OrderType.valueOf(it) }

            CardSurface {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Quantity", color = Palette.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StepperButton("−", A11y.Order.qtyDecrement) { vm.decrement() }
                        Text(
                            qtyString(vm.quantity), color = Palette.text, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(48.dp).testTag(A11y.Order.qtyValue), textAlign = TextAlign.Center,
                        )
                        StepperButton("+", A11y.Order.qtyIncrement) { vm.increment() }
                    }
                }
                if (vm.type == OrderType.limit) {
                    HorizontalDivider(color = Palette.line, modifier = Modifier.padding(vertical = 12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Limit price", color = Palette.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("$", color = Palette.sand, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        BasicTextField(
                            value = vm.limitPriceText,
                            onValueChange = { vm.limitPriceText = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Palette.text, fontSize = 16.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End),
                            cursorBrush = SolidColor(Palette.sand),
                            modifier = Modifier.width(100.dp).testTag(A11y.Order.limitPriceField),
                        )
                    }
                }
            }

            CardSurface {
                SummaryRow("Reference price", "$" + MoneyFormat.price(vm.referencePrice), A11y.Order.refPrice)
                HorizontalDivider(color = Palette.line, modifier = Modifier.padding(vertical = 12.dp))
                SummaryRow("Estimated total", vm.estTotal.formatted, A11y.Order.estTotal)
            }

            if (vm.showWarning) {
                Text(
                    "⚠️ Limit sell below market — will execute immediately.",
                    color = Palette.loss, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag(A11y.Order.warning),
                )
            }
            vm.errorMessage?.let { Text(it, color = Palette.loss, fontSize = 14.sp, fontWeight = FontWeight.Medium) }

            PrimaryButton("Review order", Modifier.testTag(A11y.Order.reviewButton), enabled = vm.isValid) {
                vm.errorMessage = null
                vm.showConfirm = true
            }
        }

        if (vm.placed) {
            Toast(
                statusMessage(vm.status), A11y.Order.statusToast,
                Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            )
        }
    }

    if (vm.showConfirm) {
        ModalBottomSheet(
            onDismissRequest = { vm.showConfirm = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Palette.bg,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp).testTag(A11y.Order.confirmSheet),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    "${if (vm.side == OrderSide.buy) "Buy" else "Sell"} ${vm.symbol}",
                    color = Palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                )
                CardSurface {
                    SummaryRow("Quantity", qtyString(vm.quantity), null)
                    HorizontalDivider(color = Palette.line, modifier = Modifier.padding(vertical = 12.dp))
                    SummaryRow("Price", "$" + MoneyFormat.price(vm.executionPrice), null)
                    HorizontalDivider(color = Palette.line, modifier = Modifier.padding(vertical = 12.dp))
                    SummaryRow("Total", vm.estTotal.formatted, null)
                }
                Spacer(Modifier.height(4.dp))
                // Not disabled while submitting: idempotency lives in the view model,
                // so a double-tap can exercise `orderDoubleSubmit`.
                val placeLabel = if (Defects.isActive(DefectId.missingA11yLabel)) " " else "Place order"
                PrimaryButton(
                    "Place order",
                    Modifier.testTag(A11y.Order.placeButton).semantics { contentDescription = placeLabel },
                ) {
                    scope.launch {
                        vm.place()
                        if (vm.placed) vm.showConfirm = false
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperButton(glyph: String, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(Palette.surface2).clickable { onClick() }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = Palette.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, tag: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Palette.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            value, color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
            modifier = if (tag != null) Modifier.testTag(tag) else Modifier,
        )
    }
}

private fun statusMessage(status: OrderStatus?): String = when (status) {
    OrderStatus.filled -> "Order filled"
    OrderStatus.pending -> "Order pending…"
    OrderStatus.rejected -> "Order rejected"
    null -> ""
}

private fun qtyString(q: BigDecimal): String {
    val stripped = q.stripTrailingZeros()
    return if (stripped.scale() <= 0) stripped.toBigInteger().toString() else MoneyFormat.decimal(q, 4)
}
