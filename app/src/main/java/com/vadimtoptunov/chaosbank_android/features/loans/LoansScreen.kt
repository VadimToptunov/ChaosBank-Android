package com.vadimtoptunov.chaosbank_android.features.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.core.money.Currency
import com.vadimtoptunov.chaosbank_android.core.money.LoanCalc
import com.vadimtoptunov.chaosbank_android.core.money.Money
import com.vadimtoptunov.chaosbank_android.core.money.MoneyFormat
import com.vadimtoptunov.chaosbank_android.ui.components.CardSurface
import com.vadimtoptunov.chaosbank_android.ui.components.ChaosScreen
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

@Composable
fun LoansScreen() {
    ChaosScreen("Personal loan", A11y.Loans.root, showBadge = false) {
        CardSurface {
            Text("Borrow ${Money(LoanCalc.principal, Currency.EUR).formatted} over ${LoanCalc.months} months", color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            LoanRow("APR", "${MoneyFormat.decimal(LoanCalc.displayedApr(), 1)}%", A11y.Loans.apr)
            LoanRow("Monthly payment", Money(LoanCalc.monthlyPayment(), Currency.EUR).formatted, A11y.Loans.monthly)
            LoanRow("Total repayable", Money(LoanCalc.totalCost(), Currency.EUR).formatted, A11y.Loans.total)
        }
        Text("Representative example. Not a real credit offer.", color = Palette.muted, fontSize = 11.sp)
    }
}

@Composable
private fun LoanRow(label: String, value: String, tag: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Palette.muted, fontSize = 14.sp)
        Text(value, color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, modifier = Modifier.testTag(tag))
    }
}
