package com.vadimtoptunov.chaosbank_android.core.defects

/** The taxonomy a tester reasons in. Profiles are largely derived from these. */
enum class DefectCategory(val title: String) {
    Money("Money & logic"),
    Validation("Validation"),
    Localization("Localization"),
    State("State & navigation"),
    Concurrency("Concurrency & races"),
    Ui("UI & layout"),
    Accessibility("Accessibility"),
    Security("Security & privacy"),
    Network("Networking"),
    Performance("Performance"),
}
