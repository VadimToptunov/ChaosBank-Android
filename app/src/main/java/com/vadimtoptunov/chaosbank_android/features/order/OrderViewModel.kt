package com.vadimtoptunov.chaosbank_android.features.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.app.AppServices
import com.vadimtoptunov.chaosbank_android.core.backend.BackendError
import com.vadimtoptunov.chaosbank_android.core.backend.BackendException
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.AmountParser
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.core.money.Money
import com.vadimtoptunov.chaosbank_android.core.money.MoneyFormat
import com.vadimtoptunov.chaosbank_android.core.money.roundedMoney
import com.vadimtoptunov.chaosbank_android.models.Order
import com.vadimtoptunov.chaosbank_android.models.OrderRequest
import com.vadimtoptunov.chaosbank_android.models.OrderSide
import com.vadimtoptunov.chaosbank_android.models.OrderStatus
import com.vadimtoptunov.chaosbank_android.models.OrderType
import java.math.BigDecimal
import java.util.UUID

class OrderViewModel(request: OrderRequest, private val services: AppServices) {
    val symbol: String = request.symbol
    var side by mutableStateOf(request.side)
    var type by mutableStateOf(OrderType.market)
    var quantity by mutableStateOf(if (Defects.isActive(DefectId.orderQtyDefaultsZero)) BigDecimal.ZERO else BigDecimal.ONE)
    var limitPriceText by mutableStateOf(MoneyFormat.price(request.capturedPrice))
    var showConfirm by mutableStateOf(false)
    var isSubmitting by mutableStateOf(false)
    var placed by mutableStateOf(false)
    var status by mutableStateOf<OrderStatus?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    private val capturedPrice: BigDecimal = request.capturedPrice
    private val currency = Currency.USD
    private var heldQuantity: BigDecimal = BigDecimal.ZERO

    suspend fun load() {
        heldQuantity = services.backend.fetchHoldings().firstOrNull { it.symbol == symbol }?.quantity ?: BigDecimal.ZERO
    }

    private fun active(id: DefectId) = Defects.isActive(id)

    /** `livePriceRace`: re-reads the live feed instead of the captured price. */
    val referencePrice: BigDecimal
        get() = if (active(DefectId.livePriceRace)) services.market.price(symbol) else capturedPrice

    val limitPrice: BigDecimal? get() = AmountParser.parse(limitPriceText)

    val executionPrice: BigDecimal
        get() = when (type) {
            OrderType.market -> referencePrice
            // `limitExecutesAtMarket`: a limit order fills at the market price.
            OrderType.limit -> if (active(DefectId.limitExecutesAtMarket)) referencePrice else (limitPrice ?: referencePrice)
        }

    val estTotal: Money
        get() {
            // `estTotalIgnoresQty`: the total drops the quantity factor.
            val qty = if (active(DefectId.estTotalIgnoresQty)) BigDecimal.ONE else quantity
            // `roundingDrift`: routes the multiply through Double, so the shown total drifts.
            return if (active(DefectId.roundingDrift)) Money(BigDecimal(qty.toDouble() * executionPrice.toDouble()), currency)
            else Money((qty * executionPrice).roundedMoney(), currency)
        }

    val limitBelowMarket: Boolean
        get() = type == OrderType.limit && side == OrderSide.sell && (limitPrice ?: BigDecimal.ZERO) < referencePrice

    /** `limitValidation`: suppresses the below-market warning. */
    val showWarning: Boolean get() = if (active(DefectId.limitValidation)) false else limitBelowMarket

    val isValid: Boolean
        get() {
            if (active(DefectId.limitValidation)) return true
            if (quantity.signum() <= 0) return false
            if (type == OrderType.limit) {
                val lp = limitPrice ?: return false
                if (lp.signum() <= 0) return false
            }
            // `sellWithoutHoldingReviewable`: lets a sell be reviewed with no position.
            if (side == OrderSide.sell && quantity > heldQuantity && !active(DefectId.sellWithoutHoldingReviewable)) return false
            return true
        }

    // `qtyIncrementByTwo`: the stepper jumps by two.
    fun increment() { quantity += if (active(DefectId.qtyIncrementByTwo)) BigDecimal(2) else BigDecimal.ONE }

    fun decrement() {
        // `limitValidation`: decrement can go to zero / negative.
        quantity = if (active(DefectId.limitValidation)) quantity - BigDecimal.ONE
        else (quantity - BigDecimal.ONE).max(BigDecimal.ZERO)
    }

    suspend fun place() {
        if (!isValid) return
        // `orderDoubleSubmit`: drops the in-flight guard, so a double-tap places two.
        if (!active(DefectId.orderDoubleSubmit) && isSubmitting) return
        isSubmitting = true
        try {
            // `buySellSwapped`: the placed order uses the opposite side.
            val placedSide = if (active(DefectId.buySellSwapped)) (if (side == OrderSide.buy) OrderSide.sell else OrderSide.buy) else side
            val order = Order(
                id = UUID.randomUUID().toString(), symbol = symbol, side = placedSide, type = type,
                quantity = quantity, limitPrice = if (type == OrderType.limit) limitPrice else null,
                referencePrice = referencePrice, executionPrice = executionPrice,
                status = OrderStatus.pending, placedAt = System.currentTimeMillis() / 1000,
            )
            val filled = services.backend.placeOrder(order)
            // `orderStuckPending`: filled, but the UI keeps reporting pending.
            status = if (active(DefectId.orderStuckPending)) OrderStatus.pending else filled.status
            services.bumpData()
            placed = true
        } catch (e: BackendException) {
            status = OrderStatus.rejected
            errorMessage = when (e.error) {
                BackendError.insufficientFunds -> "Insufficient funds"
                BackendError.insufficientHolding -> "Not enough to sell"
                else -> "Order rejected"
            }
        } finally {
            isSubmitting = false
        }
    }
}
