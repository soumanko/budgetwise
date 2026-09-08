package com.soumanko.budgetwise.data.repository

import com.soumanko.budgetwise.data.model.Transaction
import com.soumanko.budgetwise.data.model.TransactionInsert
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

class TransactionRepository(private val postgrest: Postgrest) {

    suspend fun getTransactions(): Result<List<Transaction>> {
        return try {
            val transactions = postgrest["transactions"]
                .select()
                .decodeList<Transaction>()
            Result.success(transactions)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getTransactionsByDateRange(startDate: String, endDate: String): Result<List<Transaction>> {
        return try {
            val transactions = postgrest["transactions"].select {
                filter {
                    gte("transaction_date", startDate)
                    lte("transaction_date", endDate)
                }
                // Month boundary queries do not determine UI order, just data boundaries.
            }.decodeList<Transaction>()
            Result.success(transactions)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getTotalBalance(): Result<BigDecimal> {
        return try {
            val response = postgrest.rpc("get_user_balance")
            val jsonElement = response.decodeAs<kotlinx.serialization.json.JsonElement>()
            val balanceStr = jsonElement.jsonPrimitive.content
            Result.success(BigDecimal(balanceStr))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getTransactionsPage(
        lastCreatedAt: String?,
        lastId: String?,
        limitCount: Long = 20,
        type: String? = null,
        category: String? = null,
        searchQuery: String? = null
    ): Result<List<Transaction>> {
        return try {
            val transactions = postgrest["transactions"].select {
                filter {
                    if (type != null && type != "all") {
                        eq("type", type)
                    }
                    if (category != null && category != "all") {
                        eq("category", category)
                    }
                    if (!searchQuery.isNullOrBlank()) {
                        val q = "%$searchQuery%"
                        or {
                            ilike("description", q)
                            ilike("merchant", q)
                            ilike("category", q)
                            ilike("notes", q)
                        }
                    }
                    if (lastCreatedAt != null && lastId != null) {
                        or {
                            lt("created_at", lastCreatedAt)
                            and {
                                eq("created_at", lastCreatedAt)
                                lt("id", lastId)
                            }
                        }
                    }
                }
                order("created_at", order = Order.DESCENDING)
                order("id", order = Order.DESCENDING)
                limit(limitCount)
            }.decodeList<Transaction>()
            Result.success(transactions)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun addTransaction(transaction: TransactionInsert): Result<Unit> {
        return try {
            postgrest["transactions"].insert(transaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        return try {
            postgrest["transactions"].update(transaction) {
                filter {
                    eq("id", transaction.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(id: String): Result<Unit> {
        return try {
            postgrest["transactions"].delete {
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
