package com.vadimtoptunov.chaosbank_android.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.models.TickDirection
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

/**
 * The signature element: a price that briefly flashes green/up or red/down on each
 * tick, then settles back to the neutral text colour.
 */
@Composable
fun LiveTickerText(
    text: String,
    direction: TickDirection,
    tag: String,
    modifier: Modifier = Modifier,
    size: Int = 16,
    weight: FontWeight = FontWeight.SemiBold,
) {
    val color = remember { Animatable(Palette.text) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(text) {
        if (first) { first = false; return@LaunchedEffect }
        color.snapTo(Palette.tick(direction))
        color.animateTo(Palette.text, animationSpec = tween(600))
    }
    Text(
        text,
        color = color.value,
        fontSize = size.sp,
        fontWeight = weight,
        fontFamily = FontFamily.Monospace,
        modifier = modifier.testTag(tag),
    )
}
