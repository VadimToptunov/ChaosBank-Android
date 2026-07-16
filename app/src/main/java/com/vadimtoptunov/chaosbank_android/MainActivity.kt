package com.vadimtoptunov.chaosbank_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.vadimtoptunov.chaosbank_android.app.AppServices
import com.vadimtoptunov.chaosbank_android.app.ConfigResolver
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.BuildBadge
import com.vadimtoptunov.chaosbank_android.ui.theme.ChaosBankAndroidTheme
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

class MainActivity : ComponentActivity() {

    private lateinit var services: AppServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

        setContent {
            ChaosBankAndroidTheme {
                CompositionLocalProvider(LocalAppServices provides services) {
                    PlaceholderRoot()
                }
            }
        }
    }
}

@Composable
private fun PlaceholderRoot() {
    Box(Modifier.fillMaxSize().background(Palette.bg), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("🏛  ChaosBank", color = Palette.sand, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Android port", color = Palette.muted, fontSize = 14.sp)
            BuildBadge()
        }
    }
}
