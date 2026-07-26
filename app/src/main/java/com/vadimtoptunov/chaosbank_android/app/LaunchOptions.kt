package com.vadimtoptunov.chaosbank_android.app

import com.vadimtoptunov.chaosbank_android.BuildConfig

/**
 * Non-defect launch affordances for UI tests / demos, read from Intent extras
 * (e.g. adb `-e CHAOSBANK_START_UNLOCKED 1 -e CHAOSBANK_TAB markets`).
 */
data class LaunchOptions(
    val startUnlocked: Boolean,
    val initialTab: Int,
    val showDevMenu: Boolean,
    val showWebLogin: Boolean,
    /** Render ported screens with the XML/View system instead of Compose. */
    val viewsBuild: Boolean,
) {
    companion object {
        private val tabs = mapOf("home" to 0, "markets" to 1, "portfolio" to 2, "card" to 3)

        /** Resolved options for the current process — read where threading the
         *  instance is awkward (e.g. the pushed-route host). Mirrors iOS. */
        var current = LaunchOptions(false, 0, false, false, false)
            private set

        fun from(extra: (String) -> String?): LaunchOptions {
            fun flag(key: String) = extra(key).let { it == "1" || it.equals("true", ignoreCase = true) }
            val tab = tabs[extra("CHAOSBANK_TAB")?.lowercase()] ?: 0
            val opts = LaunchOptions(
                startUnlocked = flag("CHAOSBANK_START_UNLOCKED"),
                initialTab = tab,
                showDevMenu = flag("CHAOSBANK_SHOW_DEV"),
                showWebLogin = flag("CHAOSBANK_SHOW_WEB_LOGIN"),
                viewsBuild = flag("CHAOSBANK_VIEWS") || BuildConfig.CHAOSBANK_VIEWS_BUILD,
            )
            current = opts
            return opts
        }
    }
}
