package com.soumanko.budgetwise.data.repository

import com.soumanko.budgetwise.data.model.RecurringExpense
import io.github.jan.supabase.postgrest.Postgrest

class RecurringExpenseRepository(private val postgrest: Postgrest) {

    suspend fun getActiveRecurringExpenses(): Result<List<RecurringExpense>> {
        return try {
            val expenses = postgrest["recurring_expenses"]
                .select {
                    filter {
                        eq("active", true)
                    }
                }
                .decodeList<RecurringExpense>()
            Result.success(expenses)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
