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
    suspend fun getRecurringExpense(id: String): Result<RecurringExpense?> {
        return try {
            val expense = postgrest["recurring_expenses"]
                .select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingleOrNull<RecurringExpense>()
            Result.success(expense)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun insertRecurringExpense(expense: com.soumanko.budgetwise.data.model.RecurringExpenseInsert): Result<Unit> {
        return try {
            postgrest["recurring_expenses"].insert(expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRecurringExpense(expense: RecurringExpense): Result<Unit> {
        return try {
            postgrest["recurring_expenses"].update(expense) {
                filter {
                    eq("id", expense.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecurringExpense(id: String): Result<Unit> {
        return try {
            postgrest["recurring_expenses"].delete {
                filter {
                    eq("id", id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
