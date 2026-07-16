package com.vadimtoptunov.chaosbank_android.models

import com.vadimtoptunov.chaosbank_android.core.money.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ModelsTest {
    @Test fun transaction_directionFromSign() {
        assertEquals(TransactionDirection.moneyOut, Transaction("a", "t", "c", 0, BigDecimal("-1"), Currency.EUR).direction)
        assertEquals(TransactionDirection.moneyIn, Transaction("b", "t", "c", 0, BigDecimal("1"), Currency.EUR).direction)
        assertEquals(TransactionDirection.moneyIn, Transaction("c", "t", "c", 0, BigDecimal.ZERO, Currency.EUR).direction)
    }

    @Test fun holding_math() {
        val h = Holding("AAPL", BigDecimal("10"), BigDecimal("100"))
        assertEquals(0, BigDecimal("1000").compareTo(h.costBasis))
        assertEquals(0, BigDecimal("1200").compareTo(h.marketValue(BigDecimal("120"))))
        assertEquals(0, BigDecimal("200").compareTo(h.pnl(BigDecimal("120"))))
        assertEquals(0, BigDecimal("20").compareTo(h.pnlPercent(BigDecimal("120"))))
    }

    @Test fun holding_pnlPercentZeroCost() {
        assertEquals(0, BigDecimal.ZERO.compareTo(Holding("X", BigDecimal.ZERO, BigDecimal.ZERO).pnlPercent(BigDecimal("5"))))
    }

    @Test fun quote_changePct() {
        val q = Quote("AAPL", BigDecimal("110"), BigDecimal("100"), BigDecimal("115"), BigDecimal("95"), TickDirection.up)
        assertEquals(0, BigDecimal("10").compareTo(q.changePct))
        assertEquals(0, BigDecimal("10").compareTo(q.changeAbsolute))
    }

    @Test fun quote_changePctZeroOpen() {
        val q = Quote("X", BigDecimal("110"), BigDecimal.ZERO, BigDecimal("1"), BigDecimal("1"), TickDirection.flat)
        assertEquals(0, BigDecimal.ZERO.compareTo(q.changePct))
    }

    @Test fun orderRequest_idIsStable() {
        val r = OrderRequest("AAPL", OrderSide.buy, BigDecimal("189.50"))
        assertEquals("AAPL.buy.189.50", r.id)
    }

    @Test fun order_estimatedTotal() {
        val o = Order("1", "AAPL", OrderSide.buy, OrderType.market, BigDecimal("2"), null, BigDecimal("100"), BigDecimal("100"), OrderStatus.pending, 0)
        assertEquals(0, BigDecimal("200").compareTo(o.estimatedTotal))
    }

    @Test fun account_idIsCurrency() {
        assertEquals(Currency.EUR, Account("EUR", Currency.EUR, BigDecimal.ONE).id)
    }
}
