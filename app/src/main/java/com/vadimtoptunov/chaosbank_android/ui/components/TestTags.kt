package com.vadimtoptunov.chaosbank_android.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Exposes every `testTag` below this node as a view resource-id, so UiAutomator
 * (and Appium on top of it) can find elements by the same ids the iOS build uses.
 * Compose tests don't need this; black-box drivers do.
 *
 * A dialog or bottom sheet is a separate window with its own semantics root, so
 * its content needs this modifier too.
 */
fun Modifier.exposeTestTags(): Modifier = semantics { testTagsAsResourceId = true }
