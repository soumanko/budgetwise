package com.soumanko.budgetwise.domain.finance

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val inrFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    init {
        // Ensure standard formatting without extra spaces if needed
        inrFormat.maximumFractionDigits = 2
        inrFormat.minimumFractionDigits = 2
    }

    fun formatINR(amount: BigDecimal): String {
        return inrFormat.format(amount)
    }
}

fun BigDecimal.toINR(): String {
    return CurrencyFormatter.formatINR(this)
}
