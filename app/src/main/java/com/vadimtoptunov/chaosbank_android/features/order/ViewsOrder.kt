package com.vadimtoptunov.chaosbank_android.features.order

import android.app.AlertDialog
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import com.vadimtoptunov.chaosbank_android.R
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
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * The "views build" rendering of the order ticket — a faithful twin of the
 * Compose [OrderScreen], inflated from XML and bound to the same OrderViewModel
 * (so it shares its defects: qtyIncrementByTwo, orderDoubleSubmit, missingA11yLabel…).
 * The View system isn't reactive, so state changes call [render] by hand.
 */
@Composable
fun ViewsOrderScreen(request: OrderRequest) {
    val services = LocalAppServices.current
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val vm = remember { OrderViewModel(request, services) }
    LaunchedEffect(Unit) { vm.load() }

    AndroidView(
        modifier = Modifier.fillMaxSize().testTag(A11y.Order.root),
        factory = { ctx ->
            val container = FrameLayout(ctx)
            val root = LayoutInflater.from(ctx).inflate(R.layout.screen_order_views, container, false)
            container.addView(root)

            val sideBuy = root.findViewById<Button>(R.id.seg_side_buy).also { it.contentDescription = A11y.Order.sideBuy }
            val sideSell = root.findViewById<Button>(R.id.seg_side_sell).also { it.contentDescription = A11y.Order.sideSell }
            val typeMarket = root.findViewById<Button>(R.id.seg_type_market).also { it.contentDescription = A11y.Order.typeMarket }
            val typeLimit = root.findViewById<Button>(R.id.seg_type_limit).also { it.contentDescription = A11y.Order.typeLimit }
            val qtyValue = root.findViewById<TextView>(R.id.order_qty_value).also { it.contentDescription = A11y.Order.qtyValue }
            val qtyDec = root.findViewById<Button>(R.id.order_qty_dec).also { it.contentDescription = A11y.Order.qtyDecrement }
            val qtyInc = root.findViewById<Button>(R.id.order_qty_inc).also { it.contentDescription = A11y.Order.qtyIncrement }
            val limitRow = root.findViewById<View>(R.id.order_limit_row)
            val limitField = root.findViewById<EditText>(R.id.order_limit_field).also { it.contentDescription = A11y.Order.limitPriceField }
            val refPrice = root.findViewById<TextView>(R.id.order_ref_price).also { it.contentDescription = A11y.Order.refPrice }
            val estTotal = root.findViewById<TextView>(R.id.order_est_total).also { it.contentDescription = A11y.Order.estTotal }
            val warning = root.findViewById<TextView>(R.id.order_warning).also { it.contentDescription = A11y.Order.warning }
            val error = root.findViewById<TextView>(R.id.order_error)
            val review = root.findViewById<Button>(R.id.order_review).also { it.contentDescription = A11y.Order.reviewButton }

            fun render() {
                styleSegment(sideBuy, vm.side == OrderSide.buy)
                styleSegment(sideSell, vm.side == OrderSide.sell)
                styleSegment(typeMarket, vm.type == OrderType.market)
                styleSegment(typeLimit, vm.type == OrderType.limit)
                qtyValue.text = qtyString(vm.quantity)
                limitRow.visibility = if (vm.type == OrderType.limit) View.VISIBLE else View.GONE
                refPrice.text = "$" + MoneyFormat.price(vm.referencePrice)
                estTotal.text = vm.estTotal.formatted
                warning.visibility = if (vm.showWarning) View.VISIBLE else View.GONE
                error.visibility = if (vm.errorMessage != null) View.VISIBLE else View.GONE
                error.text = vm.errorMessage
                review.isEnabled = vm.isValid
                review.alpha = if (vm.isValid) 1f else 0.5f
            }

            sideBuy.setOnClickListener { vm.side = OrderSide.buy; render() }
            sideSell.setOnClickListener { vm.side = OrderSide.sell; render() }
            typeMarket.setOnClickListener { vm.type = OrderType.market; render() }
            typeLimit.setOnClickListener { vm.type = OrderType.limit; render() }
            qtyDec.setOnClickListener { vm.decrement(); render() }
            qtyInc.setOnClickListener { vm.increment(); render() }
            limitField.setText(vm.limitPriceText)
            limitField.doAfterTextChanged { vm.limitPriceText = it?.toString() ?: ""; render() }

            review.setOnClickListener {
                vm.errorMessage = null
                showConfirm(ctx, vm) {
                    // placed callback
                    showToast(container, statusMessage(vm.status))
                    if (vm.status == OrderStatus.filled) scope.launch { delay(1400); nav.pop() }
                }
            }

            render()
            container
        },
    )
}

private fun showConfirm(ctx: android.content.Context, vm: OrderViewModel, onPlaced: () -> Unit) {
    val view = LayoutInflater.from(ctx).inflate(R.layout.screen_order_confirm_views, null)
    view.contentDescription = A11y.Order.confirmSheet
    view.findViewById<TextView>(R.id.confirm_title).text =
        "${if (vm.side == OrderSide.buy) "Buy" else "Sell"} ${vm.symbol}"
    view.findViewById<TextView>(R.id.confirm_qty).text = qtyString(vm.quantity)
    view.findViewById<TextView>(R.id.confirm_price).text = "$" + MoneyFormat.price(vm.executionPrice)
    view.findViewById<TextView>(R.id.confirm_total).text = vm.estTotal.formatted
    val dialog = AlertDialog.Builder(ctx).setView(view).create()

    val place = view.findViewById<Button>(R.id.order_place)
    place.contentDescription = if (Defects.isActive(DefectId.missingA11yLabel)) " " else A11y.Order.placeButton
    // Not disabled while submitting: idempotency lives in the view model, so a
    // double-tap can exercise `orderDoubleSubmit`.
    val scope = kotlinx.coroutines.MainScope()
    place.setOnClickListener {
        scope.launch {
            vm.place()
            if (vm.placed) { dialog.dismiss(); onPlaced() }
        }
    }
    dialog.show()
}

private fun styleSegment(button: Button, selected: Boolean) {
    button.setBackgroundColor((if (selected) Palette.sand else Palette.surface2).toArgb())
    button.setTextColor((if (selected) Palette.bg else Palette.text).toArgb())
}

private fun showToast(container: FrameLayout, message: String) {
    val toast = TextView(container.context).apply {
        text = message
        setTextColor(Palette.text.toArgb())
        setBackgroundColor(Palette.surface2.toArgb())
        setPadding(28, 16, 28, 16)
        textSize = 14f
        contentDescription = A11y.Order.statusToast
    }
    val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
    lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
    lp.topMargin = 24
    container.addView(toast, lp)
    toast.postDelayed({ container.removeView(toast) }, 2200)
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
