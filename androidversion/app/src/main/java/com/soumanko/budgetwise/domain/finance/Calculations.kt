package com.soumanko.budgetwise.domain.finance

import com.soumanko.budgetwise.data.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode

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

object Calculations {
    private val HUNDRED = BigDecimal("100")

    fun toMoney(value: BigDecimal): BigDecimal {
        return value.setScale(2, RoundingMode.HALF_UP)
    }

    fun addMoney(a: BigDecimal, b: BigDecimal): BigDecimal {
        return (a + b).setScale(2, RoundingMode.HALF_UP)
    }

    fun subtractMoney(a: BigDecimal, b: BigDecimal): BigDecimal {
        return (a - b).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateBalance(transactions: List<Transaction>): BigDecimal {
        var balance = BigDecimal.ZERO
        for (t in transactions) {
            if (t.type == "income") {
                balance += t.amount
            } else {
                balance -= t.amount
            }
        }
        return toMoney(balance)
    }

    fun calculateMonthlyStats(transactions: List<Transaction>): MonthlyStats {
        var income = BigDecimal.ZERO
        var expense = BigDecimal.ZERO

        for (t in transactions) {
            if (t.type == "income") {
                income += t.amount
            } else {
                expense += t.amount
            }
        }

        val netSavings = income - expense
        val savingsRate = if (income > BigDecimal.ZERO) {
            (netSavings.divide(income, 4, RoundingMode.HALF_UP) * HUNDRED).setScale(1, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return MonthlyStats(
            totalIncome = toMoney(income),
            totalExpenses = toMoney(expense),
            netSavings = toMoney(netSavings),
            savingsRate = savingsRate,
            transactionCount = transactions.size
        )
    }

    fun calculateCategorySpending(transactions: List<Transaction>): List<CategorySpending> {
        val expenses = transactions.filter { it.type == "expense" }
        val categoryMap = mutableMapOf<String, Pair<BigDecimal, Int>>()

        var total = BigDecimal.ZERO
        for (t in expenses) {
            total += t.amount
            val existing = categoryMap[t.category] ?: Pair(BigDecimal.ZERO, 0)
            categoryMap[t.category] = Pair(existing.first + t.amount, existing.second + 1)
        }

        val result = mutableListOf<CategorySpending>()
        for ((category, data) in categoryMap) {
            val percentage = if (total > BigDecimal.ZERO) {
                (data.first.divide(total, 4, RoundingMode.HALF_UP) * HUNDRED).setScale(1, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
            result.add(CategorySpending(
                category = category,
                amount = toMoney(data.first),
                percentage = percentage,
                count = data.second
            ))
        }

        return result.sortedByDescending { it.amount }
    }

    fun calculateDailySpending(transactions: List<Transaction>): List<DailySpending> {
        val dailyMap = mutableMapOf<String, Pair<BigDecimal, BigDecimal>>()

        for (t in transactions) {
            val date = t.transactionDate
            val existing = dailyMap[date] ?: Pair(BigDecimal.ZERO, BigDecimal.ZERO)
            if (t.type == "expense") {
                dailyMap[date] = Pair(existing.first + t.amount, existing.second)
            } else {
                dailyMap[date] = Pair(existing.first, existing.second + t.amount)
            }
        }

        val result = mutableListOf<DailySpending>()
        for ((date, data) in dailyMap) {
            result.add(DailySpending(
                date = date,
                amount = toMoney(data.first),
                income = toMoney(data.second)
            ))
        }

        return result.sortedBy { it.date }
    }

    fun calculateSafeToSpend(
        balance: BigDecimal,
        daysRemaining: Int,
        upcomingRecurringTotal: BigDecimal,
        monthlyBudget: BigDecimal
    ): BigDecimal {
        if (daysRemaining <= 0) return BigDecimal.ZERO

        val available = subtractMoney(balance, upcomingRecurringTotal)

        var effectiveAvailable = available
        if (monthlyBudget > BigDecimal.ZERO) {
            effectiveAvailable = effectiveAvailable.min(monthlyBudget)
        }

        if (effectiveAvailable <= BigDecimal.ZERO) return BigDecimal.ZERO

        return toMoney(effectiveAvailable.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP))
    }
}
