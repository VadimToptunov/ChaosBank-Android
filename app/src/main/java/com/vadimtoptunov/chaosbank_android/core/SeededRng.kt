package com.vadimtoptunov.chaosbank_android.core

/**
 * SplitMix64 — deterministic, fast. Every "random" value in the app (the price
 * walk, race-defect coin flips) is drawn from an RNG seeded off the build seed so
 * two runs with the same seed reproduce exactly.
 */
class SeededRng(seed: Long) {
    private var state: ULong = seed.toULong() + 0x9E3779B97F4A7C15uL

    fun next(): ULong {
        state += 0x9E3779B97F4A7C15uL
        var z = state
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        return z xor (z shr 31)
    }

    /** Uniform double in [0, 1). */
    fun nextDouble(): Double = (next() shr 11).toLong().toDouble() / (1L shl 53).toDouble()

    /** Uniform double in [min, max). */
    fun nextInRange(min: Double, max: Double): Double = min + nextDouble() * (max - min)
}

/**
 * Deterministic 64-bit FNV-1a hash. Seeds per-symbol decoration (sparkline shape,
 * pseudo market-cap/volume) so it is stable across launches.
 */
object StableHash {
    fun of(string: String): ULong {
        var h = 0xcbf29ce484222325uL
        for (c in string) {
            h = h xor c.code.toULong()
            h *= 0x100000001b3uL
        }
        return h
    }
}
