package com.vadimtoptunov.chaosbank_android.core

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.money.Currency

/**
 * THE single source of truth for test tags (Compose `Modifier.testTag`). Format
 * `screen.element[.qualifier]`, stable across profiles — a defect never renames one.
 */
object A11y {
    object TabBar {
        const val root = "tabBar.root"
        const val home = "tabBar.home"
        const val markets = "tabBar.markets"
        const val portfolio = "tabBar.portfolio"
        const val card = "tabBar.card"
    }

    object Build { const val badge = "build.badge" }

    object Nav { const val back = "nav.back" }

    object Dev {
        const val menu = "dev.menu"
        const val close = "dev.close"
        const val activeLabel = "dev.activeLabel"
        const val priceSource = "dev.priceSource"
        const val tokenStorage = "dev.tokenStorage"
        const val exercises = "dev.exercises"
        const val exercisesList = "dev.exercises.list"
        fun profile(id: String) = "dev.profile.$id"
        fun defectToggle(id: DefectId) = "dev.defect.${id.name}"
        fun priceSourceOption(kind: String) = "dev.priceSource.$kind"
        fun exercise(id: String) = "dev.exercise.$id"
        fun exerciseApply(id: String) = "dev.exercise.$id.apply"
    }

    object Privacy { const val cover = "privacy.cover" }

    object Auth {
        const val gate = "auth.gate"
        const val loginRoot = "auth.login"
        const val loginError = "auth.loginError"
        const val webLoginButton = "auth.webLoginButton"
        const val webSheet = "auth.webSheet"
        const val webCancel = "auth.webCancel"
        const val webUsernameId = "web-username"
        const val webPasswordId = "web-password"
        const val webSubmitId = "web-submit"
        const val otpRoot = "auth.otp"
        const val otpField = "auth.otpField"
        const val otpSubmit = "auth.otpSubmit"
        const val otpResend = "auth.otpResend"
        const val otpError = "auth.otpError"
        const val otpHint = "auth.otpHint"
        const val otpExpiry = "auth.otpExpiry"
        const val passcodeRoot = "auth.passcode"
        const val passcodeField = "auth.passcodeField"
        const val passcodeSubmit = "auth.passcodeSubmit"
        const val passcodeError = "auth.passcodeError"
        const val biometricButton = "auth.biometricButton"
    }

    object Home {
        const val root = "home.root"
        const val totalBalance = "home.totalBalance"
        const val todayChange = "home.todayChange"
        const val currencySegment = "home.currencySegment"
        const val quickActionTransfer = "home.quickAction.transfer"
        const val quickActionExchange = "home.quickAction.exchange"
        const val quickActionAddMoney = "home.quickAction.addMoney"
        const val quickActionCard = "home.quickAction.card"
        const val recentActivity = "home.recentActivity"
        const val seeAllActivity = "home.seeAllActivity"
        fun account(c: Currency) = "home.account.${c.code}"
        fun activityRow(id: String) = "home.activity.$id"
    }

    object Transfer {
        const val root = "transfer.root"
        const val recipientField = "transfer.recipientField"
        const val amountField = "transfer.amountField"
        const val noteField = "transfer.noteField"
        const val balanceAfter = "transfer.balanceAfter"
        const val continueButton = "transfer.continueButton"
        const val confirmSheet = "transfer.confirmSheet"
        const val confirmButton = "transfer.confirmButton"
        const val retryButton = "transfer.retryButton"
        const val successToast = "transfer.successToast"
        const val error = "transfer.error"
    }

    object Exchange {
        const val root = "exchange.root"
        const val sellCurrency = "exchange.sellCurrency"
        const val getCurrency = "exchange.getCurrency"
        const val amountField = "exchange.amountField"
        const val rate = "exchange.rate"
        const val fee = "exchange.fee"
        const val youGet = "exchange.youGet"
        const val executeButton = "exchange.executeButton"
        const val successToast = "exchange.successToast"
    }

    object Transactions {
        const val root = "transactions.root"
        const val searchField = "transactions.searchField"
        const val filterAll = "transactions.filter.all"
        const val filterIn = "transactions.filter.in"
        const val filterOut = "transactions.filter.out"
        const val list = "transactions.list"
        const val loadMore = "transactions.loadMore"
        const val count = "transactions.count"
        fun row(id: String) = "transactions.row.$id"
    }

    object Markets {
        const val root = "markets.root"
        const val segmentWatchlist = "markets.segment.watchlist"
        const val segmentStocks = "markets.segment.stocks"
        const val segmentCrypto = "markets.segment.crypto"
        const val liveBadge = "markets.liveBadge"
        const val list = "markets.list"
        fun asset(symbol: String) = "markets.asset.$symbol"
        fun assetPrice(symbol: String) = "markets.asset.$symbol.price"
        fun assetChange(symbol: String) = "markets.asset.$symbol.change"
    }

    object Asset {
        const val root = "asset.root"
        const val symbol = "asset.symbol"
        const val price = "asset.price"
        const val change = "asset.change"
        const val buyButton = "asset.buyButton"
        const val sellButton = "asset.sellButton"
        const val statMarketCap = "asset.stat.marketCap"
        const val statVolume = "asset.stat.volume"
        const val statHigh = "asset.stat.high"
        const val statLow = "asset.stat.low"
        fun timeframe(label: String) = "asset.timeframe.$label"
    }

    object Order {
        const val root = "order.root"
        const val sideBuy = "order.side.buy"
        const val sideSell = "order.side.sell"
        const val typeMarket = "order.type.market"
        const val typeLimit = "order.type.limit"
        const val qtyDecrement = "order.qtyStepper.decrement"
        const val qtyIncrement = "order.qtyStepper.increment"
        const val qtyValue = "order.qtyStepper.value"
        const val limitPriceField = "order.limitPriceField"
        const val refPrice = "order.refPrice"
        const val estTotal = "order.estTotal"
        const val warning = "order.warning"
        const val reviewButton = "order.reviewButton"
        const val confirmSheet = "order.confirmSheet"
        const val placeButton = "order.placeButton"
        const val statusToast = "order.statusToast"
    }

    object Portfolio {
        const val root = "portfolio.root"
        const val totalValue = "portfolio.totalValue"
        const val pnl = "portfolio.pnl"
        const val allocationBar = "portfolio.allocationBar"
        const val list = "portfolio.list"
        const val empty = "portfolio.empty"
        fun holding(symbol: String) = "portfolio.holding.$symbol"
        fun holdingValue(symbol: String) = "portfolio.holding.$symbol.value"
        fun holdingPnl(symbol: String) = "portfolio.holding.$symbol.pnl"
    }

    object Card {
        const val root = "card.root"
        const val visual = "card.visual"
        const val number = "card.number"
        const val cvv = "card.cvv"
        const val freezeToggle = "card.freezeToggle"
        const val frozenBadge = "card.frozenBadge"
        const val onlinePaymentsToggle = "card.onlinePaymentsToggle"
        const val limitField = "card.limitField"
        const val limitError = "card.limitError"
        const val pinButton = "card.pinButton"
        const val orderPhysicalButton = "card.orderPhysicalButton"
    }
}
