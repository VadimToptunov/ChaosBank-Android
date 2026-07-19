package com.vadimtoptunov.chaosbank_android.app

/**
 * Parses `chaosbank://<host>` deep links into an initial tab or a pushed route.
 * Pure string parsing (no Android URI) so it is unit-testable.
 *
 * A deep link normally still passes through the auth gate; the `deepLinkSkipsAuth`
 * defect lets it bypass authentication (see [MainActivity]).
 */
object DeepLink {
    private const val SCHEME = "chaosbank://"
    private val tabs = mapOf("home" to 0, "markets" to 1, "portfolio" to 2, "card" to 3)

    private fun host(uri: String?): String? {
        if (uri == null || !uri.startsWith(SCHEME, ignoreCase = true)) return null
        val rest = uri.substring(SCHEME.length).substringBefore("/").substringBefore("?")
        return rest.lowercase().ifEmpty { null }
    }

    fun tabIndex(uri: String?): Int? = host(uri)?.let { tabs[it] }

    fun route(uri: String?): Route? = when (host(uri)) {
        "transfer" -> Route.Transfer
        "exchange" -> Route.Exchange
        "addmoney" -> Route.AddMoney
        "transactions" -> Route.Transactions
        else -> null
    }

    /** True when the URI names a known destination. */
    fun isPresent(uri: String?): Boolean = tabIndex(uri) != null || route(uri) != null

    /** Whether this deep link should skip the auth gate — only under the defect. */
    fun bypassesAuth(uri: String?, defectActive: Boolean): Boolean = isPresent(uri) && defectActive
}
