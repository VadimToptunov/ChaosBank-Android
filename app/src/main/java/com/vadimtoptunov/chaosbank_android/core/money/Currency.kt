package com.vadimtoptunov.chaosbank_android.core.money

enum class Currency(val symbol: String) {
    EUR("€"),
    USD("$"),
    GBP("£");

    val code: String get() = name
}
