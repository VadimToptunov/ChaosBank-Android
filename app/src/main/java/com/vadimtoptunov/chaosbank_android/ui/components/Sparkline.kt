package com.vadimtoptunov.chaosbank_android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vadimtoptunov.chaosbank_android.core.SeededRng
import com.vadimtoptunov.chaosbank_android.core.StableHash
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

/**
 * A tiny deterministic price sparkline. Shape is seeded off the symbol so it is
 * stable across launches; coloured green/red by net direction.
 */
@Composable
fun Sparkline(symbol: String, up: Boolean, modifier: Modifier = Modifier, pointCount: Int = 24) {
    val points = remember(symbol, pointCount) {
        val rng = SeededRng(StableHash.of(symbol).toLong())
        var value = 0.5
        List(pointCount) {
            value = (value + rng.nextInRange(-0.12, 0.12)).coerceIn(0.05, 0.95)
            value
        }
    }
    val color = if (up) Palette.gain else Palette.loss
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val stepX = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height * (1f - v.toFloat())
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
