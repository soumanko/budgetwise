package com.soumanko.budgetwise.data.repository

import com.soumanko.budgetwise.data.model.Account
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order

class AccountRepository(private val postgrest: Postgrest) {

    suspend fun getPrimaryAccount(): Result<Account?> {
        return try {
            val accounts = postgrest["accounts"].select {
                order("created_at", order = Order.ASCENDING)
                limit(1)
            }.decodeList<Account>()
            
            Result.success(accounts.firstOrNull())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun getAllAccounts(): Result<List<Account>> {
        return try {
            val accounts = postgrest["accounts"].select {
                order("created_at", order = Order.ASCENDING)
            }.decodeList<Account>()
            
            Result.success(accounts)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
