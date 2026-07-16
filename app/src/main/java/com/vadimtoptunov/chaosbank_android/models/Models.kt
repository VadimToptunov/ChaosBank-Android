package com.vadimtoptunov.chaosbank_android.models

import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.core.money.Money
import java.math.BigDecimal
import java.math.RoundingMode

data class Account(val name: String, val currency: Currency, val balance: BigDecimal) {
    val id: Currency get() = currency
}

enum class TransactionDirection { moneyIn, moneyOut }

data class Transaction(
    val id: String,
    val title: String,
    val category: String,
    /** Epoch seconds. */
    val date: Long,
    /** Signed: negative = money out, positive = money in. */
    val amount: BigDecimal,
    val currency: Currency,
) {
    val direction: TransactionDirection
        get() = if (amount.signum() < 0) TransactionDirection.moneyOut else TransactionDirection.moneyIn

    val money: Money get() = Money(amount, currency)
}

enum class AssetKind { stock, crypto }

data class Asset(
    val symbol: String,
    val name: String,
    val kind: AssetKind,
    val currency: Currency,
    val basePrice: BigDecimal,
    val volatility: BigDecimal,
) {
    val id: String get() = symbol
}

enum class TickDirection { up, down, flat }

data class Quote(
    val symbol: String,
    val price: BigDecimal,
    val dayOpen: BigDecimal,
    val dayHigh: BigDecimal,
    val dayLow: BigDecimal,
    val lastDirection: TickDirection,
) {
    val changePct: BigDecimal
        get() = if (dayOpen.signum() == 0) BigDecimal.ZERO
        else (price - dayOpen).divide(dayOpen, 6, RoundingMode.HALF_EVEN) * BigDecimal(100)

    val changeAbsolute: BigDecimal get() = price - dayOpen
}

data class Holding(val symbol: String, val quantity: BigDecimal, val avgCost: BigDecimal) {
    val id: String get() = symbol
    val costBasis: BigDecimal get() = quantity * avgCost
    fun marketValue(price: BigDecimal): BigDecimal = quantity * price
    fun pnl(price: BigDecimal): BigDecimal = (price - avgCost) * quantity
    fun pnlPercent(price: BigDecimal): BigDecimal =
        if (costBasis.signum() == 0) BigDecimal.ZERO
        else pnl(price).divide(costBasis, 6, RoundingMode.HALF_EVEN) * BigDecimal(100)
}

enum class OrderSide { buy, sell }
enum class OrderType { market, limit }
enum class OrderStatus { pending, filled, rejected }

data class Order(
    val id: String,
    val symbol: String,
    val side: OrderSide,
    val type: OrderType,
    val quantity: BigDecimal,
    val limitPrice: BigDecimal?,
    val referencePrice: BigDecimal,
    val executionPrice: BigDecimal,
    val status: OrderStatus,
    val placedAt: Long,
) {
    val estimatedTotal: BigDecimal get() = quantity * executionPrice
}

data class OrderRequest(val symbol: String, val side: OrderSide, val capturedPrice: BigDecimal) {
    val id: String get() = "$symbol.${side.name}.${capturedPrice.toPlainString()}"
}
