package com.vadimtoptunov.chaosbank_android.features.transfer

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
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class TransferViewModel(private val services: AppServices) {
    var recipient by mutableStateOf("")
    var amountText by mutableStateOf("")
    var note by mutableStateOf("")
    var showConfirm by mutableStateOf(false)
    var isSubmitting by mutableStateOf(false)
    var succeeded by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var canRetry by mutableStateOf(false)

    val fromCurrency = Currency.EUR
    var fromBalance by mutableStateOf(BigDecimal.ZERO); private set

    private val idempotencyKey = UUID.randomUUID().toString()

    suspend fun load() {
        fromBalance = services.backend.fetchAccount(fromCurrency)?.balance ?: BigDecimal.ZERO
    }

    private fun active(id: DefectId) = Defects.isActive(id)

    val parsedAmount: BigDecimal? get() = AmountParser.parse(amountText)
    val amount: BigDecimal? get() = parsedAmount?.takeIf { it.signum() > 0 }

    val balanceAfter: Money?
        get() = (amount ?: parsedAmount)?.let {
            // `balanceAfterAdds`: the preview adds instead of subtracting.
            Money(if (active(DefectId.balanceAfterAdds)) fromBalance + it else fromBalance - it, fromCurrency)
        }

    // `transferConfirmWrongRecipient`: shows a different name than entered.
    val confirmRecipientText: String
        get() = if (active(DefectId.transferConfirmWrongRecipient)) "ACME Holdings Ltd" else effectiveRecipient

    private val recipientValid: Boolean
        get() = if (active(DefectId.whitespaceRecipient)) recipient.isNotEmpty() else recipient.trim().isNotEmpty()

    val effectiveRecipient: String
        get() = if (active(DefectId.whitespaceRecipient)) recipient else recipient.trim()

    val canContinue: Boolean
        get() {
            if (!recipientValid) return false
            val a = parsedAmount ?: return false
            // `amountExceedsBalanceAllowed`: skips the client-side balance check.
            if (a > fromBalance && !active(DefectId.amountExceedsBalanceAllowed)) return false
            // `transferNegativeCredits`: a negative amount is accepted.
            if (a.signum() < 0) return active(DefectId.transferNegativeCredits)
            // `zeroAmountAccepted`: allows a zero amount.
            return if (active(DefectId.zeroAmountAccepted)) a.signum() >= 0 else a.signum() > 0
        }

    suspend fun confirmTransfer() {
        val parsed = parsedAmount ?: return
        // `doubleCharge`: removes the in-flight guard, so two taps send twice.
        if (!active(DefectId.doubleCharge) && isSubmitting) return
        isSubmitting = true
        errorMessage = null
        try {
            // `transferRoundsUp`: round the amount up to the next whole unit.
            val amount = if (active(DefectId.transferRoundsUp)) parsed.setScale(0, RoundingMode.UP) else parsed
            // `transferDebitsWrongAccount`: debit USD instead of the chosen account.
            val debitFrom = if (active(DefectId.transferDebitsWrongAccount)) Currency.USD else fromCurrency
            // `doubleCharge`: mints a fresh key per tap, defeating backend dedupe.
            val key = if (active(DefectId.doubleCharge)) UUID.randomUUID().toString() else idempotencyKey
            services.backend.transfer(debitFrom, amount, effectiveRecipient, note, key)
            services.bumpData()
            succeeded = true
            canRetry = false
        } catch (e: BackendException) {
            when (e.error) {
                BackendError.timeout -> {
                    services.bumpData()
                    // `timeoutAsSuccess`: report success despite no confirmation.
                    if (active(DefectId.timeoutAsSuccess)) succeeded = true
                    else { errorMessage = "Request timed out — you can retry safely."; canRetry = true }
                }
                BackendError.insufficientFunds -> errorMessage = "Insufficient funds"
                else -> errorMessage = "Transfer failed"
            }
        } finally {
            isSubmitting = false
        }
    }

    suspend fun retry() = confirmTransfer()
}
