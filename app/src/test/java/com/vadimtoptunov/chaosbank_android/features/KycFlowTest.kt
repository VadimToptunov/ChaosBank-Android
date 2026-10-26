package com.vadimtoptunov.chaosbank_android.features

import com.vadimtoptunov.chaosbank_android.app.KycStore
import com.vadimtoptunov.chaosbank_android.app.LaunchOptions
import com.vadimtoptunov.chaosbank_android.core.defects.ChaosConfig
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.features.kyc.KycDocumentKind
import com.vadimtoptunov.chaosbank_android.features.kyc.KycDocumentScan
import com.vadimtoptunov.chaosbank_android.features.kyc.KycFlowModel
import com.vadimtoptunov.chaosbank_android.features.kyc.KycOutcome
import com.vadimtoptunov.chaosbank_android.features.kyc.KycReview
import com.vadimtoptunov.chaosbank_android.features.kyc.KycSelfieScan
import com.vadimtoptunov.chaosbank_android.features.kyc.ScriptedDocumentScanner
import com.vadimtoptunov.chaosbank_android.features.kyc.ScriptedSelfieChecker
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KycFlowTest {
    @After fun reset() { use(emptySet()) }

    private fun use(defects: Set<DefectId>) {
        Defects.configure(ChaosConfig(0, defects, if (defects.isEmpty()) "clean" else "test"))
    }

    private val valid = KycDocumentScan(KycReview.accountHolder, "P4916204", LocalDate.of(2031, 4, 30))
    private val expired = KycDocumentScan(KycReview.accountHolder, "P4916204", LocalDate.of(2024, 1, 31))
    private val otherName = KycDocumentScan("ALEX MORGAN", "P4916204", LocalDate.of(2031, 4, 30))
    private val today = LocalDate.of(2026, 10, 25)

    // Review rules

    @Test fun validDocumentAndMatchingSelfie_verify() {
        use(emptySet())
        assertEquals(KycOutcome.Verified, KycReview.decide(valid, KycSelfieScan.match, today))
    }

    @Test fun expiredDocument_rejectedUnlessDefect() {
        use(emptySet())
        assertEquals(KycOutcome.Rejected("Document expired"), KycReview.decide(expired, KycSelfieScan.match, today))
        use(setOf(DefectId.kycAcceptsExpiredDocument))
        assertEquals(KycOutcome.Verified, KycReview.decide(expired, KycSelfieScan.match, today))
    }

    @Test fun otherName_rejectedUnlessDefect() {
        use(emptySet())
        assertEquals(KycOutcome.Rejected("Name doesn't match the account holder"),
            KycReview.decide(otherName, KycSelfieScan.match, today))
        use(setOf(DefectId.kycNameMismatchAccepted))
        assertEquals(KycOutcome.Verified, KycReview.decide(otherName, KycSelfieScan.match, today))
    }

    @Test fun mismatchedSelfie_rejectedUnlessDefect() {
        use(emptySet())
        assertEquals(KycOutcome.Rejected("Face doesn't match the document"),
            KycReview.decide(valid, KycSelfieScan.mismatch, today))
        use(setOf(DefectId.kycSelfieMismatchAccepted))
        assertEquals(KycOutcome.Verified, KycReview.decide(valid, KycSelfieScan.mismatch, today))
    }

    @Test fun stuckPending_reportsApprovedCheckAsPending() {
        use(setOf(DefectId.kycStuckPending))
        assertEquals(KycOutcome.Pending, KycReview.decide(valid, KycSelfieScan.match, today))
    }

    // Flow

    @Test fun happyPath_verifiesTheStore() = runTest {
        use(emptySet())
        val store = KycStore(verified = false)
        val flow = KycFlowModel(ScriptedDocumentScanner("valid"), ScriptedSelfieChecker("match"))
        flow.scanDocument()
        assertEquals(KycReview.accountHolder, flow.document?.name)
        flow.takeSelfie()
        assertTrue(flow.canSubmit)
        flow.submit(store)
        assertEquals(KycOutcome.Verified, flow.outcome)
        assertTrue(store.verified)
    }

    @Test fun unreadableDocument_blocksTheSelfie() = runTest {
        use(emptySet())
        val flow = KycFlowModel(ScriptedDocumentScanner("unreadable"), ScriptedSelfieChecker("match"))
        flow.scanDocument()
        assertEquals(false, flow.document?.isReadable)
        assertFalse(flow.canTakeSelfie)
        assertFalse(flow.canSubmit)
    }

    @Test fun noFace_blocksSubmit() = runTest {
        use(emptySet())
        val flow = KycFlowModel(ScriptedDocumentScanner("valid"), ScriptedSelfieChecker("noFace"))
        flow.scanDocument()
        flow.takeSelfie()
        assertEquals(KycSelfieScan.noFace, flow.selfie)
        assertFalse(flow.canSubmit)
    }

    @Test fun rejectedOrPending_leavesTheStoreUnverified() = runTest {
        use(setOf(DefectId.kycStuckPending))
        val store = KycStore(verified = false)
        val flow = KycFlowModel(ScriptedDocumentScanner("valid"), ScriptedSelfieChecker("match"))
        flow.scanDocument()
        flow.takeSelfie()
        flow.submit(store)
        assertEquals(KycOutcome.Pending, flow.outcome)
        assertFalse(store.verified)
    }

    @Test fun unknownScripts_fallBackToTheHappyPath() = runTest {
        val doc = ScriptedDocumentScanner("nonsense").scan(KycDocumentKind.idCard)
        assertTrue(doc.isReadable)
        assertEquals(KycSelfieScan.match, ScriptedSelfieChecker("nonsense").check(doc))
    }

    @Test fun launchOptions_readTheKycScripts() {
        val extras = mapOf(
            "CHAOSBANK_KYC_UNVERIFIED" to "1",
            "CHAOSBANK_KYC_DOCUMENT" to "expired",
            "CHAOSBANK_KYC_SELFIE" to "mismatch",
        )
        val opts = LaunchOptions.from { extras[it] }
        assertTrue(opts.kycUnverified)
        assertEquals("expired", opts.kycDocument)
        assertEquals("mismatch", opts.kycSelfie)
    }
}
