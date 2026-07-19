package com.vadimtoptunov.chaosbank_android.core.defects

/**
 * The stable identity of every deliberate defect. The enum constant name is the
 * canonical id used verbatim in launch arguments (`-ChaosBankDefects doubleCharge`).
 * Adding a value here and to [DefectRegistry] is the only way a new bug enters the app.
 */
enum class DefectId {
    // Money / logic
    roundingDrift,
    pnlSign,
    exchangeFeeNotApplied,
    pnlPercentVsValue,
    homeTotalOmitsAccount,
    balanceFloorRounded,
    todayChangeSignFlipped,
    transferDebitsWrongAccount,
    balanceAfterAdds,
    transferRoundsUp,
    exchangeInverseRate,
    exchangeCreditsWrongAccount,
    exchangeFeeDoubled,
    youGetShowsGross,
    changePctSignFlipped,
    detailPriceOffset,
    detailChangeWrongBase,
    estTotalIgnoresQty,
    limitExecutesAtMarket,
    holdingValueUsesCost,
    totalValueOmitsHolding,
    pnlPercentAbsOnly,

    // Validation
    limitValidation,
    zeroAmountAccepted,
    whitespaceRecipient,
    passcodeWeakAccepted,
    amountExceedsBalanceAllowed,
    transferNegativeCredits,
    exchangeSameCurrencyAllowed,
    searchCaseSensitive,
    sellWithoutHoldingReviewable,
    orderQtyDefaultsZero,
    loginAcceptsEmptyCreds,
    cardLimitAcceptsZero,

    // Localization
    localeParse,
    dateTimezoneShift,
    balanceWrongCurrencySymbol,
    priceMissingDecimals,
    searchTrimsNothing,

    // State / navigation
    staleBalance,
    paginationDup,
    paginationNeverEnds,
    cardToggleInvert,
    filterLeaksCategory,
    orderStuckPending,
    quickActionTransferOpensExchange,
    recentActivityShowsTwo,
    exchangeRateStaleAfterSwap,
    filterOutLeaksIn,
    searchIgnoresCategory,
    cryptoShownInStocks,
    watchlistShowsAll,
    assetRowOpensWrongDetail,
    buyButtonPlacesSell,
    buySellSwapped,
    onlinePaymentsInverted,
    transferConfirmWrongRecipient,
    notificationBadgeStale,
    notificationOpensWrongScreen,

    // Concurrency / races
    doubleCharge,
    livePriceRace,
    orderDoubleSubmit,
    exchangeDoubleSubmit,
    homeRefreshRace,
    syncLostUpdate,

    // Performance
    transactionsHeavyList,
    mainThreadStall,
    feedPollsTooOften,
    transactionsSortEveryRender,
    sparklineHeavyPoints,
    transactionsRegroupHeavy,

    // UI / layout
    disabledButtonTappable,
    otpResendNoCooldown,
    successToastMissing,
    accountStripHidesGBP,
    outgoingSignHidden,
    transactionCountWrong,
    detailStatHighLowSwapped,
    qtyIncrementByTwo,
    cardExpiryInPast,
    otpAutoFillsCode,
    successToastTooBrief,
    flakyAnimation,

    // Accessibility
    duplicateAssetA11yId,
    missingA11yLabel,
    wrongA11yLabel,
    marketRowNoLabel,
    freezeToggleNoLabel,

    // Security / privacy
    authBypass,
    noPrivacyBlur,
    otpAcceptsExpired,
    otpNoLockout,
    sessionTimeoutDisabled,
    tokenInUserDefaults,
    cardNumberFullyVisible,
    cardCvvVisible,
    otpCodeInLog,
    pinShownPlaintext,
    otpAcceptsAnyCode,
    passcodeAnyAccepted,
    passcodeStoredPlaintext,
    credentialsInLog,
    deepLinkSkipsAuth,
    biometricUnlocksFromAnyStage,

    // Network (scenario-driven backend)
    retryDuplicate,
    slowResponseRace,
    timeoutAsSuccess,
    staleOfflineBalance,
    balanceReadReturnsZero,
    transactionsDupOnFetch,
    staleHoldingsAfterOrder,
    offlineBannerMissing;

    companion object {
        fun from(raw: String): DefectId? = entries.firstOrNull { it.name == raw.trim() }
    }
}
