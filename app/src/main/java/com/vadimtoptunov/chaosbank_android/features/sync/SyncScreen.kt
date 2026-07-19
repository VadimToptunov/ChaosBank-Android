package com.vadimtoptunov.chaosbank_android.features.sync

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.PrimaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.SecondaryButton
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import kotlinx.coroutines.launch

/** A concurrency playground surfacing the `syncLostUpdate` race. */
@Composable
fun SyncScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val services = LocalAppServices.current
    val scope = rememberCoroutineScope()
    val vm = remember { SyncViewModel(services) }
    LaunchedEffect(Unit) { vm.reset(); vm.load() }

    Column(Modifier.fillMaxSize().background(Palette.bg).testTag(A11y.Sync.root)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Back", color = Palette.sand, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onBack() })
            Text("Sync playground", color = Palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
        }
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            CardSurface {
                Text("Counter", color = Palette.muted, fontSize = 13.sp)
                Text(
                    vm.counter.toString(), color = Palette.text, fontSize = 44.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.testTag(A11y.Sync.counter),
                )
                Text(
                    "Run ${vm.concurrency} parallel +1 → expect ${vm.expected}",
                    color = Palette.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp).testTag(A11y.Sync.expected),
                )
            }
            PrimaryButton("Run ${vm.concurrency} concurrent +1", Modifier.testTag(A11y.Sync.runButton), enabled = !vm.running) {
                scope.launch { vm.runConcurrent() }
            }
            SecondaryButton("Reset", Modifier.testTag(A11y.Sync.resetButton)) {
                scope.launch { vm.reset() }
            }
        }
    }
}
