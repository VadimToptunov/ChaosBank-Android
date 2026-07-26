package com.vadimtoptunov.chaosbank_android.features.card

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import com.vadimtoptunov.chaosbank_android.R
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects

/**
 * The "views build" rendering of the Card screen: an inflated XML layout hosted via
 * AndroidView, bound against the same [CardViewModel]. Hosts defects characteristic
 * of the Android View system. Locators are exposed as `contentDescription` (the
 * View-system a11y surface), the parallel to the UIKit build's accessibilityIdentifier.
 */
@Composable
fun ViewsCardScreen() {
    val vm = remember { CardViewModel() }
    AndroidView(
        modifier = Modifier.fillMaxSize().testTag(A11y.Card.root),
        factory = { ctx ->
            val root = LayoutInflater.from(ctx).inflate(R.layout.screen_card_views, null)
            root.contentDescription = A11y.Card.root

            val number = root.findViewById<TextView>(R.id.card_number)
            number.text = vm.displayedPAN
            number.contentDescription = A11y.Card.number
            root.findViewById<TextView>(R.id.card_holder).text = "${vm.holder}    ${vm.expiry}"

            val freeze = root.findViewById<Switch>(R.id.card_freeze_toggle)
            val online = root.findViewById<Switch>(R.id.card_online_toggle)
            freeze.contentDescription = A11y.Card.freezeToggle
            online.contentDescription = A11y.Card.onlinePaymentsToggle

            // Correct: bind switches from the model. `toggleInitialStateNotBound`: leave
            // them at the hardcoded default (off), so Online payments (model default: on)
            // wrongly shows off on load. Change handlers are always wired.
            val bound = !Defects.isActive(DefectId.toggleInitialStateNotBound)
            freeze.isChecked = if (bound) vm.frozen else false
            online.isChecked = if (bound) vm.onlinePayments else false
            freeze.setOnCheckedChangeListener { _, v -> vm.frozen = v }
            online.setOnCheckedChangeListener { _, v -> vm.onlinePayments = v }

            val limit = root.findViewById<EditText>(R.id.card_limit_field)
            limit.setText(vm.monthlyLimitText)
            limit.contentDescription = A11y.Card.limitField
            val limitError = root.findViewById<TextView>(R.id.card_limit_error)
            limitError.contentDescription = A11y.Card.limitError
            // inputType="number" already filters non-digits at the IME.
            limit.doAfterTextChanged {
                // Correct: commit the edit to the model (two-way). `fieldEditNotCommitted`:
                // skip it, so the field shows the text but the model — and its validation —
                // keep the old value.
                if (!Defects.isActive(DefectId.fieldEditNotCommitted)) vm.monthlyLimitText = it?.toString().orEmpty()
                val err = vm.limitError
                limitError.text = err ?: ""
                limitError.visibility = if (err == null) View.GONE else View.VISIBLE
            }

            root.findViewById<Button>(R.id.card_pin_button).setOnClickListener {
                AlertDialog.Builder(ctx).setTitle("Card PIN").setMessage("Your PIN is ${vm.pinText}")
                    .setPositiveButton("Done", null).show()
            }
            root.findViewById<Button>(R.id.card_virtual_button).setOnClickListener {
                AlertDialog.Builder(ctx).setTitle("Virtual card").setMessage(vm.virtualCardNumber)
                    .setPositiveButton("Done", null).show()
            }
            root
        },
    )
}
