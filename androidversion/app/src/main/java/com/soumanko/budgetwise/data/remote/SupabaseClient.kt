package com.soumanko.budgetwise.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    private const val SUPABASE_URL = "https://wpwfaztaxqvhuubdspou.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Indwd2ZhenRheHF2aHV1YmRzcG91Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODYxNTk0NzUsImV4cCI6MjEwMTczNTQ3NX0.bC20VmJt36nVi6_aN_JAuqUKmnBSracGv-bOSZr2Sbg"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}
