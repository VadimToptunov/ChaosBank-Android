package com.vadimtoptunov.chaosbank_android.app

import com.vadimtoptunov.chaosbank_android.core.defects.ChaosConfig
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationStoreTest {
    @Before fun clean() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }
    @After fun reset() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }

    private fun on(vararg d: DefectId) { Defects.configure(ChaosConfig(0, d.toSet(), "test")) }

    @Test fun clean_badgeClearsAfterReading() {
        val store = NotificationStore()
        assertTrue(store.unreadCount > 0)
        store.markAllRead()
        assertEquals(0, store.unreadCount)
    }

    @Test fun badgeStale_keepsOriginalCount() {
        on(DefectId.notificationBadgeStale)
        val store = NotificationStore()
        val before = store.unreadCount
        store.markAllRead()
        assertEquals(before, store.unreadCount)
        assertTrue(store.unreadCount > 0)
    }

    @Test fun clean_targetIsStatedRoute() {
        val store = NotificationStore()
        val n = store.items.first { it.route == Route.Transactions }
        assertEquals(Route.Transactions, store.target(n))
    }

    @Test fun opensWrongScreen_targetDiffers() {
        on(DefectId.notificationOpensWrongScreen)
        val store = NotificationStore()
        val n = store.items.first { it.route == Route.Transactions }
        assertNotEquals(n.route, store.target(n))
    }
}
