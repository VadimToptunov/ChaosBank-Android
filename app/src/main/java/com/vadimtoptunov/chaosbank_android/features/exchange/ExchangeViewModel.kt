package com.vadimtoptunov.chaosbank_android.features.exchange

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
import com.vadimtoptunov.chaosbank_android.core.money.FxRates
import com.vadimtoptunov.chaosbank_android.core.money.Money
import com.vadimtoptunov.chaosbank_android.core.money.roundedMoney
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ExchangeViewModel(private val services: AppServices) {
    var sell by mutableStateOf(Currency.EUR)
    var get by mutableStateOf(Currency.USD)
    var amountText by mutableStateOf("")
    var isSubmitting by mutableStateOf(false)
    var succeeded by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var sellBalance by mutableStateOf(BigDecimal.ZERO); private set

    private val scope = MainScope()
    private var requestToken = 0
    private val cachedInitialRate = FxRates.rate(Currency.EUR, Currency.USD)

    private fun active(id: DefectId) = Defects.isActive(id)

    suspend fun load() {
        requestToken += 1
        val token = requestToken
        applyBalance(services.backend.fetchAccount(sell)?.balance ?: BigDecimal.ZERO, token)
    }

    /**
     * `slowResponseRace`: a late stale response for the previous currency clobbers
     * the fresh balance (the token guard that would drop it is skipped).
     */
    suspend fun refreshBalance(previous: Currency? = null) {
        requestToken += 1
        val token = requestToken
        if (previous != null && previous != sell) {
            val staleToken = token - 1
            scope.launch {
                val stale = services.backend.fetchAccount(previous, extraDelayMs = 300)?.balance ?: BigDecimal.ZERO
                applyBalance(stale, staleToken)
            }
        }
        applyBalance(services.backend.fetchAccount(sell)?.balance ?: BigDecimal.ZERO, token)
    }

    private fun applyBalance(value: BigDecimal, token: Int) {
        if (!active(DefectId.slowResponseRace) && token < requestToken) return
        sellBalance = value
    }

    val amount: BigDecimal? get() = AmountParser.parse(amountText)?.takeIf { it.signum() > 0 }

    val rate: BigDecimal
        get() = when {
            // `exchangeInverseRate`: apply the wrong-direction rate.
            active(DefectId.exchangeInverseRate) -> FxRates.rate(get, sell)
            // `exchangeRateStaleAfterSwap`: keep the original EUR→USD rate after a swap.
            active(DefectId.exchangeRateStaleAfterSwap) -> cachedInitialRate
            else -> FxRates.rate(sell, get)
        }

    val fee: Money get() = Money((amount ?: BigDecimal.ZERO) * FxRates.feeRate, sell).rounded

    val youGet: Money
        get() {
            val a = amount ?: return Money.zero(get)
            // `youGetShowsGross`: display the pre-fee amount while the credit is net.
            val base = if (active(DefectId.youGetShowsGross)) a else a - a * FxRates.feeRate
            return Money((base * rate).roundedMoney(), get)
        }

    val canExecute: Boolean
        get() {
            val a = amount ?: return false
            // `exchangeSameCurrencyAllowed`: allow sell == get.
            val currenciesOk = sell != get || active(DefectId.exchangeSameCurrencyAllowed)
            return a.signum() > 0 && a <= sellBalance && currenciesOk
        }

    private fun creditedValue(): BigDecimal {
        val a = amount ?: return BigDecimal.ZERO
        val base = when {
            // `exchangeFeeNotApplied`: credit the gross amount.
            active(DefectId.exchangeFeeNotApplied) -> a
            // `exchangeFeeDoubled`: subtract the fee twice.
            active(DefectId.exchangeFeeDoubled) -> a - a * FxRates.feeRate * BigDecimal(2)
            else -> a - a * FxRates.feeRate
        }
        // `roundingDrift`: route the conversion through Double so the stored value drifts.
        return if (active(DefectId.roundingDrift)) BigDecimal(base.toDouble() * rate.toDouble())
        else (base * rate).roundedMoney()
    }

    suspend fun execute() {
        val a = amount ?: return
        // `exchangeDoubleSubmit`: removes the in-flight guard, so a double-tap exchanges twice.
        if (!active(DefectId.exchangeDoubleSubmit) && isSubmitting) return
        isSubmitting = true
        try {
            // `exchangeCreditsWrongAccount`: credit the sell account instead of get.
            val creditTo = if (active(DefectId.exchangeCreditsWrongAccount)) sell else get
            services.backend.exchange(sell, creditTo, a, creditedValue())
            services.bumpData()
            succeeded = true
            refreshBalance()
        } catch (e: BackendException) {
            errorMessage = if (e.error == BackendError.insufficientFunds) "Insufficient funds" else "Exchange failed"
        } finally {
            isSubmitting = false
        }
    }

    fun swapDirection() {
        val previous = sell
        val tmp = sell; sell = get; get = tmp
        scope.launch { refreshBalance(previous) }
    }

    fun selectSell(currency: Currency) {
        val previous = sell
        sell = currency
        scope.launch { refreshBalance(previous) }
    }

    fun selectGet(currency: Currency) { get = currency }
}
