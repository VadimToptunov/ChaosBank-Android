package com.vadimtoptunov.chaosbank_android.features.kyc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vadimtoptunov.chaosbank_android.app.KycStore
import com.vadimtoptunov.chaosbank_android.app.LaunchOptions
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import kotlinx.coroutines.delay
import java.time.LocalDate

/*
 * Mock identity verification (KYC): choose a document → "scan" it → take a "selfie"
 * → submit → verified / rejected / pending. The emulator has no camera worth using,
 * so the scanner and the face check sit behind small interfaces whose default
 * implementations replay what the launch options ask for (`CHAOSBANK_KYC_DOCUMENT`,
 * `CHAOSBANK_KYC_SELFIE`). A real app would put its scanning SDK behind the same seam.
 *
 * The review rules are written correctly; four defects override them at single,
 * guarded points: `kycAcceptsExpiredDocument`, `kycNameMismatchAccepted`,
 * `kycSelfieMismatchAccepted`, `kycStuckPending`. Mirrors iOS `KycFlow.swift`.
 */

enum class KycDocumentKind(val title: String) { passport("Passport"), idCard("ID card") }

/** What the document scanner read. `null` fields mean the scan was unreadable. */
data class KycDocumentScan(val name: String?, val number: String?, val expiry: LocalDate?) {
    val isReadable: Boolean get() = name != null && number != null && expiry != null
}

enum class KycSelfieScan(val message: String) {
    match("Face matches the document"),
    mismatch("Face doesn't match the document"),
    noFace("No face detected — try again"),
}

fun interface KycDocumentScanning { suspend fun scan(kind: KycDocumentKind): KycDocumentScan }
fun interface KycSelfieChecking { suspend fun check(document: KycDocumentScan): KycSelfieScan }

/** Replays `CHAOSBANK_KYC_DOCUMENT`: valid | expired | unreadable | nameMismatch. */
class ScriptedDocumentScanner(private val script: String) : KycDocumentScanning {
    override suspend fun scan(kind: KycDocumentKind): KycDocumentScan {
        delay(600)
        val number = if (kind == KycDocumentKind.passport) "P4916204" else "ID7731205"
        return when (script) {
            "expired" -> KycDocumentScan(KycReview.accountHolder, number, LocalDate.of(2024, 1, 31))
            "unreadable" -> KycDocumentScan(null, null, null)
            "nameMismatch" -> KycDocumentScan("ALEX MORGAN", number, LocalDate.of(2031, 4, 30))
            else -> KycDocumentScan(KycReview.accountHolder, number, LocalDate.of(2031, 4, 30))
        }
    }
}

/** Replays `CHAOSBANK_KYC_SELFIE`: match | mismatch | noFace. */
class ScriptedSelfieChecker(private val script: String) : KycSelfieChecking {
    override suspend fun check(document: KycDocumentScan): KycSelfieScan {
        delay(600)
        return KycSelfieScan.entries.firstOrNull { it.name == script } ?: KycSelfieScan.match
    }
}

sealed interface KycOutcome {
    data object Verified : KycOutcome
    data class Rejected(val reason: String) : KycOutcome
    data object Pending : KycOutcome
}

object KycReview {
    /** The account holder's name as it appears on their documents. */
    const val accountHolder = "VADIM TOPTUNOV"

    /** The decision on a readable document and a detected face. */
    fun decide(document: KycDocumentScan, selfie: KycSelfieScan, today: LocalDate = LocalDate.now()): KycOutcome {
        // `kycAcceptsExpiredDocument`: the expiry date isn't checked.
        val expiry = document.expiry
        if (expiry != null && expiry.isBefore(today) && !Defects.isActive(DefectId.kycAcceptsExpiredDocument)) {
            return KycOutcome.Rejected("Document expired")
        }
        // `kycNameMismatchAccepted`: the document's name isn't compared with the account holder.
        if (document.name != accountHolder && !Defects.isActive(DefectId.kycNameMismatchAccepted)) {
            return KycOutcome.Rejected("Name doesn't match the account holder")
        }
        // `kycSelfieMismatchAccepted`: a face that doesn't match still passes.
        if (selfie == KycSelfieScan.mismatch && !Defects.isActive(DefectId.kycSelfieMismatchAccepted)) {
            return KycOutcome.Rejected("Face doesn't match the document")
        }
        // `kycStuckPending`: an approved check is reported as still under review.
        return if (Defects.isActive(DefectId.kycStuckPending)) KycOutcome.Pending else KycOutcome.Verified
    }
}

class KycFlowModel(
    private val scanner: KycDocumentScanning = ScriptedDocumentScanner(LaunchOptions.current.kycDocument),
    private val faceCheck: KycSelfieChecking = ScriptedSelfieChecker(LaunchOptions.current.kycSelfie),
) {
    var kind by mutableStateOf(KycDocumentKind.passport)
    var document by mutableStateOf<KycDocumentScan?>(null); private set
    var selfie by mutableStateOf<KycSelfieScan?>(null); private set
    var outcome by mutableStateOf<KycOutcome?>(null); private set
    var isWorking by mutableStateOf(false); private set

    val canTakeSelfie: Boolean get() = document?.isReadable == true && !isWorking

    /** Submit needs a readable document and a detected face. */
    val canSubmit: Boolean
        get() = canTakeSelfie && (selfie == KycSelfieScan.match || selfie == KycSelfieScan.mismatch)

    suspend fun scanDocument() {
        isWorking = true
        document = scanner.scan(kind)
        selfie = null
        isWorking = false
    }

    suspend fun takeSelfie() {
        val doc = document ?: return
        if (!doc.isReadable) return
        isWorking = true
        selfie = faceCheck.check(doc)
        isWorking = false
    }

    suspend fun submit(store: KycStore) {
        val doc = document ?: return
        val face = selfie ?: return
        if (!canSubmit) return
        isWorking = true
        delay(800)
        val result = KycReview.decide(doc, face)
        outcome = result
        if (result == KycOutcome.Verified) store.applyVerified(true)
        isWorking = false
    }
}
