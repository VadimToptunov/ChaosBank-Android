package com.vadimtoptunov.chaosbank_android.core.money

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {
    @Test fun roundedMoney_isHalfEven() {
        assertEquals(BigDecimal("1.00"), BigDecimal("1.005").roundedMoney())
        assertEquals(BigDecimal("1.02"), BigDecimal("1.015").roundedMoney())
        assertEquals(BigDecimal("2.68"), BigDecimal("2.675").roundedMoney())
    }

    @Test fun roundedScale_arbitrary() {
        assertEquals(BigDecimal("3.1"), BigDecimal("3.14159").roundedScale(1))
        assertEquals(BigDecimal("3.1416"), BigDecimal("3.14159").roundedScale(4))
    }

    @Test fun formatted_hasSymbolAndGrouping() {
        assertEquals("€1,234.50", Money(BigDecimal("1234.5"), Currency.EUR).formatted)
        assertEquals("$0.00", Money(BigDecimal.ZERO, Currency.USD).formatted)
    }

    @Test fun formattedSigned_prefixesSign() {
        assertEquals("+€10.00", Money(BigDecimal("10"), Currency.EUR).formattedSigned)
        assertEquals("−£5.50", Money(BigDecimal("-5.5"), Currency.GBP).formattedSigned)
        assertEquals("+$0.00", Money(BigDecimal.ZERO, Currency.USD).formattedSigned)
    }

    @Test fun rounded_returnsMoney() {
        assertEquals(BigDecimal("1.02"), Money(BigDecimal("1.015"), Currency.EUR).rounded.amount)
        assertEquals(BigDecimal("1.00"), Money(BigDecimal("1.005"), Currency.EUR).rounded.amount)
    }

    @Test fun zero_factory() {
        assertEquals(BigDecimal.ZERO, Money.zero(Currency.EUR).amount)
        assertEquals(Currency.EUR, Money.zero(Currency.EUR).currency)
    }

    @Test fun moneyFormat_decimalGrouping() {
        assertEquals("1,000,000.00", MoneyFormat.decimal(BigDecimal("1000000")))
        assertEquals("1,000", MoneyFormat.decimal(BigDecimal("1000"), 0))
    }

    @Test fun moneyFormat_price_default2() {
        assertEquals("189.50", MoneyFormat.price(BigDecimal("189.5")))
        // DecimalFormat rounds half-to-even: 189.5 -> 190.
        assertEquals("190", MoneyFormat.price(BigDecimal("189.5"), 0))
        assertEquals("187", MoneyFormat.price(BigDecimal("187.4"), 0))
    }

    @Test fun moneyFormat_percentSigned() {
        assertEquals("+0.40%", MoneyFormat.percent(BigDecimal("0.4")))
        assertEquals("−1.25%", MoneyFormat.percent(BigDecimal("-1.25")))
        assertEquals("+0.00%", MoneyFormat.percent(BigDecimal.ZERO))
    }

    @Test fun currency_symbolsAndCodes() {
        assertEquals("€", Currency.EUR.symbol)
        assertEquals("USD", Currency.USD.code)
        assertEquals("£", Currency.GBP.symbol)
    }
}
