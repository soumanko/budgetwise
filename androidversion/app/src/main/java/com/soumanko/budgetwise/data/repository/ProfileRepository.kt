package com.soumanko.budgetwise.data.repository

import com.soumanko.budgetwise.data.model.Profile
import io.github.jan.supabase.postgrest.Postgrest

class ProfileRepository(private val postgrest: Postgrest) {

    suspend fun getProfile(): Result<Profile> {
        return try {
            // RLS automatically limits this to the authenticated user's profile
            val profile = postgrest["profiles"]
                .select()
                .decodeSingle<Profile>()
            Result.success(profile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
