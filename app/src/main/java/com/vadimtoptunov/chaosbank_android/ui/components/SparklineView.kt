package com.vadimtoptunov.chaosbank_android.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.compose.ui.graphics.toArgb
import com.vadimtoptunov.chaosbank_android.core.SeededRng
import com.vadimtoptunov.chaosbank_android.core.StableHash
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

/**
 * The View-system twin of the Compose [Sparkline]: same deterministic shape
 * (seeded off the symbol), same green/red-by-direction colour. Used by the views
 * build on Markets rows and the asset detail so both builds draw identical charts.
 */
class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    var symbol: String = ""
        set(value) { field = value; invalidate() }
    var up: Boolean = true
        set(value) { field = value; invalidate() }
    /** `sparklineHeavyPoints` drives this to an absurd count; callers pass the same value. */
    var pointCount: Int = 24
        set(value) { field = value; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    private fun samples(): List<Double> {
        val rng = SeededRng(StableHash.of(symbol).toLong())
        var value = 0.5
        return List(pointCount) {
            value = (value + rng.nextInRange(-0.12, 0.12)).coerceIn(0.05, 0.95)
            value
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (symbol.isEmpty() || width == 0 || height == 0) return
        val pts = samples()
        if (pts.size < 2) return
        val stepX = width.toFloat() / (pts.size - 1)
        path.reset()
        pts.forEachIndexed { i, v ->
            val x = i * stepX
            val y = height * (1f - v.toFloat())
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        paint.color = (if (up) Palette.gain else Palette.loss).toArgb()
        canvas.drawPath(path, paint)
    }
}
