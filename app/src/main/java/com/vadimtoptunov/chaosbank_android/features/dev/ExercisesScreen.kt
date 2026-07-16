package com.vadimtoptunov.chaosbank_android.features.dev

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.exercises.Exercise
import com.vadimtoptunov.chaosbank_android.core.exercises.Exercises
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

@Composable
fun ExercisesScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val services = LocalAppServices.current
    val order = listOf("junior", "middle", "senior")

    Column(Modifier.fillMaxSize().background(Palette.bg).testTag(A11y.Dev.exercisesList)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Back", color = Palette.sand, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onBack() })
            Text("Exercises", color = Palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("${Exercises.all.size} exercises · one per defect", color = Palette.muted, fontSize = 12.sp)
            order.forEach { level ->
                val items = Exercises.all.filter { it.difficulty == level }
                if (items.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(level.uppercase(), color = Palette.sand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        items.forEach { ExerciseCard(it) { ids -> services.applyDefects(ids, it.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, onApply: (Set<DefectId>) -> Unit) {
    CardSurface(modifier = Modifier.testTag(A11y.Dev.exercise(exercise.id))) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.id, color = Palette.sand, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text(exercise.category, color = Palette.muted, fontSize = 11.sp)
            }
            Text(exercise.title, color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(exercise.task, color = Palette.muted, fontSize = 13.sp)
            Text("Clean: ${exercise.expectedClean}", color = Palette.gain, fontSize = 11.sp)
            Text("Buggy: ${exercise.expectedBuggy}", color = Palette.loss, fontSize = 11.sp)
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Palette.sand)
                    .clickable { onApply(exercise.defects.mapNotNull { DefectId.from(it) }.toSet()) }
                    .padding(horizontal = 14.dp, vertical = 8.dp).testTag(A11y.Dev.exerciseApply(exercise.id)),
            ) {
                Text("Apply this exercise", color = Palette.bg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
