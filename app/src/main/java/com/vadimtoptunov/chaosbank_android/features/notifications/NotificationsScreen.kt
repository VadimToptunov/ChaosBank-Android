package com.vadimtoptunov.chaosbank_android.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.app.LocalNavigator
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

@Composable
fun NotificationsScreen() {
    val services = LocalAppServices.current
    val nav = LocalNavigator.current
    val store = services.notifications
    // Opening the centre marks everything read (the badge follows — unless stale).
    LaunchedEffect(Unit) { store.markAllRead() }

    ChaosScreen("Notifications", A11y.Notifications.root, showBadge = false) {
        CardSurface(padding = 8.dp) {
            store.items.forEachIndexed { i, n ->
                Row(
                    Modifier.clickable { nav.push(store.target(n)) }.padding(vertical = 10.dp, horizontal = 8.dp)
                        .testTag(A11y.Notifications.row(n.id)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!n.read) Box3()
                    Column(Modifier.padding(start = if (n.read) 18.dp else 8.dp)) {
                        Text(n.title, color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(n.body, color = Palette.muted, fontSize = 13.sp)
                    }
                }
                if (i < store.items.size - 1) HorizontalDivider(color = Palette.line)
            }
        }
    }
}

@Composable
private fun Box3() {
    androidx.compose.foundation.layout.Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.sand))
}
