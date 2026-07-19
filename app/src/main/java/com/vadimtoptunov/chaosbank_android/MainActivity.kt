package com.vadimtoptunov.chaosbank_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import com.vadimtoptunov.chaosbank_android.app.AppServices
import com.vadimtoptunov.chaosbank_android.app.AuthFlow
import com.vadimtoptunov.chaosbank_android.app.ConfigResolver
import com.vadimtoptunov.chaosbank_android.app.DeepLink
import com.vadimtoptunov.chaosbank_android.app.LaunchOptions
import com.vadimtoptunov.chaosbank_android.app.Route
import com.vadimtoptunov.chaosbank_android.core.TokenStore
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.RootScreen
import com.vadimtoptunov.chaosbank_android.ui.theme.ChaosBankAndroidTheme

class MainActivity : ComponentActivity() {

    private lateinit var services: AppServices
    private lateinit var auth: AuthFlow
    private lateinit var options: LaunchOptions
    private val inactive = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TokenStore.init(this)

        // The Gradle flavor bakes a default profile (distributable per-defect build).
        ConfigResolver.bakedDefaultProfile = BuildConfig.CHAOSBANK_BAKED_PROFILE.ifEmpty { null }

        val extra: (String) -> String? = { intent.getStringExtra(it) }
        val config = ConfigResolver.resolve(
            profile = extra("CHAOSBANK_PROFILE"),
            defects = extra("CHAOSBANK_DEFECTS"),
            seed = extra("CHAOSBANK_SEED"),
            priceSource = extra("CHAOSBANK_PRICE_SOURCE"),
        )
        Defects.configure(config)
        services = AppServices(config)
        services.startFeed()

        // Deep link (chaosbank://<host>): pick a tab and, for pushed screens, a route.
        // Correct behaviour keeps the auth gate; `deepLinkSkipsAuth` bypasses it.
        val deepUri = intent.dataString
        val deepBypass = DeepLink.bypassesAuth(deepUri, Defects.isActive(DefectId.deepLinkSkipsAuth))
        val pendingRoute: Route? = DeepLink.route(deepUri)

        val base = LaunchOptions.from(extra)
        options = base.copy(
            initialTab = DeepLink.tabIndex(deepUri) ?: base.initialTab,
            startUnlocked = base.startUnlocked || deepBypass,
        )
        auth = AuthFlow(startUnlocked = options.startUnlocked)

        setContent {
            ChaosBankAndroidTheme {
                CompositionLocalProvider(LocalAppServices provides services) {
                    RootScreen(auth, options, inactive, pendingRoute)
                }
            }
        }
    }

    override fun onPause() { super.onPause(); inactive.value = true }
    override fun onResume() { super.onResume(); inactive.value = false }
    override fun onStop() { super.onStop(); if (::auth.isInitialized) auth.handleBackground() }
}
