package com.vadimtoptunov.chaosbank_android.features.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.TokenStore
import com.vadimtoptunov.chaosbank_android.core.defects.BugProfile
import com.vadimtoptunov.chaosbank_android.core.defects.BugProfiles
import com.vadimtoptunov.chaosbank_android.core.defects.Defect
import com.vadimtoptunov.chaosbank_android.core.defects.DefectCategory
import com.vadimtoptunov.chaosbank_android.core.defects.DefectRegistry
import com.vadimtoptunov.chaosbank_android.core.exercises.Exercises
import com.vadimtoptunov.chaosbank_android.core.feed.PriceSourceKind
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentBar
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentItem
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

@Composable
fun DevMenuScreen(onClose: () -> Unit) {
    var showExercises by remember { mutableStateOf(false) }
    if (showExercises) {
        ExercisesScreen(onBack = { showExercises = false })
        return
    }

    val services = LocalAppServices.current

    Column(Modifier.fillMaxSize().background(Palette.bg).testTag(A11y.Dev.menu)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Developer", color = Palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Done", color = Palette.sand, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onClose() }.testTag(A11y.Dev.close))
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            CardSurface {
                Text("Active profile", color = Palette.muted, fontSize = 12.sp)
                Text(services.config.label, color = Palette.sand, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag(A11y.Dev.activeLabel))
                Text("Build ${services.config.version} · RNG seed ${services.config.seedBadge}", color = Palette.muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.surface)
                    .clickable { showExercises = true }.padding(14.dp).testTag(A11y.Dev.exercises),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Exercises", color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${Exercises.all.size}  ›", color = Palette.muted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }

            Section("Price data") {
                SegmentBar(
                    items = PriceSourceKind.entries.map { SegmentItem(it.name, it.title, A11y.Dev.priceSourceOption(it.name)) },
                    selected = services.market.source.name,
                    modifier = Modifier.testTag(A11y.Dev.priceSource),
                ) { services.setPriceSource(PriceSourceKind.valueOf(it)) }
                Text(
                    if (services.market.source == PriceSourceKind.live) "Real Yahoo Finance quotes — non-deterministic."
                    else "Seeded simulation — reproducible for tests.",
                    color = Palette.muted, fontSize = 11.sp,
                )
            }

            Section("Security") {
                CardSurface {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Session token storage", color = Palette.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(
                            TokenStore.storageDescription,
                            color = if (TokenStore.isTokenInPrefs) Palette.loss else Palette.gain,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag(A11y.Dev.tokenStorage),
                        )
                    }
                }
            }

            Section("Profiles") {
                BugProfiles.all.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { ProfileChip(it, services.config.label == it.id, Modifier.weight(1f)) { services.applyProfile(it) } }
                        if (pair.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }

            Section("Defects (${services.config.activeDefects.size} active)") {
                DefectCategory.entries.forEach { category ->
                    val defects = DefectRegistry.all.filter { it.category == category }
                    if (defects.isNotEmpty()) CategoryBlock(category, defects)
                }
            }
        }
    }
}

@Composable
private fun ProfileChip(profile: BugProfile, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (active) Palette.sand else Palette.surface2)
            .clickable { onClick() }.padding(12.dp).testTag(A11y.Dev.profile(profile.id)),
    ) {
        Text(profile.title, color = if (active) Palette.bg else Palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text("${profile.defects.size} defects", color = if (active) Palette.bg.copy(alpha = 0.7f) else Palette.muted, fontSize = 11.sp)
    }
}

@Composable
private fun CategoryBlock(category: DefectCategory, defects: List<Defect>) {
    val services = LocalAppServices.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(category.title.uppercase(), color = Palette.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        CardSurface(padding = 8.dp) {
            defects.forEachIndexed { index, defect ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(defect.title, color = Palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(defect.id.name, color = Palette.muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = services.isActive(defect.id), onCheckedChange = { services.toggle(defect.id) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Palette.sand),
                        modifier = Modifier.testTag(A11y.Dev.defectToggle(defect.id)),
                    )
                }
                if (index < defects.size - 1) HorizontalDivider(color = Palette.line)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = Palette.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}
