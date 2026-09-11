package com.soumanko.budgetwise.data.local

import android.content.Context
import android.content.SharedPreferences

object AppearancePrefs {
    private const val PREFS_NAME = "budgetwise_appearance_prefs"
    private const val KEY_APPEARANCE_MODE = "appearance_mode"

    const val MODE_SYSTEM = "SYSTEM"
    const val MODE_LIGHT = "LIGHT"
    const val MODE_DARK = "DARK"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAppearanceMode(context: Context): String {
        return getPrefs(context).getString(KEY_APPEARANCE_MODE, MODE_SYSTEM) ?: MODE_SYSTEM
    }

    fun setAppearanceMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_APPEARANCE_MODE, mode).apply()
    }
}
