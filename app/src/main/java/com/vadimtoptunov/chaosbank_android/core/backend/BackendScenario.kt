package com.vadimtoptunov.chaosbank_android.core.backend

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId

/** Network scenario knobs for the mock backend, resolved from the active defect set. */
data class BackendScenario(
    val retryDuplicate: Boolean = false,
    val timeoutAsSuccess: Boolean = false,
    val slowResponseRace: Boolean = false,
    val staleOfflineBalance: Boolean = false,
    val balanceReadReturnsZero: Boolean = false,
    val transactionsDupOnFetch: Boolean = false,
    val staleHoldingsAfterOrder: Boolean = false,
) {
    companion object {
        fun from(defects: Set<DefectId>) = BackendScenario(
            retryDuplicate = DefectId.retryDuplicate in defects,
            timeoutAsSuccess = DefectId.timeoutAsSuccess in defects,
            slowResponseRace = DefectId.slowResponseRace in defects,
            staleOfflineBalance = DefectId.staleOfflineBalance in defects,
            balanceReadReturnsZero = DefectId.balanceReadReturnsZero in defects,
            transactionsDupOnFetch = DefectId.transactionsDupOnFetch in defects,
            staleHoldingsAfterOrder = DefectId.staleHoldingsAfterOrder in defects,
        )
    }
}

enum class BackendError { insufficientFunds, unknownAccount, invalidAmount, unknownAsset, insufficientHolding, timeout, offline }

class BackendException(val error: BackendError) : Exception(error.name)
