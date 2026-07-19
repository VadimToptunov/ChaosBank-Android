package com.vadimtoptunov.chaosbank_android.features

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.features.transfer.TransferViewModel
import com.vadimtoptunov.chaosbank_android.support.CoroutineTest
import com.vadimtoptunov.chaosbank_android.support.servicesWith
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TransferViewModelTest : CoroutineTest() {

    private fun loaded(vararg d: DefectId) = TransferViewModel(servicesWith(*d))

    @Test fun emptyForm_cannotContinue() = runTest {
        val vm = loaded(); vm.load()
        assertFalse(vm.canContinue)
    }

    @Test fun validForm_canContinue() = runTest {
        val vm = loaded(); vm.load()
        vm.recipient = "Alex"; vm.amountText = "100"
        assertTrue(vm.canContinue)
    }

    @Test fun overBalance_blockedByDefault() = runTest {
        val vm = loaded(); vm.load()
        vm.recipient = "Alex"; vm.amountText = "99999999"
        assertFalse(vm.canContinue)
    }

    @Test fun amountExceedsBalanceAllowed_permitsOverBalance() = runTest {
        val vm = loaded(DefectId.amountExceedsBalanceAllowed); vm.load()
        vm.recipient = "Alex"; vm.amountText = "99999999"
        assertTrue(vm.canContinue)
    }

    @Test fun zeroAmountAccepted_permitsZero() = runTest {
        val vm = loaded(DefectId.zeroAmountAccepted); vm.load()
        vm.recipient = "Alex"; vm.amountText = "0"
        assertTrue(vm.canContinue)
    }

    @Test fun clean_rejectsZero() = runTest {
        val vm = loaded(); vm.load()
        vm.recipient = "Alex"; vm.amountText = "0"
        assertFalse(vm.canContinue)
    }

    @Test fun whitespaceRecipient_keepsSpaces() = runTest {
        val clean = loaded(); clean.load(); clean.recipient = "  "; clean.amountText = "10"
        assertFalse(clean.canContinue)
        val buggy = loaded(DefectId.whitespaceRecipient); buggy.load(); buggy.recipient = "  "; buggy.amountText = "10"
        assertTrue(buggy.canContinue)
        assertEquals("  ", buggy.effectiveRecipient)
    }

    @Test fun balanceAfterAdds_addsInsteadOfSubtracts() = runTest {
        val vm = loaded(DefectId.balanceAfterAdds); vm.load()
        vm.amountText = "100"
        assertTrue(vm.balanceAfter!!.amount > vm.fromBalance)
    }

    @Test fun clean_balanceAfterSubtracts() = runTest {
        val vm = loaded(); vm.load()
        vm.amountText = "100"
        assertEquals(0, vm.fromBalance.minus(BigDecimal("100")).compareTo(vm.balanceAfter!!.amount))
    }

    @Test fun transferConfirmWrongRecipient_showsDifferentName() = runTest {
        val vm = loaded(DefectId.transferConfirmWrongRecipient); vm.load()
        vm.recipient = "Alex"
        assertEquals("ACME Holdings Ltd", vm.confirmRecipientText)
    }

    @Test fun confirmTransfer_succeedsAndDebits() = runTest {
        val services = servicesWith()
        val vm = TransferViewModel(services); vm.load()
        vm.recipient = "Alex"; vm.amountText = "100"
        val before = services.backend.fetchAccount(Currency.EUR)!!.balance
        vm.confirmTransfer()
        assertTrue(vm.succeeded)
        assertEquals(0, before.minus(BigDecimal("100")).compareTo(services.backend.fetchAccount(Currency.EUR)!!.balance))
    }

    @Test fun unverifiedKyc_blocksLargeTransfer() = runTest {
        val services = servicesWith()
        services.kyc.applyVerified(false)
        val vm = TransferViewModel(services); vm.load()
        vm.recipient = "Alex"; vm.amountText = "2000"
        assertTrue(vm.kycBlocked)
        assertFalse(vm.canContinue)
    }

    @Test fun kycBypassAllowsTransfer_permitsLargeUnverified() = runTest {
        val services = servicesWith(DefectId.kycBypassAllowsTransfer)
        services.kyc.applyVerified(false)
        val vm = TransferViewModel(services); vm.load()
        vm.recipient = "Alex"; vm.amountText = "2000"
        assertTrue(vm.canContinue)
    }

    @Test fun confirmTransfer_insufficientFundsSetsError() = runTest {
        val vm = loaded(); vm.load()
        vm.recipient = "Alex"; vm.amountText = "99999999"
        vm.confirmTransfer()
        assertFalse(vm.succeeded)
        assertEquals("Insufficient funds", vm.errorMessage)
    }
}
