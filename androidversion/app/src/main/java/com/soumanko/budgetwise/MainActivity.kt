package com.soumanko.budgetwise

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.soumanko.budgetwise.data.local.BiometricPrefs
import com.soumanko.budgetwise.ui.BudgetWiseApp
import com.soumanko.budgetwise.ui.security.BiometricLockManager
import com.soumanko.budgetwise.ui.security.LockScreen
import com.soumanko.budgetwise.ui.theme.BudgetWiseTheme

class MainActivity : FragmentActivity() {

    // Track whether the app is currently locked
    private lateinit var _isLocked: MutableState<Boolean>
    private var wasInBackground = false

    private lateinit var requestNotificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_BudgetWise)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        _isLocked = mutableStateOf(BiometricPrefs.isLockEnabled(this))

        requestNotificationPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                com.soumanko.budgetwise.data.local.NotificationPrefs.setMasterEnabled(this, true)
                val request = androidx.work.PeriodicWorkRequestBuilder<com.soumanko.budgetwise.worker.NotificationWorker>(12, java.util.concurrent.TimeUnit.HOURS).build()
                androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork("BudgetWiseNotifications", androidx.work.ExistingPeriodicWorkPolicy.KEEP, request)
            } else {
                com.soumanko.budgetwise.data.local.NotificationPrefs.setMasterEnabled(this, false)
                androidx.work.WorkManager.getInstance(this).cancelUniqueWork("BudgetWiseNotifications")
            }
        }

        // Observe lifecycle to detect background → foreground transitions
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    wasInBackground = true
                }
                Lifecycle.Event.ON_START -> {
                    if (wasInBackground && BiometricPrefs.isLockEnabled(this@MainActivity)) {
                        _isLocked.value = true
                    }
                    wasInBackground = false
                }
                else -> {}
            }
        })

        val appearanceState = mutableStateOf(com.soumanko.budgetwise.data.local.AppearancePrefs.getAppearanceMode(this))

        setContent {
            val currentAppearance by appearanceState
            BudgetWiseTheme(appearance = currentAppearance) {
                val isLocked by _isLocked

                if (isLocked) {
                    LockScreen(
                        onUnlockClicked = { triggerBiometricUnlock() }
                    )
                    // Auto-trigger prompt when lock screen appears
                    LaunchedEffect(Unit) {
                        triggerBiometricUnlock()
                    }
                } else {
                    BudgetWiseApp(
                        onRequestNotificationPermission = {
                            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onAppearanceChanged = { newAppearance ->
                            com.soumanko.budgetwise.data.local.AppearancePrefs.setAppearanceMode(this@MainActivity, newAppearance)
                            appearanceState.value = newAppearance
                        }
                    )
                }
            }
        }
    }

    private fun triggerBiometricUnlock() {
        BiometricLockManager.showPrompt(
            activity = this,
            onSuccess = { _isLocked.value = false },
            onError = { /* Stay locked — user can tap Unlock to retry */ }
        )
    }
}