package com.vadimtoptunov.chaosbank_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.app.AuthFlow
import com.vadimtoptunov.chaosbank_android.app.LaunchOptions
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.ui.auth.AuthContainer
import com.vadimtoptunov.chaosbank_android.ui.components.BuildBadge
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

@Composable
fun RootScreen(auth: AuthFlow, options: LaunchOptions, inactive: State<Boolean>) {
    val services = LocalAppServices.current
    Box(Modifier.fillMaxSize().background(Palette.bg)) {
        if (auth.isUnlocked) {
            key(services.configVersion) { TabScaffold(options) }
        } else {
            AuthContainer(auth, options)
        }
        // Obscure sensitive content while inactive; `noPrivacyBlur` suppresses it.
        if (inactive.value && auth.isUnlocked && !Defects.isActive(DefectId.noPrivacyBlur)) {
            PrivacyCover()
        }
    }
}

private data class Tab(val title: String, val icon: ImageVector, val tag: String)

@Composable
private fun TabScaffold(options: LaunchOptions) {
    val tabs = listOf(
        Tab("Home", Icons.Filled.Home, A11y.TabBar.home),
        Tab("Markets", Icons.Filled.ShowChart, A11y.TabBar.markets),
        Tab("Portfolio", Icons.Filled.PieChart, A11y.TabBar.portfolio),
        Tab("Card", Icons.Filled.CreditCard, A11y.TabBar.card),
    )
    var selected by rememberSaveable { mutableIntStateOf(options.initialTab) }

    Scaffold(
        containerColor = Palette.bg,
        bottomBar = {
            NavigationBar(containerColor = Palette.surface) {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag(tab.tag),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Palette.sand,
                            selectedTextColor = Palette.sand,
                            unselectedIconColor = Palette.muted,
                            unselectedTextColor = Palette.muted,
                            indicatorColor = Palette.surface2,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            TabPlaceholder(tabs[selected].title)
        }
    }
}

@Composable
private fun TabPlaceholder(title: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = Palette.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("(screen coming in M4)", color = Palette.muted, fontSize = 13.sp)
        BuildBadge()
    }
}

@Composable
private fun PrivacyCover() {
    Box(Modifier.fillMaxSize().background(Palette.bg), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag(A11y.Privacy.cover),
        ) {
            Text("🔒", fontSize = 40.sp)
            Text("ChaosBank", color = Palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}
