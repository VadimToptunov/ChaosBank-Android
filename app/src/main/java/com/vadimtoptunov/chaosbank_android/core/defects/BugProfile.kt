package com.vadimtoptunov.chaosbank_android.core.defects

/**
 * Named bug profiles — the "one build, many training tasks" surface. Most profiles
 * are derived from [DefectCategory] so they stay in sync with the catalog.
 */
data class BugProfile(
    val id: String,
    val title: String,
    val summary: String,
    val seed: Int,
    val defects: Set<DefectId>,
)

object BugProfiles {

    private fun category(id: String, title: String, c: DefectCategory, seed: Int = 0) =
        BugProfile(id, title, "All ${c.title.lowercase()} defects.", seed, DefectRegistry.ids(c))

    val clean = BugProfile(
        "clean", "Clean", "No defects. Passes the full reference suite.", 0, emptySet()
    )

    val all: List<BugProfile> = listOf(
        clean,
        category("ui", "UI & layout", DefectCategory.Ui),
        category("validation", "Validation", DefectCategory.Validation),
        category("accessibility", "Accessibility", DefectCategory.Accessibility),
        category("state", "State & navigation", DefectCategory.State),
        category("localization", "Localization", DefectCategory.Localization),
        category("security", "Security & privacy", DefectCategory.Security),
        category("network", "Networking", DefectCategory.Network, seed = 7),
        BugProfile(
            "flaky", "Flaky (races)", "Concurrency & race-condition defects. Seed-pinned.",
            7, DefectRegistry.ids(DefectCategory.Concurrency)
        ),
        BugProfile(
            "beginner", "Beginner", "A few easy, deterministic defects to start.",
            0, setOf(DefectId.zeroAmountAccepted, DefectId.staleBalance, DefectId.cardToggleInvert)
        ),
        BugProfile(
            "middle", "Middle", "Validation, pagination, rounding & locale.",
            0, setOf(DefectId.paginationDup, DefectId.localeParse, DefectId.roundingDrift, DefectId.limitValidation)
        ),
        BugProfile(
            "senior", "Senior", "Races, networking & subtle state/security defects.",
            7, DefectRegistry.ids(DefectCategory.Concurrency) +
                DefectRegistry.ids(DefectCategory.Network) +
                setOf(DefectId.pnlSign, DefectId.authBypass, DefectId.slowResponseRace)
        ),
        BugProfile(
            "all", "Everything", "Every defect at once.", 7, DefectId.entries.toSet()
        ),
    )

    fun profile(id: String): BugProfile? = all.firstOrNull { it.id.equals(id, ignoreCase = true) }
}
