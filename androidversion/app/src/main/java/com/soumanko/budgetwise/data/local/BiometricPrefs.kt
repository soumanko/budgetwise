package com.soumanko.budgetwise.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Local SharedPreferences wrapper for biometric app lock settings.
 * Stores only the boolean toggle state — no passwords or biometric data.
 */
object BiometricPrefs {
    private const val PREFS_NAME = "budgetwise_biometric_prefs"
    private const val KEY_LOCK_ENABLED = "biometric_lock_enabled"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isLockEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_LOCK_ENABLED, false)
    }

    fun setLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }
}
