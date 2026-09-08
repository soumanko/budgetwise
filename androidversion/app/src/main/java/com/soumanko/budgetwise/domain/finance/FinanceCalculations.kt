package com.soumanko.budgetwise.domain.finance

import java.math.BigDecimal
import java.math.RoundingMode

object FinanceCalculations {

    /**
     * Calculate "Safe to Spend" — daily budget based on remaining balance and days in month.
     */
    fun calculateSafeToSpend(
        balance: BigDecimal,
        daysRemaining: Int,
        upcomingRecurringTotal: BigDecimal,
        monthlyBudget: BigDecimal
    ): BigDecimal {
        if (daysRemaining <= 0) return BigDecimal.ZERO

        // Available = balance - upcoming recurring expenses
        val available = balance.subtract(upcomingRecurringTotal)

        // If there's a monthly budget, use the lesser of available and remaining budget
        var effectiveAvailable = available
        if (monthlyBudget > BigDecimal.ZERO) {
            effectiveAvailable = available.min(monthlyBudget)
        }

        if (effectiveAvailable <= BigDecimal.ZERO) return BigDecimal.ZERO

        return effectiveAvailable.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
    }
}
