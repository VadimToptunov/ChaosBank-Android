package com.vadimtoptunov.chaosbank_android.core.defects

import com.vadimtoptunov.chaosbank_android.core.feed.PriceSourceKind

/** The active build. `label` drives the badge; `activeDefects` drives every injection. */
data class ChaosConfig(
    val seed: Int,
    val activeDefects: Set<DefectId>,
    val label: String,
    val priceSource: PriceSourceKind = PriceSourceKind.simulated,
) {
    val version: String = "1.0"
    val seedBadge: String get() = "%02d".format(seed)
}

/**
 * The single query surface every guarded injection point uses:
 *   `if (Defects.isActive(DefectId.someBug)) { ... }`
 */
object Defects {
    @Volatile
    var config: ChaosConfig = ChaosConfig(0, emptySet(), "clean")
        private set

    fun configure(config: ChaosConfig) {
        this.config = config
    }

    fun isActive(id: DefectId): Boolean = config.activeDefects.contains(id)

    val seed: Int get() = config.seed
}
