package com.vadimtoptunov.chaosbank_android.features.exchange

import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
 * The "views build" rendering of the Exchange form: an inflated XML layout hosted
 * via AndroidView, bound against the same ExchangeViewModel. The 'You get' field
 * hosts the outputNotRecomputed defect.
 */
@Composable
fun ViewsExchangeScreen() {
    val services = LocalAppServices.current
    val vm = remember { ExchangeViewModel(services) }
    LaunchedEffect(Unit) { vm.load() }

    AndroidView(
        modifier = Modifier.fillMaxSize().testTag(A11y.Exchange.root),
        factory = { ctx ->
            val root = LayoutInflater.from(ctx).inflate(R.layout.screen_exchange_views, null)
            root.contentDescription = A11y.Exchange.root
            root.findViewById<TextView>(R.id.exchange_title).text = "${vm.sell} → ${vm.get}"

            val amount = root.findViewById<EditText>(R.id.exchange_amount)
            val rate = root.findViewById<TextView>(R.id.exchange_rate)
            val fee = root.findViewById<TextView>(R.id.exchange_fee)
            val youGet = root.findViewById<TextView>(R.id.exchange_you_get)
            val execute = root.findViewById<Button>(R.id.exchange_execute)
            amount.contentDescription = A11y.Exchange.amountField
            rate.contentDescription = A11y.Exchange.rate
            fee.contentDescription = A11y.Exchange.fee
            youGet.contentDescription = A11y.Exchange.youGet
            execute.contentDescription = A11y.Exchange.executeButton

            // Initial values always render; only the recompute-on-change is defective.
            rate.text = "Rate ${vm.rate}"
            youGet.text = vm.youGet.formatted
            fee.text = "Fee ${vm.fee.formatted}"

            amount.doAfterTextChanged {
                vm.amountText = it?.toString().orEmpty()
                // Correct: recompute the derived fields. `outputNotRecomputed`: skip it,
                // so 'You get' stays at its initial value regardless of the amount.
                if (!Defects.isActive(DefectId.outputNotRecomputed)) {
                    youGet.text = vm.youGet.formatted
                    fee.text = "Fee ${vm.fee.formatted}"
                }
            }
            execute.setOnClickListener {
                AlertDialog.Builder(ctx).setTitle("Exchange").setMessage("You get ${vm.youGet.formatted}")
                    .setPositiveButton("Done", null).show()
            }
            root
        },
    )
}
