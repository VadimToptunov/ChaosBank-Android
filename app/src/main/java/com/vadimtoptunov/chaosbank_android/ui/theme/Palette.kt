package com.vadimtoptunov.chaosbank_android.ui.theme

import androidx.compose.ui.graphics.Color
import com.vadimtoptunov.chaosbank_android.models.TickDirection
import java.math.BigDecimal

/** Design tokens from the spec. Green/red are reserved strictly for gain/loss. */
object Palette {
    val bg = Color(0xFF0E1218)       // deep blue-black background
    val surface = Color(0xFF171C25)  // cards
    val surface2 = Color(0xFF1F2733) // elevated
    val line = Color(0xFF262F3D)     // hairline borders
    val sand = Color(0xFFE9B45E)     // brand / primary actions
    val gain = Color(0xFF34D399)     // gains only
    val loss = Color(0xFFF87171)     // losses only
    val text = Color(0xFFF4F1EA)     // warm off-white
    val muted = Color(0xFF8B95A6)    // secondary text

    /** Sign-aware gain/loss color. Zero is neutral (muted). */
    fun pnl(value: BigDecimal): Color = when {
        value.signum() > 0 -> gain
        value.signum() < 0 -> loss
        else -> muted
    }

    fun tick(direction: TickDirection): Color = when (direction) {
        TickDirection.up -> gain
        TickDirection.down -> loss
        TickDirection.flat -> muted
    }
}
