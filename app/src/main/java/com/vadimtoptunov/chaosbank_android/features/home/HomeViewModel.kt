package com.vadimtoptunov.chaosbank_android.features.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.app.AppServices
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.core.money.FxRates
import com.vadimtoptunov.chaosbank_android.core.money.Money
import com.vadimtoptunov.chaosbank_android.core.money.MoneyFormat
import com.vadimtoptunov.chaosbank_android.core.money.roundedMoney
import com.vadimtoptunov.chaosbank_android.models.Account
import com.vadimtoptunov.chaosbank_android.models.Transaction
import java.math.BigDecimal
import java.math.RoundingMode

class HomeViewModel(private val services: AppServices) {
    var accounts by mutableStateOf<List<Account>>(emptyList()); private set
    var recent by mutableStateOf<List<Transaction>>(emptyList()); private set
    var selectedCurrency by mutableStateOf(Currency.EUR)

    private var loadToken = 0

    suspend fun load() {
        loadToken += 1
        val token = loadToken
        val fetchedAccounts = services.backend.fetchAccounts()
        val all = services.backend.fetchTransactions()
        // `homeRefreshRace`: a stale in-flight load may clobber a newer one.
        if (token < loadToken && !active(DefectId.homeRefreshRace)) return
        accounts = fetchedAccounts
        recent = all.take(if (active(DefectId.recentActivityShowsTwo)) 2 else 4)
    }

    suspend fun refreshAfterMutation() {
        if (active(DefectId.staleBalance)) return
        load()
    }

    val totalBalance: Money
        get() {
            val src = if (active(DefectId.homeTotalOmitsAccount)) accounts.filter { it.currency != Currency.GBP } else accounts
            var sum = src.fold(BigDecimal.ZERO) { acc, a -> acc + FxRates.convert(a.balance, a.currency, selectedCurrency) }
            if (active(DefectId.balanceFloorRounded)) sum = sum.setScale(0, RoundingMode.DOWN)
            return Money(sum, selectedCurrency)
        }

    val totalBalanceText: String
        get() = if (active(DefectId.balanceWrongCurrencySymbol)) "€" + MoneyFormat.decimal(totalBalance.amount.roundedMoney())
        else totalBalance.formatted

    val todayChange: Money
        get() {
            val base = totalBalance.amount * BigDecimal("0.004")
            val signed = if (active(DefectId.todayChangeSignFlipped)) base.negate() else base
            return Money(signed, selectedCurrency)
        }

    val todayChangePercent: BigDecimal
        get() {
            val base = BigDecimal("0.40")
            return if (active(DefectId.todayChangeSignFlipped)) base.negate() else base
        }

    private fun active(id: DefectId) = Defects.isActive(id)
}
