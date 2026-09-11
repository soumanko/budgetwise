package com.soumanko.budgetwise.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soumanko.budgetwise.MainActivity
import com.soumanko.budgetwise.data.local.NotificationPrefs
import com.soumanko.budgetwise.data.remote.SupabaseClient
import com.soumanko.budgetwise.data.repository.BudgetRepository
import com.soumanko.budgetwise.data.repository.ProfileRepository
import com.soumanko.budgetwise.data.repository.RecurringExpenseRepository
import com.soumanko.budgetwise.data.repository.TransactionRepository
import com.soumanko.budgetwise.domain.finance.Calculations
import com.soumanko.budgetwise.domain.finance.DateUtils
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import java.math.BigDecimal

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!NotificationPrefs.isMasterEnabled(context)) {
            return Result.success()
        }

        // Check if there's an authenticated session
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        if (currentUser == null) {
            return Result.failure()
        }

        val client = SupabaseClient.client.postgrest
        val profileRepository = ProfileRepository(client)
        val budgetRepository = BudgetRepository(client)
        val transactionRepository = TransactionRepository(client)
        // val recurringExpenseRepository = RecurringExpenseRepository(client)

        try {
            val profileResult = profileRepository.getProfile()
            val profile = profileResult.getOrNull() ?: return Result.failure()

            createNotificationChannel()

            if (NotificationPrefs.isLowBalanceAlertsEnabled(context)) {
                val balanceResult = transactionRepository.getTotalBalance()
                val balance = balanceResult.getOrNull() ?: BigDecimal.ZERO
                
                if (balance < profile.lowBalanceThreshold) {
                    sendNotification(
                        id = 1,
                        title = "Low balance alert",
                        message = "Your balance has fallen below ${profile.currency}${profile.lowBalanceThreshold}.",
                        target = "dashboard"
                    )
                }
            }

            if (NotificationPrefs.isBudgetAlertsEnabled(context)) {
                val currentMonth = DateUtils.getMonthStart()
                val budgetsResult = budgetRepository.getBudgetsForMonth(currentMonth)
                val budgets = budgetsResult.getOrNull() ?: emptyList()

                val transactionsResult = transactionRepository.getTransactionsByDateRange(
                    startDate = currentMonth,
                    endDate = DateUtils.getMonthEnd()
                )
                val transactions = transactionsResult.getOrNull() ?: emptyList()

                val categorySpending = Calculations.calculateCategorySpending(transactions)

                budgets.forEach { budget ->
                    val spent = categorySpending.find { it.category == budget.category }?.amount ?: BigDecimal.ZERO
                    val ratio = if (budget.amount > BigDecimal.ZERO) {
                        spent.divide(budget.amount, 2, java.math.RoundingMode.HALF_UP)
                    } else {
                        BigDecimal.ZERO
                    }
                    
                    if (ratio >= BigDecimal("0.80")) {
                        sendNotification(
                            id = budget.id.hashCode(),
                            title = "${budget.category} budget",
                            message = "You've used ${ratio.multiply(BigDecimal(100)).toInt()}% of your ${budget.category} budget.",
                            target = "budgets"
                        )
                    }
                }
            }

            if (NotificationPrefs.isRecurringAlertsEnabled(context)) {
                // Future recurring implementation
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "BudgetWise Alerts"
            val descriptionText = "Important financial alerts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("BudgetWise_Alerts", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(id: Int, title: String, message: String, target: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigateTo", target)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, "BudgetWise_Alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, builder.build())
    }
}
