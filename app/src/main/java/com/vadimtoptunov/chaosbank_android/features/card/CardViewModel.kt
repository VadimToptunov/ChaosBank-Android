package com.vadimtoptunov.chaosbank_android.features.card

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects

class CardViewModel {
    private var storedFrozen by mutableStateOf(false)
    private var storedOnlinePayments by mutableStateOf(true)

    private fun active(id: DefectId) = Defects.isActive(id)

    /** `cardToggleInvert`: after freezing, the toggle reads back off. */
    var frozen: Boolean
        get() = if (active(DefectId.cardToggleInvert)) !storedFrozen else storedFrozen
        set(value) { storedFrozen = value }

    /** `onlinePaymentsInverted`: the toggle reads back inverted. */
    var onlinePayments: Boolean
        get() = if (active(DefectId.onlinePaymentsInverted)) !storedOnlinePayments else storedOnlinePayments
        set(value) { storedOnlinePayments = value }

    var monthlyLimitText by mutableStateOf("2000")

    val holder = "V. TOPTUNOV"
    private val panSuffix = "4291"
    private val fullPAN = "4916 2043 1188 4291"
    private val cvv = "829"
    private val pin = "4821"

    // `cardExpiryInPast`: the displayed expiry is already in the past.
    val expiry: String get() = if (active(DefectId.cardExpiryInPast)) "08/20" else "08/29"

    // `pinShownPlaintext`: the PIN is shown in the clear instead of masked.
    val pinText: String get() = if (active(DefectId.pinShownPlaintext)) pin else "••••"

    // `cardLimitAcceptsZero`: a zero limit is accepted without an error.
    val limitError: String?
        get() {
            val value = monthlyLimitText.toIntOrNull() ?: -1
            return if (value <= 0 && !active(DefectId.cardLimitAcceptsZero)) "Monthly limit must be greater than zero" else null
        }

    // `cardNumberFullyVisible`: shows the full PAN instead of masking.
    val displayedPAN: String get() = if (active(DefectId.cardNumberFullyVisible)) fullPAN else "•••• •••• •••• $panSuffix"

    // `cardCvvVisible`: prints the CVV on the card face.
    val visibleCVV: String? get() = if (active(DefectId.cardCvvVisible)) cvv else null

    private val virtualPan = "4000 1234 5678 9010"

    // `virtualCardShowsRealPan`: the virtual card leaks the real PAN instead of a distinct number.
    val virtualCardNumber: String get() = if (active(DefectId.virtualCardShowsRealPan)) fullPAN else virtualPan
}
