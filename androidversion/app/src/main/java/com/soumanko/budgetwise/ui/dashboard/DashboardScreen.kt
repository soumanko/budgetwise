package com.soumanko.budgetwise.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.soumanko.budgetwise.domain.finance.toINR
import com.soumanko.budgetwise.ui.theme.IncomeGreen
import com.soumanko.budgetwise.ui.theme.ExpenseRed
import com.soumanko.budgetwise.data.model.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onLogout: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToSavingsGoals: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToDailyReceipt: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Automatically refresh data when screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Welcome back,", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("BudgetWise", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Person, 
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DashboardUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadData() }) {
                            Text("Retry")
                        }
                    }
                }
                is DashboardUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No transactions yet. Add some to get started!", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onNavigateToAddTransaction) {
                                Text("Add Transaction")
                            }
                        }
                    }
                }
                is DashboardUiState.Success -> {
                    DashboardContent(
                        state = state,
                        onNavigateToBudgets = onNavigateToBudgets,
                        onNavigateToRecurring = onNavigateToRecurring,
                        onNavigateToSavingsGoals = onNavigateToSavingsGoals,
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onNavigateToAddTransaction = onNavigateToAddTransaction,
                        onNavigateToTransactions = onNavigateToTransactions,
                        onNavigateToDailyReceipt = onNavigateToDailyReceipt
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    state: DashboardUiState.Success,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToSavingsGoals: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToDailyReceipt: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Total Balance
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Total Balance", 
                    style = MaterialTheme.typography.bodyLarge, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.balance.toINR(), 
                        style = MaterialTheme.typography.displayLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif), 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Mock trend for design placeholder
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                        Text("+12%", color = IncomeGreen, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        
        // Income vs Expenses
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("+${state.currentStats.totalIncome.toINR()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = IncomeGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Expenses", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("-${state.currentStats.totalExpenses.toINR()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ExpenseRed)
                }
            }
        }

        // Safe to Spend
        item {
            Card(
                modifier = Modifier.fillMaxWidth(), 
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Safe to Spend", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (state.safeToSpend != null) {
                        Text("${state.safeToSpend.toINR()} / day", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Based on your remaining budget and days left this month.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Budget data unavailable", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        
        // Quick Actions
        item {
            Column {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionIcon(icon = Icons.Filled.Add, label = "Add", onClick = onNavigateToAddTransaction, isPrimary = true)
                    QuickActionIcon(icon = Icons.Filled.ShoppingCart, label = "Budgets", onClick = onNavigateToBudgets)
                    QuickActionIcon(icon = Icons.Filled.Refresh, label = "Recurring", onClick = onNavigateToRecurring)
                    QuickActionIcon(icon = Icons.Filled.List, label = "Receipt", onClick = onNavigateToDailyReceipt)
                    QuickActionIcon(icon = Icons.Filled.DateRange, label = "Analytics", onClick = onNavigateToAnalytics)
                }
            }
        }

        // Recent Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToTransactions() }.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = onNavigateToTransactions) {
                    Text("See all", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        items(state.recentTransactions, key = { it.id }) { tx ->
            TransactionRow(tx = tx)
        }
    }
}

@Composable
fun QuickActionIcon(icon: ImageVector, label: String, onClick: () -> Unit, isPrimary: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                icon, 
                contentDescription = label, 
                tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TransactionRow(tx: Transaction) {
    val isIncome = tx.type == "income"
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val amountPrefix = if (isIncome) "+" else "-"
    val icon = if (isIncome) Icons.Filled.KeyboardArrowDown else Icons.Filled.ShoppingCart

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.merchant ?: tx.category,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${tx.category} · ${tx.transactionDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "$amountPrefix${tx.amount.toINR()}",
            color = amountColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Removed ReceiptDialog
