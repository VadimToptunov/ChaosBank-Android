package com.vadimtoptunov.chaosbank_android.core.defects

enum class Severity { critical, major, minor }

enum class Flakiness { deterministic, raceCondition }

/** A defect is a documented, first-class object — never a magic boolean. */
data class Defect(
    val id: DefectId,
    val title: String,
    val feature: String,
    val category: DefectCategory,
    val severity: Severity,
    val violates: String,
    val flakiness: Flakiness,
)
