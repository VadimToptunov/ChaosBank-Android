package com.vadimtoptunov.chaosbank_android.core.defects

import com.vadimtoptunov.chaosbank_android.core.exercises.Exercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefectRegistryTest {
    @Test fun registry_coversEveryDefectId_once() {
        assertEquals(DefectId.entries.size, DefectRegistry.all.size)
        val ids = DefectRegistry.all.map { it.id }.toSet()
        assertEquals(DefectId.entries.toSet(), ids)
    }

    @Test fun everyDefectId_hasRegistryEntry() {
        DefectId.entries.forEach { assertNotNull(DefectRegistry.defect(it)) }
    }

    @Test fun catalogSize() {
        assertEquals(117, DefectRegistry.all.size)
    }

    @Test fun ids_byCategory_areSubsetOfCategory() {
        DefectCategory.entries.forEach { c ->
            DefectRegistry.ids(c).forEach { assertEquals(c, DefectRegistry.defect(it).category) }
        }
    }

    @Test fun defectsForSeed_isDeterministic() {
        assertEquals(DefectRegistry.defectsForSeed(7), DefectRegistry.defectsForSeed(7))
    }

    @Test fun defectId_fromRoundTrips() {
        DefectId.entries.forEach { assertEquals(it, DefectId.from(it.name)) }
        assertEquals(null, DefectId.from("nope-not-real"))
    }
}

class BugProfilesTest {
    @Test fun clean_hasNoDefects() {
        assertTrue(BugProfiles.clean.defects.isEmpty())
    }

    @Test fun lookup_isCaseInsensitive() {
        assertEquals("flaky", BugProfiles.profile("FLAKY")?.id)
        assertEquals(null, BugProfiles.profile("does-not-exist"))
    }

    @Test fun everything_containsAllDefects() {
        assertEquals(DefectId.entries.toSet(), BugProfiles.profile("all")!!.defects)
    }

    @Test fun categoryProfile_matchesRegistry() {
        assertEquals(DefectRegistry.ids(DefectCategory.Ui), BugProfiles.profile("ui")!!.defects)
    }
}

class DefectsTest {
    @Test fun configure_updatesActiveAndSeed() {
        Defects.configure(ChaosConfig(7, setOf(DefectId.pnlSign), "x"))
        assertTrue(Defects.isActive(DefectId.pnlSign))
        assertFalse(Defects.isActive(DefectId.roundingDrift))
        assertEquals(7, Defects.seed)
        Defects.configure(ChaosConfig(0, emptySet(), "clean"))
    }
}

class ExercisesCatalogTest {
    @Test fun oneExercisePerDefect() {
        assertEquals(DefectId.entries.size, Exercises.all.size)
    }

    @Test fun idsAreUnique() {
        assertEquals(Exercises.all.size, Exercises.all.map { it.id }.toSet().size)
    }

    @Test fun everyExerciseMapsToOneRealDefect() {
        Exercises.all.forEach { ex ->
            assertEquals(1, ex.defects.size)
            assertNotNull(DefectId.from(ex.defects.first()))
        }
    }

    @Test fun difficultyIsFromKnownSet() {
        val allowed = setOf("junior", "middle", "senior")
        Exercises.all.forEach { assertTrue(it.difficulty in allowed) }
    }

    @Test fun idsCarryAndroidPrefix() {
        Exercises.all.forEach { assertTrue(it.id.startsWith("AND-")) }
    }
}
