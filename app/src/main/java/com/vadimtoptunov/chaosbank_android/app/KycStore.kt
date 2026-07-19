package com.vadimtoptunov.chaosbank_android.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import java.math.BigDecimal

/** Identity-verification (KYC) state (banking-breadth cluster). Verified by default. */
class KycStore {
    var verified by mutableStateOf(true); private set
    fun applyVerified(value: Boolean) { verified = value }
}

/**
 * Whether a transfer is allowed given KYC status. Transfers above [threshold] require a
 * verified identity. The `kycBypassAllowsTransfer` defect lets an unverified user send
 * a large transfer anyway.
 */
object KycGate {
    val threshold: BigDecimal = BigDecimal("1000")

    fun allowsTransfer(amount: BigDecimal, verified: Boolean): Boolean {
        if (verified) return true
        if (amount <= threshold) return true
        return Defects.isActive(DefectId.kycBypassAllowsTransfer)
    }
}
