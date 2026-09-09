package com.soumanko.budgetwise.domain.finance

import com.soumanko.budgetwise.data.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Date

object FinanceCalculations {

    /**
     * Calculate balance from transactions.
     * Balance = Total Income - Total Expenses
     */
    fun calculateBalance(transactions: List<Transaction>): BigDecimal {
        var balance = BigDecimal.ZERO
        for (t in transactions) {
            if (t.type == "income") {
                balance = balance.add(t.amount)
            } else {
                balance = balance.subtract(t.amount)
            }
        }
        return balance.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculate monthly statistics from transactions within a date range.
     */
    fun calculateMonthlyStats(transactions: List<Transaction>): MonthlyStats {
        var income = BigDecimal.ZERO
        var expenses = BigDecimal.ZERO

        for (t in transactions) {
            if (t.type == "income") {
                income = income.add(t.amount)
            } else {
                expenses = expenses.add(t.amount)
            }
        }

        val netSavings = income.subtract(expenses)
        var savingsRate = BigDecimal.ZERO
        if (income > BigDecimal.ZERO) {
            savingsRate = netSavings.divide(income, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
        }

        return MonthlyStats(
            totalIncome = income.setScale(2, RoundingMode.HALF_UP),
            totalExpenses = expenses.setScale(2, RoundingMode.HALF_UP),
            netSavings = netSavings.setScale(2, RoundingMode.HALF_UP),
            savingsRate = savingsRate.setScale(1, RoundingMode.HALF_UP),
            transactionCount = transactions.size
        )
    }

    /**
     * Calculate spending breakdown by category.
     */
    fun calculateCategorySpending(transactions: List<Transaction>): List<CategorySpending> {
        val expenses = transactions.filter { it.type == "expense" }
        val categoryMap = mutableMapOf<String, Pair<BigDecimal, Int>>()

        var totalExpenses = BigDecimal.ZERO
        for (t in expenses) {
            totalExpenses = totalExpenses.add(t.amount)
            val current = categoryMap[t.category] ?: Pair(BigDecimal.ZERO, 0)
            categoryMap[t.category] = Pair(current.first.add(t.amount), current.second + 1)
        }

        val result = mutableListOf<CategorySpending>()
        for ((category, data) in categoryMap) {
            val amount = data.first
            var percentage = BigDecimal.ZERO
            if (totalExpenses > BigDecimal.ZERO) {
                percentage = amount.divide(totalExpenses, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
            }
            result.add(
                CategorySpending(
                    category = category,
                    amount = amount.setScale(2, RoundingMode.HALF_UP),
                    percentage = percentage.setScale(1, RoundingMode.HALF_UP),
                    count = data.second
                )
            )
        }

        return result.sortedByDescending { it.amount }
    }

    /**
     * Calculate daily spending data for charts.
     */
    fun calculateDailySpending(transactions: List<Transaction>): List<DailySpending> {
        val dailyMap = mutableMapOf<String, Pair<BigDecimal, BigDecimal>>()

        for (t in transactions) {
            // Keep only YYYY-MM-DD
            val date = t.transactionDate.substringBefore("T")
            val current = dailyMap[date] ?: Pair(BigDecimal.ZERO, BigDecimal.ZERO)
            
            if (t.type == "expense") {
                dailyMap[date] = Pair(current.first.add(t.amount), current.second)
            } else {
                dailyMap[date] = Pair(current.first, current.second.add(t.amount))
            }
        }

        val result = mutableListOf<DailySpending>()
        for ((date, data) in dailyMap) {
            result.add(
                DailySpending(
                    date = date,
                    amount = data.first.setScale(2, RoundingMode.HALF_UP),
                    income = data.second.setScale(2, RoundingMode.HALF_UP)
                )
            )
        }

        return result.sortedBy { it.date }
    }

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

    /**
     * Predict end-of-month expenses based on current spending rate.
     */
    fun predictMonthlyExpenses(
        currentExpenses: BigDecimal,
        daysPassed: Int,
        totalDaysInMonth: Int
    ): BigDecimal {
        if (daysPassed <= 0) return BigDecimal.ZERO
        val dailyRate = currentExpenses.divide(BigDecimal(daysPassed), 4, RoundingMode.HALF_UP)
        return dailyRate.multiply(BigDecimal(totalDaysInMonth)).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Predict end-of-month balance.
     */
    fun predictEndOfMonthBalance(
        currentBalance: BigDecimal,
        predictedRemainingExpenses: BigDecimal,
        expectedRemainingIncome: BigDecimal
    ): BigDecimal {
        return currentBalance.subtract(predictedRemainingExpenses).add(expectedRemainingIncome).setScale(2, RoundingMode.HALF_UP)
    }

    data class FinancialHealthFactor(val label: String, val status: String, val detail: String)
    data class FinancialHealthResult(val score: Int, val factors: List<FinancialHealthFactor>)

    /**
     * Calculate financial health score (0-100).
     */
    fun calculateFinancialHealthScore(
        savingsRate: BigDecimal,
        budgetAdherence: BigDecimal,
        spendingConsistency: BigDecimal,
        hasEmergencyFund: Boolean,
        overBudgetCategories: Int,
        totalCategories: Int
    ): FinancialHealthResult {
        var score = 50
        val factors = mutableListOf<FinancialHealthFactor>()

        // Savings rate (0-25 points)
        if (savingsRate >= BigDecimal("30")) {
            score += 25
            factors.add(FinancialHealthFactor("Savings rate", "good", "Your savings rate is ${savingsRate.toPlainString()}%"))
        } else if (savingsRate >= BigDecimal("15")) {
            score += 15
            factors.add(FinancialHealthFactor("Savings rate", "good", "Your savings rate is ${savingsRate.toPlainString()}%"))
        } else if (savingsRate >= BigDecimal.ZERO) {
            score += 5
            factors.add(FinancialHealthFactor("Savings rate", "warning", "Your savings rate is only ${savingsRate.toPlainString()}%"))
        } else {
            score -= 10
            factors.add(FinancialHealthFactor("Savings rate", "bad", "You're spending more than you earn"))
        }

        // Budget adherence (0-25 points)
        if (budgetAdherence >= BigDecimal("90")) {
            score += 25
            factors.add(FinancialHealthFactor("Budget adherence", "good", "You stayed within your budgets"))
        } else if (budgetAdherence >= BigDecimal("70")) {
            score += 15
            factors.add(FinancialHealthFactor("Budget adherence", "warning", "Some budgets were exceeded"))
        } else {
            score += 0
            factors.add(FinancialHealthFactor("Budget adherence", "bad", "Multiple budgets exceeded"))
        }

        // Over-budget categories penalty
        if (overBudgetCategories > 0) {
            score -= overBudgetCategories * 3
            factors.add(
                FinancialHealthFactor(
                    "Over-budget categories",
                    "warning",
                    "$overBudgetCategories category budget${if (overBudgetCategories > 1) "s" else ""} exceeded"
                )
            )
        }

        score = score.coerceIn(0, 100)
        return FinancialHealthResult(score, factors)
    }

    /**
     * Calculate date when balance might drop below threshold.
     */
    fun predictLowBalanceDate(
        currentBalance: BigDecimal,
        avgDailyExpenses: BigDecimal,
        threshold: BigDecimal
    ): Date? {
        if (avgDailyExpenses <= BigDecimal.ZERO || currentBalance <= threshold) return null

        val distance = currentBalance.subtract(threshold)
        val daysUntilThreshold = distance.divide(avgDailyExpenses, 0, RoundingMode.DOWN).toInt()
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysUntilThreshold)

        return calendar.time
    }
}
