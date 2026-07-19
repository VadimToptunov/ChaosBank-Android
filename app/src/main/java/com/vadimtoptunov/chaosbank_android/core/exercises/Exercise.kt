package com.vadimtoptunov.chaosbank_android.core.exercises

import com.vadimtoptunov.chaosbank_android.core.defects.DefectCategory
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.DefectRegistry

/**
 * The machine-readable training catalog: one exercise per defect. Fields are
 * derived from [DefectRegistry] where possible so the catalog can't drift from the
 * actual defects; per-defect guidance comes from the curated [Exercises.specs].
 */
data class Exercise(
    val id: String,
    val title: String,
    val difficulty: String,
    val category: String,
    val feature: String,
    val defects: List<String>,
    val launchArgument: String,
    val profile: String?,
    val condition: String,
    val expectedClean: String,
    val expectedBuggy: String,
    val task: String,
    val keyLocators: List<String>,
)

object Exercises {
    private data class Spec(
        val difficulty: String,
        val task: String,
        val expectedClean: String,
        val expectedBuggy: String,
        val locators: List<String>,
    )

    private fun code(c: DefectCategory): String = when (c) {
        DefectCategory.Money -> "MON"
        DefectCategory.Validation -> "VAL"
        DefectCategory.Localization -> "LOC"
        DefectCategory.State -> "STA"
        DefectCategory.Concurrency -> "CON"
        DefectCategory.Ui -> "UI"
        DefectCategory.Accessibility -> "A11Y"
        DefectCategory.Security -> "SEC"
        DefectCategory.Network -> "NET"
        DefectCategory.Performance -> "PERF"
    }

    private fun slug(c: DefectCategory): String = when (c) {
        DefectCategory.Money -> "money"
        DefectCategory.Validation -> "validation"
        DefectCategory.Localization -> "localization"
        DefectCategory.State -> "state"
        DefectCategory.Concurrency -> "concurrency"
        DefectCategory.Ui -> "ui"
        DefectCategory.Accessibility -> "accessibility"
        DefectCategory.Security -> "security"
        DefectCategory.Network -> "network"
        DefectCategory.Performance -> "performance"
    }

    private val specs: Map<DefectId, Spec> = mapOf(
        DefectId.roundingDrift to Spec("middle",
            "Exchange EUR→USD and assert the credited history amount equals the displayed 'You get'.",
            "Stored amount == displayed amount.", "Stored amount drifts (Double rounding).",
            listOf("exchange.youGet", "exchange.executeButton", "exchange.successToast")),
        DefectId.pnlSign to Spec("junior",
            "Open Portfolio and assert TSLA's P&L renders as a loss (negative, red).",
            "Loss shows negative / loss color.", "Loss shows positive / gain color.",
            listOf("portfolio.holding.TSLA.pnl", "portfolio.pnl")),
        DefectId.limitValidation to Spec("middle",
            "Set qty to 0 and assert Review is disabled; place a below-market limit sell and assert a warning shows.",
            "Zero qty blocked; below-market limit sell warns.", "Zero qty accepted; no warning.",
            listOf("order.qtyStepper.value", "order.reviewButton", "order.warning")),
        DefectId.zeroAmountAccepted to Spec("junior",
            "Enter recipient and amount 0; assert Continue is disabled.",
            "Continue disabled at amount 0.", "Continue enabled at amount 0.",
            listOf("transfer.amountField", "transfer.continueButton")),
        DefectId.whitespaceRecipient to Spec("junior",
            "Enter a spaces-only recipient; assert Continue stays disabled.",
            "Whitespace trimmed → recipient empty → disabled.", "Spaces accepted as a valid recipient.",
            listOf("transfer.recipientField", "transfer.continueButton")),
        DefectId.passcodeWeakAccepted to Spec("middle",
            "During passcode setup, assert a 4-digit passcode is rejected (6 required).",
            "Requires 6 digits.", "Accepts 4 digits.",
            listOf("auth.passcodeField", "auth.passcodeSubmit", "auth.passcodeError")),
        DefectId.localeParse to Spec("middle",
            "Type '1,000.50' into an amount field under en_US; assert it parses to 1000.50.",
            "Parses to 1000.50.", "Collapses toward 1.0005.",
            listOf("transfer.amountField", "transfer.balanceAfter", "exchange.amountField")),
        DefectId.dateTimezoneShift to Spec("middle",
            "Open Transactions and assert a known transaction's day/time matches the home timezone.",
            "Dates in the app's home timezone.", "Dates shifted to a far-off timezone.",
            listOf("transactions.list", "transactions.row.t01")),
        DefectId.staleBalance to Spec("middle",
            "Note the Home balance, make a transfer, return to Home; assert the balance decreased.",
            "Balance refreshes after the transfer.", "Home keeps showing the pre-transfer balance.",
            listOf("home.totalBalance", "home.quickAction.transfer", "transfer.confirmButton")),
        DefectId.paginationDup to Spec("middle",
            "Tap Load more and assert each transaction id appears exactly once.",
            "No duplicates after Load more.", "A boundary transaction appears twice.",
            listOf("transactions.loadMore", "transactions.list")),
        DefectId.cardToggleInvert to Spec("junior",
            "Turn on Freeze; assert the FROZEN badge appears.",
            "Freeze state reads back correctly.", "Freeze reads back inverted.",
            listOf("card.freezeToggle", "card.frozenBadge")),
        DefectId.doubleCharge to Spec("senior",
            "Rapidly double-tap Confirm; assert exactly one transaction / one debit.",
            "Idempotent — one transaction.", "Two transactions (double charge).",
            listOf("transfer.confirmButton", "home.totalBalance")),
        DefectId.livePriceRace to Spec("senior",
            "Read the asset price, tap Buy, and assert the order's reference price equals it.",
            "Order price == tapped price.", "Order re-reads live price → differs.",
            listOf("asset.price", "asset.buyButton", "order.refPrice")),
        DefectId.orderDoubleSubmit to Spec("senior",
            "Double-tap Place order; assert only one order/holding change occurs.",
            "Idempotent — one order.", "Two orders placed.",
            listOf("order.placeButton", "portfolio.holding.AAPL")),
        DefectId.disabledButtonTappable to Spec("junior",
            "With an invalid form, assert Continue reports isEnabled == false.",
            "Disabled-looking button is non-interactive.", "Button looks disabled but is hittable.",
            listOf("transfer.continueButton")),
        DefectId.otpResendNoCooldown to Spec("middle",
            "On the OTP screen, assert Resend is disabled until the cooldown elapses.",
            "Resend blocked during cooldown.", "Resend fires during cooldown.",
            listOf("auth.otpResend", "auth.otpExpiry")),
        DefectId.duplicateAssetA11yId to Spec("middle",
            "In Markets, assert each asset identifier matches exactly one element.",
            "Every row has a unique identifier.", "NVDA collides onto AAPL's identifier.",
            listOf("markets.asset.AAPL", "markets.asset.NVDA")),
        DefectId.authBypass to Spec("senior",
            "Unlock, background then foreground the app; assert re-auth is required.",
            "Background re-locks to passcode entry.", "App stays unlocked after backgrounding.",
            listOf("auth.passcode", "tabBar.home")),
        DefectId.noPrivacyBlur to Spec("senior",
            "Send the app to the inactive/switcher state; assert the privacy cover appears.",
            "Sensitive content is covered when inactive.", "Balances/card leak into the switcher.",
            listOf("privacy.cover")),
        DefectId.otpAcceptsExpired to Spec("senior",
            "Let the OTP expire (>20s), enter the code; assert it is rejected.",
            "Expired code rejected.", "Expired code accepted.",
            listOf("auth.otpField", "auth.otpError", "auth.otpExpiry")),
        DefectId.otpNoLockout to Spec("middle",
            "Enter a wrong code 3×; assert the OTP step locks.",
            "Locks after 3 wrong attempts.", "No lockout — brute-forceable.",
            listOf("auth.otpField", "auth.otpError")),
        DefectId.sessionTimeoutDisabled to Spec("senior",
            "Leave the app idle past the timeout; assert it re-locks.",
            "Idle session re-locks.", "Session never times out.",
            listOf("auth.passcode", "tabBar.home")),
        DefectId.tokenInUserDefaults to Spec("middle",
            "After login, inspect storage; assert the session token is NOT in SharedPreferences.",
            "Token in Keystore only.", "Token written to SharedPreferences.",
            listOf("dev.tokenStorage")),
        DefectId.retryDuplicate to Spec("senior",
            "Trigger a timeout, tap Retry; assert only one transaction/debit results.",
            "Retry is idempotent (same key).", "Retry double-posts the payment.",
            listOf("transfer.retryButton", "transfer.confirmButton", "home.totalBalance")),
        DefectId.slowResponseRace to Spec("senior",
            "Switch the sell currency quickly; assert the balance shows the newest currency.",
            "Stale late response is dropped.", "Stale response clobbers the fresh balance.",
            listOf("exchange.sellCurrency", "exchange.amountField")),
        DefectId.timeoutAsSuccess to Spec("middle",
            "Make a transfer that times out; assert an error (not success) is shown.",
            "Timeout surfaced as an error.", "Timeout shown as success.",
            listOf("transfer.confirmButton", "transfer.successToast", "transfer.error")),
        DefectId.staleOfflineBalance to Spec("middle",
            "Mutate a balance and refresh; assert reads reflect the change.",
            "Reads reflect the latest state.", "Reads keep serving a stale snapshot.",
            listOf("home.totalBalance", "home.account.EUR")),
        DefectId.transactionsHeavyList to Spec("middle",
            "Open Transactions and assert scrolling stays responsive / paginated.",
            "Lazy, paginated, smooth.", "Whole huge list rendered at once → hitches.",
            listOf("transactions.list", "transactions.loadMore")),
        DefectId.mainThreadStall to Spec("middle",
            "Open Portfolio and assert the tab becomes responsive within a small budget.",
            "No main-thread hang on open.", "UI blocks ~1.2s on open.",
            listOf("portfolio.root", "portfolio.totalValue")),
        DefectId.exchangeFeeNotApplied to Spec("middle",
            "Exchange and assert the credited amount matches the fee-deducted 'You get'.",
            "Credited amount reflects the displayed fee.", "Fee ignored — credited amount is higher than shown.",
            listOf("exchange.fee", "exchange.youGet", "exchange.executeButton")),
        DefectId.pnlPercentVsValue to Spec("middle",
            "Assert portfolio P&L % equals P&L ÷ cost basis.",
            "Percent measured against cost basis.", "Percent measured against market value.",
            listOf("portfolio.pnl", "portfolio.totalValue")),
        DefectId.homeTotalOmitsAccount to Spec("junior",
            "Assert Home total equals the sum of all three accounts.",
            "Total = EUR + USD + GBP.", "GBP account omitted from the total.",
            listOf("home.totalBalance", "home.account.GBP")),
        DefectId.amountExceedsBalanceAllowed to Spec("junior",
            "Enter an amount above the balance; assert Continue is disabled.",
            "Over-balance amount blocks Continue.", "Continue enabled beyond the balance.",
            listOf("transfer.amountField", "transfer.continueButton", "transfer.balanceAfter")),
        DefectId.filterLeaksCategory to Spec("middle",
            "Select the Money-in filter; assert every visible row is money-in.",
            "Filter shows only money-in rows.", "Money-out rows leak through.",
            listOf("transactions.filter.in", "transactions.list")),
        DefectId.orderStuckPending to Spec("middle",
            "Place a market order; assert the status reports filled.",
            "Order reports filled.", "Order stuck showing pending.",
            listOf("order.placeButton", "order.statusToast")),
        DefectId.exchangeDoubleSubmit to Spec("senior",
            "Double-tap Exchange; assert only one exchange executes.",
            "Idempotent — one exchange.", "Two exchanges executed.",
            listOf("exchange.executeButton", "home.account.USD")),
        DefectId.feedPollsTooOften to Spec("middle",
            "In live mode, assert the quote endpoint isn't polled excessively.",
            "Polls at a sane interval (~3s).", "Polls ~10× too often.",
            listOf("markets.liveBadge")),
        DefectId.transactionsSortEveryRender to Spec("middle",
            "Profile Transactions rendering; assert the list isn't re-sorted each render.",
            "Sort computed once.", "Whole list re-sorted on every render.",
            listOf("transactions.list")),
        DefectId.successToastMissing to Spec("junior",
            "Complete a transfer; assert the success toast appears.",
            "Confirmation toast shown.", "No confirmation toast.",
            listOf("transfer.confirmButton", "transfer.successToast")),
        DefectId.missingA11yLabel to Spec("middle",
            "Assert the Place-order button exposes a non-empty accessibility label.",
            "Label is 'Place order'.", "Label is empty.",
            listOf("order.placeButton")),
        DefectId.wrongA11yLabel to Spec("middle",
            "Assert the Buy button's accessibility label reads 'Buy'.",
            "Label matches the action ('Buy').", "Label says 'Sell'.",
            listOf("asset.buyButton")),
        DefectId.cardNumberFullyVisible to Spec("junior",
            "Assert the card number is masked except the last four digits.",
            "Masked PAN.", "Full PAN visible.",
            listOf("card.number")),
        DefectId.cardCvvVisible to Spec("junior",
            "Assert no CVV is shown on the card face.",
            "CVV never displayed.", "CVV printed on the card.",
            listOf("card.cvv", "card.visual")),
        DefectId.otpCodeInLog to Spec("senior",
            "Inspect the console during OTP; assert the code is never logged.",
            "No secret in logs.", "OTP code written to the console.",
            listOf("auth.otpField")),
        DefectId.flakyAnimation to Spec("senior",
            "Open Markets in live mode and assert a price cell settles to its neutral colour within a bounded time on every tick.",
            "Flash animation settles within a stable, bounded duration.", "Settle time jitters per tick — a wait-for-idle step flakes.",
            listOf("markets.asset.AAPL.price", "markets.liveBadge")),
        DefectId.offlineBannerMissing to Spec("middle",
            "Enable offline mode (dev menu) and assert the offline banner is shown on the current screen.",
            "Offline banner visible while offline.", "No banner — the app serves cached data silently.",
            listOf("dev.offlineToggle", "net.offlineBanner")),
    )

    val all: List<Exercise> = run {
        val counters = mutableMapOf<DefectCategory, Int>()
        DefectId.entries.map { id ->
            val defect = DefectRegistry.defect(id)!!
            val n = (counters[defect.category] ?: 0) + 1
            counters[defect.category] = n
            val spec = specs[id]
            Exercise(
                id = "AND-${code(defect.category)}-${"%02d".format(n)}",
                title = defect.title,
                difficulty = spec?.difficulty ?: "middle",
                category = slug(defect.category),
                feature = defect.feature,
                defects = listOf(id.name),
                launchArgument = "CHAOSBANK_DEFECTS=${id.name}",
                profile = null,
                condition = defect.violates,
                expectedClean = spec?.expectedClean ?: "Behaves per the requirement.",
                expectedBuggy = spec?.expectedBuggy ?: "Requirement violated.",
                task = spec?.task ?: "Reproduce the defect and write a stable test that fails only when active.",
                keyLocators = spec?.locators ?: emptyList(),
            )
        }
    }
}
