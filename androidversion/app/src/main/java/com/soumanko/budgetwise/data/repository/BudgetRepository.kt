package com.soumanko.budgetwise.data.repository

import com.soumanko.budgetwise.data.model.Budget
import com.soumanko.budgetwise.data.model.BudgetInsert
import io.github.jan.supabase.postgrest.Postgrest

class BudgetRepository(private val postgrest: Postgrest) {

    suspend fun getBudgetsForMonth(month: String): Result<List<Budget>> {
        return try {
            val budgets = postgrest["budgets"]
                .select {
                    filter {
                        eq("month", month)
                    }
                }
                .decodeList<Budget>()
            Result.success(budgets)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun upsertBudget(budgetInsert: BudgetInsert): Result<Budget> {
        return try {
            val budget = postgrest["budgets"]
                .upsert(listOf(budgetInsert)) {
                    onConflict = "user_id, category, month"
                    select()
                }
                .decodeSingle<Budget>()
            Result.success(budget)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
