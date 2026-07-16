package com.vadimtoptunov.chaosbank_android.core.feed

import com.vadimtoptunov.chaosbank_android.models.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PriceFeedTest {
    @Test fun sameSeed_reproducesWalk() {
        val a = PriceFeed(3, SeedData.assets)
        val b = PriceFeed(3, SeedData.assets)
        repeat(5) {
            val qa = a.step()
            val qb = b.step()
            for (asset in SeedData.assets) {
                assertEquals(qa[asset.symbol]!!.price, qb[asset.symbol]!!.price)
            }
        }
    }

    @Test fun snapshot_startsAtBasePrice() {
        val feed = PriceFeed(1, SeedData.assets)
        val snap = feed.snapshot()
        assertEquals(SeedData.assets.first().basePrice, snap[SeedData.assets.first().symbol]!!.price)
    }

    @Test fun step_neverGoesBelowFloor() {
        val feed = PriceFeed(5, SeedData.assets)
        repeat(200) {
            feed.step().values.forEach { assertTrue(it.price >= BigDecimal("0.01")) }
        }
    }
}
