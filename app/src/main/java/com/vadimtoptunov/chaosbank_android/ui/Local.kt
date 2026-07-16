package com.vadimtoptunov.chaosbank_android.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.vadimtoptunov.chaosbank_android.app.AppServices

val LocalAppServices = staticCompositionLocalOf<AppServices> {
    error("AppServices not provided")
}

/** Opens the hidden developer menu (long-press the build badge). No-op by default. */
val LocalDevMenu = staticCompositionLocalOf<() -> Unit> { {} }
