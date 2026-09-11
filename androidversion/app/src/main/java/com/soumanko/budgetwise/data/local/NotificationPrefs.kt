package com.soumanko.budgetwise.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Local SharedPreferences wrapper for Notification settings.
 */
object NotificationPrefs {
    private const val PREFS_NAME = "budgetwise_notification_prefs"
    private const val KEY_MASTER_ENABLED = "master_notifications_enabled"
    private const val KEY_BUDGET_ALERTS = "budget_alerts_enabled"
    private const val KEY_LOW_BALANCE_ALERTS = "low_balance_alerts_enabled"
    private const val KEY_RECURRING_ALERTS = "recurring_alerts_enabled"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isMasterEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_MASTER_ENABLED, false)
    fun setMasterEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()

    fun isBudgetAlertsEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_BUDGET_ALERTS, true)
    fun setBudgetAlertsEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_BUDGET_ALERTS, enabled).apply()

    fun isLowBalanceAlertsEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_LOW_BALANCE_ALERTS, true)
    fun setLowBalanceAlertsEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_LOW_BALANCE_ALERTS, enabled).apply()

    fun isRecurringAlertsEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_RECURRING_ALERTS, true)
    fun setRecurringAlertsEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_RECURRING_ALERTS, enabled).apply()
}
