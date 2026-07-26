package com.vadimtoptunov.chaosbank_android.features.transfer

import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import com.vadimtoptunov.chaosbank_android.R
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.ui.LocalAppServices

/**
 * The "views build" rendering of the Transfer form: an inflated XML layout hosted
 * via AndroidView, bound against the same TransferViewModel. The Continue button's
 * enabled state hosts the submitEnabledWhenInvalid defect.
 */
@Composable
fun ViewsTransferScreen() {
    val services = LocalAppServices.current
    val vm = remember { TransferViewModel(services) }
    LaunchedEffect(Unit) { vm.load() }

    AndroidView(
        modifier = Modifier.fillMaxSize().testTag(A11y.Transfer.root),
        factory = { ctx ->
            val root = LayoutInflater.from(ctx).inflate(R.layout.screen_transfer_views, null)
            root.contentDescription = A11y.Transfer.root
            val recipient = root.findViewById<EditText>(R.id.transfer_recipient)
            val amount = root.findViewById<EditText>(R.id.transfer_amount)
            val note = root.findViewById<EditText>(R.id.transfer_note)
            val cont = root.findViewById<Button>(R.id.transfer_continue)
            recipient.contentDescription = A11y.Transfer.recipientField
            amount.contentDescription = A11y.Transfer.amountField
            note.contentDescription = A11y.Transfer.noteField
            cont.contentDescription = A11y.Transfer.continueButton

            fun refresh() {
                // Correct: the button tracks form validity. `submitEnabledWhenInvalid`:
                // leave it enabled regardless, so an invalid form can still be submitted.
                cont.isEnabled = if (Defects.isActive(DefectId.submitEnabledWhenInvalid)) true else vm.canContinue
            }
            recipient.doAfterTextChanged { vm.recipient = it?.toString().orEmpty(); refresh() }
            amount.doAfterTextChanged { vm.amountText = it?.toString().orEmpty(); refresh() }
            note.doAfterTextChanged { vm.note = it?.toString().orEmpty() }
            refresh()

            cont.setOnClickListener {
                AlertDialog.Builder(ctx)
                    .setTitle("Confirm transfer")
                    .setMessage("Send ${vm.amountText} to ${vm.effectiveRecipient}?")
                    .setPositiveButton("Send", null)
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            root
        },
    )
}
