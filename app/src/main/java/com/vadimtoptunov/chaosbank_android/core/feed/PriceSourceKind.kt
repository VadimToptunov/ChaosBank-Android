package com.vadimtoptunov.chaosbank_android.core.feed

enum class PriceSourceKind(val title: String) {
    simulated("Simulated"),
    live("Live");

    companion object {
        fun from(raw: String?): PriceSourceKind =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: simulated
    }
}
