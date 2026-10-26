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
    /** Mark the web-login [android.webkit.WebView] as debuggable
     *  (`-e CHAOSBANK_INSPECTABLE_WEB 1`). Off by default, so the web login stays an
     *  opaque hybrid — tests must reach its fields through the native tree (WebView
     *  "Mode 1"). Turned on, Chromedriver can attach and Appium can
     *  `switch_to.context(WEBVIEW_…)` and walk the DOM (WebView "Mode 2"). */
    val inspectableWeb: Boolean,
    /** Start with an unverified identity (`-e CHAOSBANK_KYC_UNVERIFIED 1`), so the KYC
     *  flow and the large-transfer gate can be exercised from launch. */
    val kycUnverified: Boolean = false,
    /** What the mock document scanner and selfie check return in the KYC flow
     *  (`-e CHAOSBANK_KYC_DOCUMENT valid|expired|unreadable|nameMismatch`,
     *  `-e CHAOSBANK_KYC_SELFIE match|mismatch|noFace`). Unknown values fall back to
     *  the happy path. Mirrors iOS `-ChaosBankKycDocument` / `-ChaosBankKycSelfie`. */
    val kycDocument: String = "valid",
    val kycSelfie: String = "match",
) {
    companion object {
        private val tabs = mapOf("home" to 0, "markets" to 1, "portfolio" to 2, "card" to 3)

        /** Resolved options for the current process — read where threading the
         *  instance is awkward (e.g. the pushed-route host). Mirrors iOS. */
        var current = LaunchOptions(false, 0, false, false, false, false)
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
                inspectableWeb = flag("CHAOSBANK_INSPECTABLE_WEB"),
                kycUnverified = flag("CHAOSBANK_KYC_UNVERIFIED"),
                kycDocument = extra("CHAOSBANK_KYC_DOCUMENT") ?: "valid",
                kycSelfie = extra("CHAOSBANK_KYC_SELFIE") ?: "match",
            )
            current = opts
            return opts
        }
    }
}
