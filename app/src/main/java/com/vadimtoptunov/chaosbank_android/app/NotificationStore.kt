package com.vadimtoptunov.chaosbank_android.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects

/** A local push-style notification pointing at an in-app destination. */
data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val route: Route,
    val read: Boolean = false,
)

/**
 * In-app notification centre (Platform cluster). Real APNs/FCM delivery is a
 * platform-config step; this models the in-app surface, unread badge and routing —
 * where the `notificationBadgeStale` and `notificationOpensWrongScreen` bugs live.
 */
class NotificationStore {
    private val seed = listOf(
        AppNotification("n1", "Transfer received", "€85.00 from Mia", Route.Transactions),
        AppNotification("n2", "FX rate alert", "EUR/USD moved 0.4% today", Route.Exchange),
        AppNotification("n3", "Add money reminder", "Top up before the weekend", Route.AddMoney),
        AppNotification("n4", "Statement ready", "June statement is available", Route.Transactions, read = true),
    )
    private val initialUnread = seed.count { !it.read }

    var items by mutableStateOf(seed); private set

    /** `notificationBadgeStale`: the badge keeps the original count after reading. */
    val unreadCount: Int
        get() = if (Defects.isActive(DefectId.notificationBadgeStale)) initialUnread else items.count { !it.read }

    fun markAllRead() { items = items.map { it.copy(read = true) } }

    /** `notificationOpensWrongScreen`: tapping opens a different destination. */
    fun target(n: AppNotification): Route =
        if (Defects.isActive(DefectId.notificationOpensWrongScreen)) wrongRoute(n) else n.route

    private fun wrongRoute(n: AppNotification): Route =
        if (n.route == Route.Transactions) Route.Exchange else Route.Transactions
}
