package com.vadimtoptunov.chaosbank_android.core.backend

/**
 * A simulated network environment (reliability cluster), chosen from the dev menu.
 * Not a defect — an environment condition testers can exercise:
 *  - [normal]  live reads and writes.
 *  - [offline] reads serve cached data; writes fail.
 *  - [slow]    every call gets a large extra latency (spinner / timeout testing).
 *  - [flaky]   writes fail transiently at random (retry / error-handling testing).
 */
enum class NetworkCondition(val title: String) {
    normal("Normal"),
    offline("Offline"),
    slow("Slow"),
    flaky("Flaky");

    companion object {
        fun from(raw: String?): NetworkCondition =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: normal
    }
}
