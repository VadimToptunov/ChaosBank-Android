package com.vadimtoptunov.chaosbank_android.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkTest {
    @Test fun parsesTabHosts() {
        assertEquals(1, DeepLink.tabIndex("chaosbank://markets"))
        assertEquals(2, DeepLink.tabIndex("chaosbank://portfolio"))
        assertEquals(0, DeepLink.tabIndex("chaosbank://home"))
        assertEquals(3, DeepLink.tabIndex("CHAOSBANK://CARD"))
    }

    @Test fun parsesRouteHosts() {
        assertEquals(Route.Transfer, DeepLink.route("chaosbank://transfer"))
        assertEquals(Route.Exchange, DeepLink.route("chaosbank://exchange"))
        assertEquals(Route.Transactions, DeepLink.route("chaosbank://transactions"))
        assertEquals(Route.AddMoney, DeepLink.route("chaosbank://addmoney"))
    }

    @Test fun ignoresUnknownAndWrongScheme() {
        assertNull(DeepLink.tabIndex("chaosbank://nope"))
        assertNull(DeepLink.route("https://markets"))
        assertNull(DeepLink.tabIndex(null))
        assertFalse(DeepLink.isPresent("chaosbank://nope"))
        assertFalse(DeepLink.isPresent(null))
    }

    @Test fun stripsPathAndQuery() {
        assertEquals(1, DeepLink.tabIndex("chaosbank://markets/AAPL?x=1"))
    }

    @Test fun isPresent_forKnownTargets() {
        assertTrue(DeepLink.isPresent("chaosbank://markets"))
        assertTrue(DeepLink.isPresent("chaosbank://transfer"))
    }

    @Test fun bypassesAuth_onlyWithDefectAndDeepLink() {
        assertTrue(DeepLink.bypassesAuth("chaosbank://markets", defectActive = true))
        assertFalse(DeepLink.bypassesAuth("chaosbank://markets", defectActive = false))
        assertFalse(DeepLink.bypassesAuth(null, defectActive = true))
        assertFalse(DeepLink.bypassesAuth("chaosbank://nope", defectActive = true))
    }
}
