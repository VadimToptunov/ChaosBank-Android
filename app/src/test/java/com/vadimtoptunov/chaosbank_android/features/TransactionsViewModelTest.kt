package com.vadimtoptunov.chaosbank_android.features

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.features.transactions.TransactionsViewModel
import com.vadimtoptunov.chaosbank_android.features.transactions.TxFilter
import com.vadimtoptunov.chaosbank_android.models.TransactionDirection
import com.vadimtoptunov.chaosbank_android.support.CoroutineTest
import com.vadimtoptunov.chaosbank_android.support.servicesWith
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionsViewModelTest : CoroutineTest() {

    private fun vm(vararg d: DefectId) = TransactionsViewModel(servicesWith(*d))

    @Test fun clean_loadsAllSixteen() = runTest {
        val vm = vm(); vm.load()
        assertEquals(16, vm.filtered.size)
    }

    @Test fun paginates_sixPerPage() = runTest {
        val vm = vm(); vm.load()
        assertEquals(6, vm.visible.size)
        assertTrue(vm.canLoadMore)
        vm.loadMore()
        assertEquals(12, vm.visible.size)
    }

    @Test fun filterMoneyIn_onlyIncoming() = runTest {
        val vm = vm(); vm.load(); vm.updateFilter(TxFilter.moneyIn)
        assertTrue(vm.filtered.all { it.direction == TransactionDirection.moneyIn })
        assertTrue(vm.filtered.isNotEmpty())
    }

    @Test fun filterLeaksCategory_moneyInLeaksOut() = runTest {
        val vm = vm(DefectId.filterLeaksCategory); vm.load(); vm.updateFilter(TxFilter.moneyIn)
        assertTrue(vm.filtered.any { it.direction == TransactionDirection.moneyOut })
    }

    @Test fun search_matchesTitleCaseInsensitive() = runTest {
        val vm = vm(); vm.load(); vm.updateSearch("coffee")
        assertTrue(vm.filtered.any { it.title.contains("Coffee") })
        assertTrue(vm.filtered.all { it.title.lowercase().contains("coffee") || it.category.lowercase().contains("coffee") })
    }

    @Test fun searchCaseSensitive_missesLowercaseQuery() = runTest {
        val vm = vm(DefectId.searchCaseSensitive); vm.load(); vm.updateSearch("coffee")
        assertTrue(vm.filtered.none { it.title.contains("Coffee") })
    }

    @Test fun updatingFilterResetsPagination() = runTest {
        val vm = vm(); vm.load(); vm.loadMore()
        assertEquals(12, vm.visible.size)
        vm.updateFilter(TxFilter.moneyOut)
        assertEquals(6, vm.visible.size)
    }

    @Test fun grouped_keysAreNonEmpty() = runTest {
        val vm = vm(); vm.load()
        assertTrue(vm.grouped.isNotEmpty())
        assertTrue(vm.grouped.all { it.second.isNotEmpty() })
    }

    @Test fun clean_paginationTerminates() = runTest {
        val vm = vm(); vm.load()
        repeat(10) { vm.loadMore() }
        assertFalse(vm.canLoadMore)
    }

    @Test fun paginationNeverEnds_alwaysHasMore() = runTest {
        val vm = vm(DefectId.paginationNeverEnds); vm.load()
        val start = vm.visible.size
        repeat(20) { vm.loadMore() }
        assertTrue(vm.canLoadMore)
        assertTrue(vm.visible.size > start)
    }

    @Test fun paginationDup_duplicatesBoundaryRow() = runTest {
        // Capture the clean size before reconfiguring the global Defects surface.
        val cleanSize = vm().run { load(); loadMore(); visible.size }
        val buggy = vm(DefectId.paginationDup); buggy.load(); buggy.loadMore()
        assertEquals(cleanSize + 1, buggy.visible.size)
    }
}
