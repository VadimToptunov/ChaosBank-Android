package com.vadimtoptunov.chaosbank_android.app

import com.vadimtoptunov.chaosbank_android.core.defects.BugProfiles
import com.vadimtoptunov.chaosbank_android.core.defects.ChaosConfig
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.DefectRegistry
import com.vadimtoptunov.chaosbank_android.core.feed.PriceSourceKind

/**
 * Resolves the active build. On Android the runtime overrides come from Intent
 * extras (adb `-e CHAOSBANK_PROFILE flaky` / instrumentation), and [bakedDefaultProfile]
 * is set from the Gradle flavor's BuildConfig field — the distributable per-defect build.
 *
 * Precedence: defects list > profile > seed > baked default > clean.
 */
object ConfigResolver {
    /** Set once from BuildConfig at app start (see [ChaosApp]). */
    var bakedDefaultProfile: String? = null

    fun resolve(
        profile: String? = null,
        defects: String? = null,
        seed: String? = null,
        priceSource: String? = null,
        baked: String? = bakedDefaultProfile,
    ): ChaosConfig {
        val explicitSeed = seed?.toIntOrNull()
        val profileId = profile ?: baked

        var activeSeed = explicitSeed ?: 0
        var activeDefects: Set<DefectId> = emptySet()
        var label = "clean"

        val p = profileId?.let { BugProfiles.profile(it) }
        if (p != null) {
            activeDefects = p.defects
            label = p.id
            activeSeed = explicitSeed ?: p.seed
        } else if (explicitSeed != null && explicitSeed != 0) {
            activeDefects = DefectRegistry.defectsForSeed(explicitSeed)
            label = "seed %02d".format(explicitSeed)
        }

        if (defects != null) {
            activeDefects = defects.split(",").mapNotNull { DefectId.from(it) }.toSet()
            label = "custom"
        }

        return ChaosConfig(activeSeed, activeDefects, label, PriceSourceKind.from(priceSource))
    }
}
