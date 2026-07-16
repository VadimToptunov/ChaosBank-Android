package com.vadimtoptunov.chaosbank_android.models

import com.vadimtoptunov.chaosbank_android.core.money.Currency
import java.math.BigDecimal

/** Concrete starting data — real tickers and plausible transactions. Deterministic. */
object SeedData {
    /** Fixed reference "now" (epoch seconds) so grouped-by-date views are deterministic. */
    const val referenceDate: Long = 1_752_000_000L

    fun daysAgo(d: Int, hours: Int = 0): Long = referenceDate - (d.toLong() * 86_400 + hours.toLong() * 3_600)

    val accounts: List<Account> = listOf(
        Account("EUR Main", Currency.EUR, BigDecimal("4820.55")),
        Account("USD Savings", Currency.USD, BigDecimal("12750.00")),
        Account("GBP Travel", Currency.GBP, BigDecimal("640.20")),
    )

    val assets: List<Asset> = listOf(
        Asset("AAPL", "Apple Inc.", AssetKind.stock, Currency.USD, BigDecimal("189.50"), BigDecimal("0.004")),
        Asset("MSFT", "Microsoft Corp.", AssetKind.stock, Currency.USD, BigDecimal("415.20"), BigDecimal("0.0035")),
        Asset("NVDA", "NVIDIA Corp.", AssetKind.stock, Currency.USD, BigDecimal("875.30"), BigDecimal("0.006")),
        Asset("TSLA", "Tesla Inc.", AssetKind.stock, Currency.USD, BigDecimal("242.10"), BigDecimal("0.007")),
        Asset("BTC", "Bitcoin", AssetKind.crypto, Currency.USD, BigDecimal("64200.00"), BigDecimal("0.008")),
        Asset("ETH", "Ethereum", AssetKind.crypto, Currency.USD, BigDecimal("3120.00"), BigDecimal("0.009")),
    )

    val watchlistSymbols: List<String> = listOf("AAPL", "NVDA", "BTC")

    val holdings: List<Holding> = listOf(
        Holding("AAPL", BigDecimal("12"), BigDecimal("150.00")),   // gain
        Holding("TSLA", BigDecimal("8"), BigDecimal("280.00")),    // loss (pnlSign target)
        Holding("ETH", BigDecimal("3.5"), BigDecimal("2400.00")),  // gain
    )

    val transactions: List<Transaction> = listOf(
        tx("t01", "Salary — Acme Corp", "Income", 0, "3200.00", Currency.EUR),
        tx("t02", "Grocery Store", "Groceries", 1, "-64.30", Currency.EUR),
        tx("t03", "Transfer to Alex", "Transfer", 1, "-120.00", Currency.EUR),
        tx("t04", "Coffee Roasters", "Dining", 2, "-4.80", Currency.EUR),
        tx("t05", "Refund — Zalando", "Shopping", 2, "39.99", Currency.EUR),
        tx("t06", "Electricity Bill", "Utilities", 3, "-88.10", Currency.EUR),
        tx("t07", "Exchange EUR → USD", "Exchange", 3, "-500.00", Currency.EUR),
        tx("t08", "Freelance Payout", "Income", 4, "740.00", Currency.USD),
        tx("t09", "App Store", "Digital", 4, "-9.99", Currency.EUR),
        tx("t10", "Restaurant Bella", "Dining", 5, "-52.40", Currency.EUR),
        tx("t11", "Pharmacy", "Health", 6, "-18.75", Currency.EUR),
        tx("t12", "Transfer from Mia", "Transfer", 6, "85.00", Currency.EUR),
        tx("t13", "Gym Membership", "Health", 7, "-29.90", Currency.EUR),
        tx("t14", "Fuel Station", "Transport", 8, "-61.20", Currency.EUR),
        tx("t15", "Book Depository", "Shopping", 9, "-23.45", Currency.EUR),
        tx("t16", "Interest", "Income", 10, "12.06", Currency.USD),
    ).sortedByDescending { it.date }

    private fun tx(id: String, title: String, category: String, daysAgo: Int, amount: String, currency: Currency): Transaction {
        val hour = (id.takeLast(2).toIntOrNull() ?: 0) % 12
        return Transaction(id, title, category, daysAgo(daysAgo, hour), BigDecimal(amount), currency)
    }
}
