package com.soumanko.budgetwise.domain.finance

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object DateUtils {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getMonthStart(date: LocalDate = LocalDate.now()): String {
        return date.withDayOfMonth(1).format(formatter)
    }

    fun getMonthEnd(date: LocalDate = LocalDate.now()): String {
        val lastDay = YearMonth.from(date).atEndOfMonth()
        return lastDay.format(formatter)
    }

    fun getPreviousMonthDateStr(): String {
        return getMonthStart(LocalDate.now().minusMonths(1))
    }
    
    fun getPreviousMonthEndStr(): String {
        return getMonthEnd(LocalDate.now().minusMonths(1))
    }

    fun getDaysRemainingInMonth(): Int {
        val now = LocalDate.now()
        val lastDay = YearMonth.from(now).lengthOfMonth()
        return lastDay - now.dayOfMonth
    }

    fun getDaysInMonth(date: LocalDate = LocalDate.now()): Int {
        return YearMonth.from(date).lengthOfMonth()
    }
}
