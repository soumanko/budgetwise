package com.soumanko.budgetwise.domain.finance

import com.soumanko.budgetwise.data.model.Budget
import com.soumanko.budgetwise.data.model.Insight
import com.soumanko.budgetwise.data.model.RecurringExpense
import com.soumanko.budgetwise.data.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object InsightGenerator {

    /**
     * Generate all deterministic insights from user data.
     */
    fun generateInsights(
        currentMonthTransactions: List<Transaction>,
        previousMonthTransactions: List<Transaction>,
        budgets: List<Budget>,
        recurringExpenses: List<RecurringExpense>,
        balance: BigDecimal,
        lowBalanceThreshold: BigDecimal
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        val currentStats = FinanceCalculations.calculateMonthlyStats(currentMonthTransactions)
        val previousStats = FinanceCalculations.calculateMonthlyStats(previousMonthTransactions)
        val categorySpending = FinanceCalculations.calculateCategorySpending(currentMonthTransactions)

        // 1. Savings achievement
        if (currentStats.netSavings > BigDecimal.ZERO) {
            insights.add(
                Insight(
                    type = "savings_achievement",
                    title = "Savings this month",
                    description = "You've saved ₹${currentStats.netSavings.toPlainString()} this month.",
                    severity = "positive",
                    data = mapOf(
                        "amount" to currentStats.netSavings.toPlainString(),
                        "rate" to currentStats.savingsRate.toPlainString()
                    )
                )
            )
        }

        // 2. Spending increase vs last month
        if (previousStats.totalExpenses > BigDecimal.ZERO && currentStats.totalExpenses > BigDecimal.ZERO) {
            val change = currentStats.totalExpenses.subtract(previousStats.totalExpenses)
                .divide(previousStats.totalExpenses, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
            
            if (change > BigDecimal("15")) {
                insights.add(
                    Insight(
                        type = "spending_increase",
                        title = "Spending increased",
                        description = "Your spending is ${change.setScale(0, RoundingMode.HALF_UP)}% higher than last month.",
                        severity = "warning",
                        data = mapOf(
                            "change" to change.toPlainString(),
                            "current" to currentStats.totalExpenses.toPlainString(),
                            "previous" to previousStats.totalExpenses.toPlainString()
                        )
                    )
                )
            } else if (change < BigDecimal("-10")) {
                insights.add(
                    Insight(
                        type = "spending_decrease",
                        title = "Spending decreased",
                        description = "Your spending is ${change.abs().setScale(0, RoundingMode.HALF_UP)}% lower than last month. Great job!",
                        severity = "positive",
                        data = mapOf(
                            "change" to change.toPlainString(),
                            "current" to currentStats.totalExpenses.toPlainString(),
                            "previous" to previousStats.totalExpenses.toPlainString()
                        )
                    )
                )
            }
        }

        // 3. Category dominance
        if (categorySpending.isNotEmpty() && categorySpending.first().percentage > BigDecimal("30")) {
            val top = categorySpending.first()
            insights.add(
                Insight(
                    type = "category_dominance",
                    title = "${top.category} is your top expense",
                    description = "${top.category} represents ${top.percentage.toPlainString()}% of your spending this month.",
                    severity = "info",
                    data = mapOf(
                        "category" to top.category,
                        "percentage" to top.percentage.toPlainString(),
                        "amount" to top.amount.toPlainString()
                    )
                )
            )
        }

        // 4. Budget warnings
        for (budget in budgets) {
            val spent = categorySpending.find { it.category == budget.category }?.amount ?: BigDecimal.ZERO
            var utilization = BigDecimal.ZERO
            if (budget.amount > BigDecimal.ZERO) {
                utilization = spent.divide(budget.amount, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
            }

            if (utilization > BigDecimal("100")) {
                val overBy = spent.subtract(budget.amount)
                insights.add(
                    Insight(
                        type = "budget_exceeded",
                        title = "${budget.category} budget exceeded",
                        description = "${budget.category} budget exceeded by ₹${overBy.toPlainString()}.",
                        severity = "critical",
                        data = mapOf(
                            "category" to budget.category,
                            "spent" to spent.toPlainString(),
                            "budget" to budget.amount.toPlainString(),
                            "utilization" to utilization.toPlainString()
                        )
                    )
                )
            } else if (utilization >= BigDecimal("80")) {
                insights.add(
                    Insight(
                        type = "budget_warning",
                        title = "${budget.category} budget almost full",
                        description = "Your ${budget.category} budget is ${utilization.setScale(0, RoundingMode.HALF_UP)}% used.",
                        severity = "warning",
                        data = mapOf(
                            "category" to budget.category,
                            "spent" to spent.toPlainString(),
                            "budget" to budget.amount.toPlainString(),
                            "utilization" to utilization.toPlainString()
                        )
                    )
                )
            }
        }

        // 5. Spending rate projection
        val calendar = Calendar.getInstance()
        val daysPassed = calendar.get(Calendar.DAY_OF_MONTH)
        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        if (daysPassed >= 3 && currentStats.totalExpenses > BigDecimal.ZERO) {
            val projected = FinanceCalculations.predictMonthlyExpenses(currentStats.totalExpenses, daysPassed, totalDays)
            val severity = if (projected > currentStats.totalIncome) "warning" else "info"
            insights.add(
                Insight(
                    type = "spending_projection",
                    title = "Projected monthly spending",
                    description = "At your current rate, you may spend approximately ₹${projected.toPlainString()} this month.",
                    severity = severity,
                    data = mapOf(
                        "projected" to projected.toPlainString(),
                        "dailyRate" to currentStats.totalExpenses.divide(BigDecimal(daysPassed), 2, RoundingMode.HALF_UP).toPlainString()
                    )
                )
            )
        }

        // 6. Low balance warning
        if (balance < lowBalanceThreshold && balance > BigDecimal.ZERO) {
            insights.add(
                Insight(
                    type = "low_balance",
                    title = "Low balance alert",
                    description = "Your balance is below ₹${lowBalanceThreshold.toPlainString()}.",
                    severity = "critical",
                    data = mapOf(
                        "balance" to balance.toPlainString(),
                        "threshold" to lowBalanceThreshold.toPlainString()
                    )
                )
            )
        }

        // 7. Upcoming recurring expenses
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val now = Date()

        val upcoming = recurringExpenses
            .filter { it.active }
            .filter { r ->
                try {
                    val dueDate = dateFormat.parse(r.nextDueDate)
                    if (dueDate != null) {
                        val diffMillis = dueDate.time - now.time
                        val daysUntil = Math.ceil(diffMillis.toDouble() / (1000 * 60 * 60 * 24)).toInt()
                        daysUntil in 0..7
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }

        if (upcoming.isNotEmpty()) {
            var totalUpcoming = BigDecimal.ZERO
            upcoming.forEach { totalUpcoming = totalUpcoming.add(it.amount) }
            insights.add(
                Insight(
                    type = "upcoming_recurring",
                    title = "Upcoming expenses",
                    description = "₹${totalUpcoming.toPlainString()} in recurring expenses due within 7 days.",
                    severity = "info",
                    data = mapOf(
                        "count" to upcoming.size.toString(),
                        "total" to totalUpcoming.toPlainString()
                    )
                )
            )
        }

        // 8. Weekend spending pattern
        val expenses = currentMonthTransactions.filter { it.type == "expense" }
        val weekendExpenses = mutableListOf<Transaction>()
        val weekdayExpenses = mutableListOf<Transaction>()

        expenses.forEach { t ->
            try {
                // assume transactionDate is yyyy-MM-dd'T'HH:mm:ss...
                val dateStr = if (t.transactionDate.contains("T")) t.transactionDate.substringBefore("T") else t.transactionDate
                val d = dateFormat.parse(dateStr)
                if (d != null) {
                    val c = Calendar.getInstance()
                    c.time = d
                    val dayOfWeek = c.get(Calendar.DAY_OF_WEEK)
                    if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) {
                        weekendExpenses.add(t)
                    } else {
                        weekdayExpenses.add(t)
                    }
                }
            } catch (e: Exception) {}
        }

        if (weekendExpenses.size >= 2 && weekdayExpenses.size >= 5) {
            var weekendTotal = BigDecimal.ZERO
            var weekdayTotal = BigDecimal.ZERO
            weekendExpenses.forEach { weekendTotal = weekendTotal.add(it.amount) }
            weekdayExpenses.forEach { weekdayTotal = weekdayTotal.add(it.amount) }

            val weekendAvg = weekendTotal.divide(BigDecimal(weekendExpenses.size), 2, RoundingMode.HALF_UP)
            val weekdayAvg = weekdayTotal.divide(BigDecimal(weekdayExpenses.size), 2, RoundingMode.HALF_UP)

            if (weekendAvg > weekdayAvg.multiply(BigDecimal("1.25"))) {
                val pctMore = weekendAvg.subtract(weekdayAvg)
                    .divide(weekdayAvg, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal("100"))
                insights.add(
                    Insight(
                        type = "weekend_pattern",
                        title = "Weekend spending pattern",
                        description = "You spend approximately ${pctMore.setScale(0, RoundingMode.HALF_UP)}% more on weekends.",
                        severity = "info",
                        data = mapOf(
                            "weekendAvg" to weekendAvg.toPlainString(),
                            "weekdayAvg" to weekdayAvg.toPlainString(),
                            "pctMore" to pctMore.toPlainString()
                        )
                    )
                )
            }
        }

        return insights
    }
}
