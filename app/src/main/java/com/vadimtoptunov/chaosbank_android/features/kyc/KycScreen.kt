package com.vadimtoptunov.chaosbank_android.features.kyc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.app.LocalNavigator
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.components.PrimaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.SecondaryButton
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentBar
import com.vadimtoptunov.chaosbank_android.ui.components.SegmentItem
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The identity-verification flow, pushed from the Card tab. One screen whose
 *  sections unlock in order: document → selfie → submit → result. Mirrors iOS `KycView`. */
@Composable
fun KycScreen() {
    val services = LocalAppServices.current
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val flow = remember { KycFlowModel() }

    ChaosScreen("Verify identity", A11y.Kyc.root, showBadge = false) {
        val outcome = flow.outcome
        if (outcome != null) {
            CardSurface {
                when (outcome) {
                    KycOutcome.Verified -> ResultTitle("Identity verified", Palette.gain)
                    KycOutcome.Pending -> {
                        ResultTitle("Under review", Palette.sand)
                        Reason("We'll let you know when the check is complete.")
                    }
                    is KycOutcome.Rejected -> {
                        ResultTitle("Verification failed", Palette.loss)
                        Reason(outcome.reason)
                    }
                }
                PrimaryButton("Done", Modifier.testTag(A11y.Kyc.doneButton)) { nav.pop() }
            }
        } else {
            CardSurface {
                Text("1. Your document", color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                SegmentBar(
                    items = KycDocumentKind.entries.map {
                        SegmentItem(it.name, it.title,
                            if (it == KycDocumentKind.passport) A11y.Kyc.docPassport else A11y.Kyc.docIdCard)
                    },
                    selected = flow.kind.name,
                ) { id -> flow.kind = KycDocumentKind.valueOf(id) }
                SecondaryButton(if (flow.document == null) "Scan document" else "Scan again",
                    Modifier.testTag(A11y.Kyc.scanButton)) { scope.launch { flow.scanDocument() } }

                flow.document?.let { doc ->
                    if (doc.isReadable) {
                        Field("Name", doc.name.orEmpty(), A11y.Kyc.docName)
                        Field("Number", doc.number.orEmpty(), A11y.Kyc.docNumber)
                        Field("Expires", doc.expiry?.let(::dateText).orEmpty(), A11y.Kyc.docExpiry)
                    } else {
                        Text("Couldn't read the document — try again in better light",
                            color = Palette.loss, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag(A11y.Kyc.scanError))
                    }
                }
            }

            if (flow.document?.isReadable == true) {
                CardSurface {
                    Text("2. A quick selfie", color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    SecondaryButton(if (flow.selfie == null) "Take selfie" else "Retake selfie",
                        Modifier.testTag(A11y.Kyc.selfieButton)) { scope.launch { flow.takeSelfie() } }
                    flow.selfie?.let { selfie ->
                        Text(selfie.message,
                            color = if (selfie == KycSelfieScan.match) Palette.gain else Palette.loss,
                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag(A11y.Kyc.selfieResult))
                    }
                }
            }

            PrimaryButton("Submit for review", Modifier.testTag(A11y.Kyc.submitButton), enabled = flow.canSubmit) {
                scope.launch { flow.submit(services.kyc) }
            }
            if (flow.isWorking) CircularProgressIndicator(color = Palette.sand)
        }
    }
}

@Composable
private fun ResultTitle(text: String, color: Color) {
    Text(text, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag(A11y.Kyc.resultTitle))
}

@Composable
private fun Reason(text: String) {
    Text(text, color = Palette.muted, fontSize = 14.sp, modifier = Modifier.testTag(A11y.Kyc.resultReason))
}

@Composable
private fun Field(label: String, value: String, tag: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Palette.muted, fontSize = 13.sp)
        Text(value, color = Palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace, modifier = Modifier.testTag(tag))
    }
}

private fun dateText(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))
