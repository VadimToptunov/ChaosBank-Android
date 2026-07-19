package com.vadimtoptunov.chaosbank_android.app

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import java.math.BigDecimal

/** A saved payment template (payee + amount) shown on the Transfer screen. */
data class PaymentTemplate(
    val id: String,
    val name: String,
    val recipient: String,
    val amount: BigDecimal,
)

/**
 * Saved payment templates (banking-breadth cluster). Applying a template prefills the
 * Transfer form. The `templatePrefillsWrongAmount` defect mangles the prefilled amount.
 */
class TemplateStore {
    val templates: List<PaymentTemplate> = listOf(
        PaymentTemplate("t1", "Rent", "Landlord GmbH", BigDecimal("1200.00")),
        PaymentTemplate("t2", "Alex", "Alex Müller", BigDecimal("50.00")),
        PaymentTemplate("t3", "Savings", "My Savings", BigDecimal("300.00")),
    )

    /** The amount to prefill when a template is applied. */
    fun prefillAmount(template: PaymentTemplate): BigDecimal =
        if (Defects.isActive(DefectId.templatePrefillsWrongAmount)) template.amount * BigDecimal.TEN
        else template.amount
}
