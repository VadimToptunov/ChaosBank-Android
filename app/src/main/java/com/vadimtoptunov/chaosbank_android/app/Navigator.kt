package com.vadimtoptunov.chaosbank_android.app

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.vadimtoptunov.chaosbank_android.models.OrderRequest

/** A pushed destination that overlays the tab scaffold (SwiftUI NavigationStack analogue). */
sealed interface Route {
    data class AssetDetail(val symbol: String) : Route
    data class OrderTicket(val request: OrderRequest) : Route
    data object Transfer : Route
    data object Exchange : Route
    data object AddMoney : Route
    data object Transactions : Route
}

/** A minimal Compose back-stack. Only the top route is rendered over the tabs. */
class Navigator {
    val stack = mutableStateListOf<Route>()
    val current: Route? get() = stack.lastOrNull()

    fun push(route: Route) { stack.add(route) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.size - 1) }
    fun reset() { stack.clear() }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No Navigator provided") }
