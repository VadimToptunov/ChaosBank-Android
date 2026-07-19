package com.vadimtoptunov.chaosbank_android.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.core.money.Money
import com.vadimtoptunov.chaosbank_android.core.money.MoneyFormat
import com.vadimtoptunov.chaosbank_android.models.Account
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.components.SectionHeader
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentBar
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentItem
import com.vadimtoptunov.chaosbank_android.ui.components.TransactionRow
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

@Composable
fun HomeScreen(
    onTransfer: () -> Unit = {},
    onExchange: () -> Unit = {},
    onAddMoney: () -> Unit = {},
    onCard: () -> Unit = {},
    onSeeAll: () -> Unit = {},
) {
    val services = LocalAppServices.current
    val nav = com.vadimtoptunov.chaosbank_android.app.LocalNavigator.current
    val vm = remember { HomeViewModel(services) }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(services.dataVersion) { vm.refreshAfterMutation() }

    ChaosScreen("Home", A11y.Home.root) {
        // Notifications bell + unread badge (Platform cluster).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clickable { nav.push(com.vadimtoptunov.chaosbank_android.app.Route.Notifications) }.testTag(A11y.Notifications.bell),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("🔔", fontSize = 18.sp)
                if (services.notifications.unreadCount > 0) {
                    Text(
                        services.notifications.unreadCount.toString(), color = Palette.bg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).background(Palette.loss).padding(horizontal = 7.dp, vertical = 2.dp).testTag(A11y.Notifications.badge),
                    )
                }
            }
        }

        // Balance card
        CardSurface {
            SegmentBar(
                items = Currency.entries.map { SegmentItem(it.code, it.code, "${A11y.Home.currencySegment}.${it.code}") },
                selected = vm.selectedCurrency.code,
                modifier = Modifier.testTag(A11y.Home.currencySegment),
            ) { vm.selectedCurrency = Currency.valueOf(it) }
            Text("Total balance", color = Palette.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp))
            Text(vm.totalBalanceText, color = Palette.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.testTag(A11y.Home.totalBalance))
            Text(
                "${vm.todayChange.formattedSigned} · ${MoneyFormat.percent(vm.todayChangePercent)} today",
                color = Palette.pnl(vm.todayChange.amount), fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp).testTag(A11y.Home.todayChange),
            )
        }

        // Account strip. `accountStripHidesGBP` drops the GBP card.
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            vm.accounts.filter { !(Defects.isActive(DefectId.accountStripHidesGBP) && it.currency == Currency.GBP) }
                .forEach { account -> AccountCard(account, vm.selectedCurrency) { vm.selectedCurrency = account.currency } }
        }

        // Quick actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("Transfer", "⇄", A11y.Home.quickActionTransfer, Modifier.weight(1f)) {
                if (Defects.isActive(DefectId.quickActionTransferOpensExchange)) onExchange() else onTransfer()
            }
            QuickAction("Exchange", "⇆", A11y.Home.quickActionExchange, Modifier.weight(1f), onExchange)
            QuickAction("Add", "＋", A11y.Home.quickActionAddMoney, Modifier.weight(1f), onAddMoney)
            QuickAction("Card", "▭", A11y.Home.quickActionCard, Modifier.weight(1f), onCard)
        }

        // Recent activity
        SectionHeader("Recent activity", trailing = "See all", modifier = Modifier.clickable { onSeeAll() }.testTag(A11y.Home.seeAllActivity))
        CardSurface(padding = 8.dp, modifier = Modifier.testTag(A11y.Home.recentActivity)) {
            vm.recent.forEachIndexed { i, tx ->
                TransactionRow(tx, A11y.Home.activityRow(tx.id))
                if (i < vm.recent.size - 1) HorizontalDivider(color = Palette.line)
            }
        }
    }
}

@Composable
private fun AccountCard(account: Account, selected: Currency, onClick: () -> Unit) {
    Column(
        Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.surface)
            .border(if (account.currency == selected) 1.5.dp else 1.dp, if (account.currency == selected) Palette.sand else Palette.line, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag(A11y.Home.account(account.currency)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(account.currency.symbol, color = Palette.sand, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(account.name, color = Palette.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text(Money(account.balance, account.currency).formatted, color = Palette.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun QuickAction(title: String, glyph: String, tag: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable { onClick() }.testTag(tag), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(Palette.surface2), contentAlignment = Alignment.Center) {
            Text(glyph, color = Palette.sand, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(title, color = Palette.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
    }
}
