package com.vadimtoptunov.chaosbank_android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

/** A labelled single-line text field used across the bank forms. */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
    placeholder: String = "",
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Palette.muted, fontSize = 13.sp)
        Box2(value, placeholder) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Palette.text, fontSize = 16.sp),
                cursorBrush = SolidColor(Palette.sand),
                modifier = Modifier.fillMaxWidth().testTag(tag),
            )
        }
    }
}

/** A large currency-symbol + amount entry row. */
@Composable
fun AmountInput(symbol: String, value: String, onValueChange: (String) -> Unit, tag: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, color = Palette.sand, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Box2(value, "0.00") {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(Palette.sand),
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp).testTag(tag),
            )
        }
    }
}

/** Renders a placeholder behind an empty field. */
@Composable
private fun Box2(value: String, placeholder: String, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(placeholder, color = Palette.muted, fontSize = 16.sp, modifier = Modifier.padding(start = 6.dp))
        }
        content()
    }
}
