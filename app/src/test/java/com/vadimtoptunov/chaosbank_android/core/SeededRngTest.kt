package com.vadimtoptunov.chaosbank_android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeededRngTest {
    @Test fun sameSeed_sameSequence() {
        val a = SeededRng(42)
        val b = SeededRng(42)
        repeat(50) { assertEquals(a.next(), b.next()) }
    }

    @Test fun differentSeed_differentSequence() {
        assertNotEquals(SeededRng(1).next(), SeededRng(2).next())
    }

    @Test fun nextDouble_inUnitInterval() {
        val rng = SeededRng(7)
        repeat(1000) {
            val d = rng.nextDouble()
            assertTrue(d >= 0.0 && d < 1.0)
        }
    }

    @Test fun nextInRange_withinBounds() {
        val rng = SeededRng(99)
        repeat(1000) {
            val v = rng.nextInRange(-0.12, 0.12)
            assertTrue(v >= -0.12 && v < 0.12)
        }
    }

    @Test fun stableHash_isDeterministicAndDistinct() {
        assertEquals(StableHash.of("AAPL"), StableHash.of("AAPL"))
        assertNotEquals(StableHash.of("AAPL"), StableHash.of("NVDA"))
        assertNotEquals(StableHash.of("AAPL"), StableHash.of("AAPLv"))
    }
}
