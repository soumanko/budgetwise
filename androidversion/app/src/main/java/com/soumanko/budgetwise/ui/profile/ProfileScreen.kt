package com.soumanko.budgetwise.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onRequestNotificationPermission: () -> Unit = {},
    onAppearanceChanged: (String) -> Unit = {},
    onNavigateToFinancialStatement: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("") }
    var monthlyBudget by remember { mutableStateOf("") }
    var lowBalanceThreshold by remember { mutableStateOf("") }
    
    var dataInitialized by remember { mutableStateOf(false) }

    var masterNotifsEnabled by remember { mutableStateOf(com.soumanko.budgetwise.data.local.NotificationPrefs.isMasterEnabled(context)) }
    var budgetAlertsEnabled by remember { mutableStateOf(com.soumanko.budgetwise.data.local.NotificationPrefs.isBudgetAlertsEnabled(context)) }
    var lowBalanceAlertsEnabled by remember { mutableStateOf(com.soumanko.budgetwise.data.local.NotificationPrefs.isLowBalanceAlertsEnabled(context)) }
    var recurringAlertsEnabled by remember { mutableStateOf(com.soumanko.budgetwise.data.local.NotificationPrefs.isRecurringAlertsEnabled(context)) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                masterNotifsEnabled = com.soumanko.budgetwise.data.local.NotificationPrefs.isMasterEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success && !dataInitialized) {
            val profile = (uiState as ProfileUiState.Success).profile
            fullName = profile.fullName
            currency = profile.currency
            monthlyBudget = profile.monthlyBudget.toString()
            lowBalanceThreshold = profile.lowBalanceThreshold.toString()
            dataInitialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings") },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProfileUiState.Error -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadProfile() }) { Text("Retry") }
                    }
                }
                is ProfileUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Personal Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = onNavigateToFinancialStatement,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Generate Financial Statement")
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Currency (e.g. USD, EUR)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = monthlyBudget,
                            onValueChange = { monthlyBudget = it },
                            label = { Text("Global Monthly Budget") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = lowBalanceThreshold,
                            onValueChange = { lowBalanceThreshold = it },
                            label = { Text("Low Balance Threshold") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val budget = monthlyBudget.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val threshold = lowBalanceThreshold.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                if (fullName.isNotBlank() && currency.isNotBlank()) {
                                    viewModel.updateProfile(
                                        fullName = fullName,
                                        currency = currency,
                                        monthlyBudget = budget,
                                        lowBalanceThreshold = threshold,
                                        onSuccess = { /* Optionally show toast */ }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Changes")
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Appearance Section
                        Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))

                        var currentAppearance by remember { mutableStateOf(com.soumanko.budgetwise.data.local.AppearancePrefs.getAppearanceMode(context)) }
                        
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = currentAppearance == com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_SYSTEM,
                                onClick = { 
                                    currentAppearance = com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_SYSTEM
                                    onAppearanceChanged(com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_SYSTEM) 
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) { Text("System") }
                            SegmentedButton(
                                selected = currentAppearance == com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_LIGHT,
                                onClick = { 
                                    currentAppearance = com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_LIGHT
                                    onAppearanceChanged(com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_LIGHT) 
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) { Text("Light") }
                            SegmentedButton(
                                selected = currentAppearance == com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_DARK,
                                onClick = { 
                                    currentAppearance = com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_DARK
                                    onAppearanceChanged(com.soumanko.budgetwise.data.local.AppearancePrefs.MODE_DARK) 
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) { Text("Dark") }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Notifications Section
                        Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Notifications", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = masterNotifsEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && 
                                        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            onRequestNotificationPermission()
                                    } else {
                                        masterNotifsEnabled = enabled
                                        com.soumanko.budgetwise.data.local.NotificationPrefs.setMasterEnabled(context, enabled)
                                        if (enabled) {
                                            val request = androidx.work.PeriodicWorkRequestBuilder<com.soumanko.budgetwise.worker.NotificationWorker>(12, java.util.concurrent.TimeUnit.HOURS).build()
                                            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork("BudgetWiseNotifications", androidx.work.ExistingPeriodicWorkPolicy.KEEP, request)
                                        } else {
                                            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("BudgetWiseNotifications")
                                        }
                                    }
                                }
                            )
                        }
                        
                        if (masterNotifsEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Budget Alerts", style = MaterialTheme.typography.bodyLarge)
                                Switch(
                                    checked = budgetAlertsEnabled,
                                    onCheckedChange = { enabled ->
                                        budgetAlertsEnabled = enabled
                                        com.soumanko.budgetwise.data.local.NotificationPrefs.setBudgetAlertsEnabled(context, enabled)
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Low Balance Alerts", style = MaterialTheme.typography.bodyLarge)
                                Switch(
                                    checked = lowBalanceAlertsEnabled,
                                    onCheckedChange = { enabled ->
                                        lowBalanceAlertsEnabled = enabled
                                        com.soumanko.budgetwise.data.local.NotificationPrefs.setLowBalanceAlertsEnabled(context, enabled)
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recurring Expense Alerts", style = MaterialTheme.typography.bodyLarge)
                                Switch(
                                    checked = recurringAlertsEnabled,
                                    onCheckedChange = { enabled ->
                                        recurringAlertsEnabled = enabled
                                        com.soumanko.budgetwise.data.local.NotificationPrefs.setRecurringAlertsEnabled(context, enabled)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Security Section
                        Text("Security", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))

                        val biometricCapability = remember {
                            com.soumanko.budgetwise.ui.security.BiometricLockManager.checkCapability(context)
                        }
                        var biometricEnabled by remember {
                            mutableStateOf(com.soumanko.budgetwise.data.local.BiometricPrefs.isLockEnabled(context))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric App Lock", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    when (biometricCapability) {
                                        com.soumanko.budgetwise.ui.security.BiometricCapability.AVAILABLE ->
                                            "Require fingerprint or face to unlock"
                                        com.soumanko.budgetwise.ui.security.BiometricCapability.NOT_ENROLLED ->
                                            "Set up biometrics in device settings first"
                                        com.soumanko.budgetwise.ui.security.BiometricCapability.NOT_AVAILABLE ->
                                            "Not supported on this device"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { enabled ->
                                    val activity = context as? androidx.fragment.app.FragmentActivity
                                    if (activity != null) {
                                        com.soumanko.budgetwise.ui.security.BiometricLockManager.showPrompt(
                                            activity = activity,
                                            onSuccess = {
                                                biometricEnabled = enabled
                                                com.soumanko.budgetwise.data.local.BiometricPrefs.setLockEnabled(context, enabled)
                                            },
                                            onError = { /* Keep the switch unchanged on error/cancel */ }
                                        )
                                    }
                                },
                                enabled = biometricCapability == com.soumanko.budgetwise.ui.security.BiometricCapability.AVAILABLE
                            )
                        }
                    }
                }
            }
        }
    }
}
