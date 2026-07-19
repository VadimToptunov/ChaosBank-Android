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
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.vadimtoptunov.chaosbank_android.app.LocalNavigator
import com.vadimtoptunov.chaosbank_android.app.Navigator
import com.vadimtoptunov.chaosbank_android.app.Route
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.features.asset.AssetDetailScreen
import com.vadimtoptunov.chaosbank_android.features.card.CardScreen
import com.vadimtoptunov.chaosbank_android.features.dev.DevMenuScreen
import com.vadimtoptunov.chaosbank_android.features.exchange.ExchangeScreen
import com.vadimtoptunov.chaosbank_android.features.home.AddMoneyScreen
import com.vadimtoptunov.chaosbank_android.features.home.HomeScreen
import com.vadimtoptunov.chaosbank_android.features.markets.MarketsScreen
import com.vadimtoptunov.chaosbank_android.features.order.OrderScreen
import com.vadimtoptunov.chaosbank_android.features.portfolio.PortfolioScreen
import com.vadimtoptunov.chaosbank_android.features.transactions.TransactionsScreen
import com.vadimtoptunov.chaosbank_android.features.transfer.TransferScreen
import com.vadimtoptunov.chaosbank_android.ui.auth.AuthContainer
import com.vadimtoptunov.chaosbank_android.ui.components.BuildBadge
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

@Composable
fun RootScreen(auth: AuthFlow, options: LaunchOptions, inactive: State<Boolean>, pendingRoute: Route? = null) {
    val services = LocalAppServices.current
    val nav = remember { Navigator() }
    var showDev by remember { mutableStateOf(options.showDevMenu) }
    // Push a deep-link's target screen once, after the auth gate is cleared.
    var deepLinkConsumed by remember { mutableStateOf(false) }
    LaunchedEffect(auth.isUnlocked) {
        if (auth.isUnlocked && !deepLinkConsumed && pendingRoute != null) {
            deepLinkConsumed = true
            nav.push(pendingRoute)
        }
    }
    CompositionLocalProvider(LocalNavigator provides nav, LocalDevMenu provides { showDev = true }) {
        Box(Modifier.fillMaxSize().background(Palette.bg)) {
            if (auth.isUnlocked) {
                key(services.configVersion) { TabScaffold(options) }
                nav.current?.let { route -> PushedHost(route) { nav.pop() } }
            } else {
                AuthContainer(auth, options)
            }
            // Obscure sensitive content while inactive; `noPrivacyBlur` suppresses it.
            if (inactive.value && auth.isUnlocked && !Defects.isActive(DefectId.noPrivacyBlur)) {
                PrivacyCover()
            }
            if (showDev) DevMenuScreen(onClose = { showDev = false })
        }
    }
}

@Composable
private fun PushedHost(route: Route, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(Palette.bg)) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clickable { onBack() }.testTag(A11y.Nav.back),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Palette.text)
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (route) {
                is Route.AssetDetail -> AssetDetailScreen(route.symbol)
                is Route.OrderTicket -> OrderScreen(route.request)
                Route.Transfer -> TransferScreen()
                Route.Exchange -> ExchangeScreen()
                Route.AddMoney -> AddMoneyScreen()
                Route.Transactions -> TransactionsScreen()
            }
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
        Box(Modifier.fillMaxSize().padding(padding)) {
            val nav = LocalNavigator.current
            when (selected) {
                0 -> HomeScreen(
                    onTransfer = { nav.push(Route.Transfer) },
                    onExchange = { nav.push(Route.Exchange) },
                    onAddMoney = { nav.push(Route.AddMoney) },
                    onCard = { selected = 3 },
                    onSeeAll = { nav.push(Route.Transactions) },
                )
                1 -> MarketsScreen()
                2 -> PortfolioScreen()
                3 -> CardScreen()
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { TabPlaceholder(tabs[selected].title) }
            }
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
