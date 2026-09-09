package com.soumanko.budgetwise.data.repository

import com.soumanko.budgetwise.data.model.SavingsGoal
import com.soumanko.budgetwise.data.model.SavingsGoalInsert
import io.github.jan.supabase.postgrest.Postgrest
import java.math.BigDecimal

class SavingsGoalRepository(private val postgrest: Postgrest) {

    suspend fun getSavingsGoals(): Result<List<SavingsGoal>> {
        return try {
            val goals = postgrest["savings_goals"]
                .select()
                .decodeList<SavingsGoal>()
            Result.success(goals)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun createSavingsGoal(goalInsert: SavingsGoalInsert): Result<SavingsGoal> {
        return try {
            val goal = postgrest["savings_goals"]
                .insert(goalInsert) {
                    select()
                }
                .decodeSingle<SavingsGoal>()
            Result.success(goal)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updateGoalAmount(id: String, newAmount: BigDecimal): Result<SavingsGoal> {
        return try {
            val goal = postgrest["savings_goals"]
                .update(
                    {
                        SavingsGoal::currentAmount setTo newAmount
                    }
                ) {
                    filter {
                        eq("id", id)
                    }
                    select()
                }
                .decodeSingle<SavingsGoal>()
            Result.success(goal)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
