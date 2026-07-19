package com.vadimtoptunov.chaosbank_android.core.exercises

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Keeps the committed `exercises.json` in sync with the Kotlin catalog. Regenerate
 * with `./gradlew :app:testStandardDebugUnitTest -DupdateExercises=1`; otherwise this
 * asserts the file on disk matches [Exercises.all].
 */
class CatalogJsonTest {

    @Test fun exercisesJson_isUpToDate() {
        val rendered = render(Exercises.all)
        val file = File("../exercises.json") // test working dir = the app module
        if (System.getProperty("updateExercises").orEmpty().isNotEmpty()) {
            file.writeText(rendered)
        }
        assertTrue("exercises.json not found at ${file.absolutePath}", file.exists())
        assertEquals(
            "exercises.json is stale — regenerate with -DupdateExercises=1",
            rendered.trim(), file.readText().trim(),
        )
    }

    private fun render(all: List<Exercise>): String =
        "[\n" + all.joinToString(",\n") { obj(it) } + "\n]\n"

    private fun obj(ex: Exercise): String {
        val fields = listOf(
            "category" to str(ex.category),
            "condition" to str(ex.condition),
            "defects" to arr(ex.defects),
            "difficulty" to str(ex.difficulty),
            "expectedBuggy" to str(ex.expectedBuggy),
            "expectedClean" to str(ex.expectedClean),
            "feature" to str(ex.feature),
            "id" to str(ex.id),
            "keyLocators" to arr(ex.keyLocators),
            "launchArgument" to str(ex.launchArgument),
            "profile" to str(ex.profile),
            "task" to str(ex.task),
            "title" to str(ex.title),
        )
        return "  {\n" + fields.joinToString(",\n") { (k, v) -> "    ${str(k)}: $v" } + "\n  }"
    }

    private fun arr(items: List<String>): String =
        if (items.isEmpty()) "[]"
        else "[\n" + items.joinToString(",\n") { "      ${str(it)}" } + "\n    ]"

    private fun str(s: String?): String {
        if (s == null) return "null"
        val sb = StringBuilder("\"")
        for (c in s) when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
        }
        return sb.append("\"").toString()
    }
}
