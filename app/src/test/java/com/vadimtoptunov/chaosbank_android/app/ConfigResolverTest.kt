package com.vadimtoptunov.chaosbank_android.app

import com.vadimtoptunov.chaosbank_android.core.backend.BackendScenario
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.DefectRegistry
import com.vadimtoptunov.chaosbank_android.core.feed.PriceSourceKind
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigResolverTest {
    @After fun clearBaked() { ConfigResolver.bakedDefaultProfile = null }

    @Test fun defaultsToClean() {
        val c = ConfigResolver.resolve(baked = null)
        assertEquals("clean", c.label)
        assertTrue(c.activeDefects.isEmpty())
        assertEquals(0, c.seed)
    }

    @Test fun profileWins() {
        val c = ConfigResolver.resolve(profile = "flaky", baked = null)
        assertEquals("flaky", c.label)
        assertTrue(c.activeDefects.isNotEmpty())
    }

    @Test fun explicitDefectsOverrideProfile() {
        val c = ConfigResolver.resolve(profile = "flaky", defects = "pnlSign,roundingDrift", baked = null)
        assertEquals("custom", c.label)
        assertEquals(setOf(DefectId.pnlSign, DefectId.roundingDrift), c.activeDefects)
    }

    @Test fun seedResolvesToSeedProfile() {
        val c = ConfigResolver.resolve(seed = "7", baked = null)
        assertEquals(DefectRegistry.defectsForSeed(7), c.activeDefects)
        assertEquals(7, c.seed)
        assertEquals("seed 07", c.label)
    }

    @Test fun profileArgBeatsBaked() {
        val c = ConfigResolver.resolve(profile = "security", baked = "flaky")
        assertEquals("security", c.label)
    }

    @Test fun bakedUsedWhenNoProfileArg() {
        val c = ConfigResolver.resolve(baked = "flaky")
        assertEquals("flaky", c.label)
    }

    @Test fun priceSourceParsed() {
        assertEquals(PriceSourceKind.live, ConfigResolver.resolve(priceSource = "live", baked = null).priceSource)
        assertEquals(PriceSourceKind.simulated, ConfigResolver.resolve(priceSource = null, baked = null).priceSource)
    }

    @Test fun unknownDefectTokensIgnored() {
        val c = ConfigResolver.resolve(defects = "pnlSign,notARealDefect", baked = null)
        assertEquals(setOf(DefectId.pnlSign), c.activeDefects)
    }
}

class BackendScenarioTest {
    @Test fun mapsEachFlag() {
        val s = BackendScenario.from(setOf(DefectId.retryDuplicate, DefectId.timeoutAsSuccess, DefectId.balanceReadReturnsZero))
        assertTrue(s.retryDuplicate)
        assertTrue(s.timeoutAsSuccess)
        assertTrue(s.balanceReadReturnsZero)
        assertFalse(s.staleOfflineBalance)
        assertFalse(s.transactionsDupOnFetch)
    }

    @Test fun emptyIsAllFalse() {
        val s = BackendScenario.from(emptySet())
        assertFalse(s.retryDuplicate || s.timeoutAsSuccess || s.slowResponseRace || s.staleOfflineBalance ||
            s.balanceReadReturnsZero || s.transactionsDupOnFetch || s.staleHoldingsAfterOrder)
    }
}
