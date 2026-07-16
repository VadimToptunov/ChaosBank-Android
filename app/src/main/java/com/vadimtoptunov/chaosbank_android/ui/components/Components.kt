package com.vadimtoptunov.chaosbank_android.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

private val Capsule = RoundedCornerShape(50)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BuildBadge(modifier: Modifier = Modifier) {
    val s = LocalAppServices.current
    val openDev = com.vadimtoptunov.chaosbank_android.ui.LocalDevMenu.current
    Text(
        text = "sandbox · ${s.config.version} · ${s.config.label}",
        color = Palette.muted,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        modifier = modifier
            .clip(Capsule)
            .background(Palette.surface2)
            .border(1.dp, Palette.line, Capsule)
            // Hidden dev menu: long-press the build badge.
            .combinedClickable(onClick = {}, onLongClick = openDev)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag(A11y.Build.badge),
    )
}

@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.surface)
            .border(1.dp, Palette.line, RoundedCornerShape(18.dp))
            .padding(padding),
        content = content,
    )
}

@Composable
fun PrimaryButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    looksDisabled: Boolean = false,
    onClick: () -> Unit,
) {
    val dimmed = !enabled || looksDisabled
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (dimmed) Palette.sand.copy(alpha = 0.35f) else Palette.sand)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, color = Palette.bg, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun SecondaryButton(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.surface2)
            .border(1.dp, Palette.line, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, color = Palette.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

data class SegmentItem(val id: String, val title: String, val tag: String)

@Composable
fun SegmentBar(items: List<SegmentItem>, selected: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Palette.surface2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            val sel = item.id == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) Palette.sand else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(item.id) }
                    .padding(vertical = 9.dp)
                    .testTag(item.tag),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    item.title,
                    color = if (sel) Palette.bg else Palette.muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Palette.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, color = Palette.sand, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StatTile(label: String, value: String, tag: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.surface2)
            .padding(12.dp),
    ) {
        Text(label, color = Palette.muted, fontSize = 12.sp)
        Text(
            value, color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace, modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
fun Toast(message: String, tag: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = Palette.bg,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        modifier = modifier
            .clip(Capsule)
            .background(Palette.gain)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag(tag),
    )
}
