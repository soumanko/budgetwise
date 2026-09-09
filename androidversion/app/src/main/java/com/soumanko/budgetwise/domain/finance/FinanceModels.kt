package com.soumanko.budgetwise.domain.finance

import java.math.BigDecimal

data class MonthlyStats(
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val netSavings: BigDecimal,
    val savingsRate: BigDecimal,
    val transactionCount: Int
)

data class CategorySpending(
    val category: String,
    val amount: BigDecimal,
    val percentage: BigDecimal,
    val count: Int
)

data class DailySpending(
    val date: String,
    val amount: BigDecimal,
    val income: BigDecimal
)
