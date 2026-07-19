package com.vadimtoptunov.chaosbank_android.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects

/**
 * Layout-direction settings (Localization cluster). `rtl` mirrors the whole app; the
 * `rtlBreaksLayout` defect forces a specific row to stay left-to-right so it does not
 * mirror — the classic "hard-coded left/right instead of start/end" bug.
 */
class LocaleSettings {
    var rtl by mutableStateOf(false); private set

    fun enableRtl(value: Boolean) { rtl = value }

    companion object {
        /**
         * Whether a row should be (incorrectly) forced left-to-right: only when the
         * app is RTL and the defect is active. Pure — unit-tested.
         */
        fun forcesLtrRow(rtl: Boolean): Boolean = rtl && Defects.isActive(DefectId.rtlBreaksLayout)
    }
}
