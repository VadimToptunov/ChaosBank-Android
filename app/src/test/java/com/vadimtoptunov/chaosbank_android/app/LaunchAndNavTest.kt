package com.vadimtoptunov.chaosbank_android.app

import com.vadimtoptunov.chaosbank_android.models.OrderRequest
import com.vadimtoptunov.chaosbank_android.models.OrderSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class LaunchOptionsTest {
    private fun from(map: Map<String, String>) = LaunchOptions.from { map[it] }

    @Test fun defaults_whenNothingProvided() {
        val o = from(emptyMap())
        assertFalse(o.startUnlocked)
        assertEquals(0, o.initialTab)
        assertFalse(o.showDevMenu)
        assertFalse(o.showWebLogin)
    }

    @Test fun parsesFlagsAndTab() {
        val o = from(mapOf(
            "CHAOSBANK_START_UNLOCKED" to "1",
            "CHAOSBANK_TAB" to "markets",
            "CHAOSBANK_SHOW_DEV" to "true",
            "CHAOSBANK_SHOW_WEB_LOGIN" to "1",
        ))
        assertTrue(o.startUnlocked)
        assertEquals(1, o.initialTab)
        assertTrue(o.showDevMenu)
        assertTrue(o.showWebLogin)
    }

    @Test fun tabNames_mapToIndices() {
        assertEquals(0, from(mapOf("CHAOSBANK_TAB" to "home")).initialTab)
        assertEquals(1, from(mapOf("CHAOSBANK_TAB" to "MARKETS")).initialTab)
        assertEquals(2, from(mapOf("CHAOSBANK_TAB" to "portfolio")).initialTab)
        assertEquals(3, from(mapOf("CHAOSBANK_TAB" to "card")).initialTab)
        assertEquals(0, from(mapOf("CHAOSBANK_TAB" to "unknown")).initialTab)
    }
}

class NavigatorTest {
    @Test fun startsEmpty() {
        val nav = Navigator()
        assertNull(nav.current)
        assertTrue(nav.stack.isEmpty())
    }

    @Test fun pushAndPop() {
        val nav = Navigator()
        nav.push(Route.Transfer)
        nav.push(Route.AssetDetail("AAPL"))
        assertEquals(Route.AssetDetail("AAPL"), nav.current)
        nav.pop()
        assertEquals(Route.Transfer, nav.current)
        nav.pop()
        assertNull(nav.current)
    }

    @Test fun popEmpty_isSafe() {
        val nav = Navigator()
        nav.pop()
        assertNull(nav.current)
    }

    @Test fun reset_clearsStack() {
        val nav = Navigator()
        nav.push(Route.Exchange)
        nav.push(Route.OrderTicket(OrderRequest("AAPL", OrderSide.buy, BigDecimal("1"))))
        nav.reset()
        assertTrue(nav.stack.isEmpty())
    }
}
