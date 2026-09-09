package com.soumanko.budgetwise.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soumanko.budgetwise.domain.finance.toINR
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onLogout: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToSavingsGoals: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BudgetWise") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile")
                    }
                }
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
                        Text("No transactions yet. Add some to get started!")
                    }
                }
                is DashboardUiState.Success -> {
                    DashboardContent(
                        state = state,
                        onNavigateToBudgets = onNavigateToBudgets,
                        onNavigateToRecurring = onNavigateToRecurring,
                        onNavigateToSavingsGoals = onNavigateToSavingsGoals,
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onNavigateToProfile = onNavigateToProfile
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
    onNavigateToProfile: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Card
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Total Balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.balance.toINR(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Quick Actions
        item {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AssistChip(onClick = onNavigateToBudgets, label = { Text("Budgets") })
                }
                item {
                    AssistChip(onClick = onNavigateToRecurring, label = { Text("Recurring") })
                }
                item {
                    AssistChip(onClick = onNavigateToSavingsGoals, label = { Text("Goals") })
                }
                item {
                    AssistChip(onClick = onNavigateToAnalytics, label = { Text("Analytics") })
                }
            }
        }

        // Safe to Spend
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Safe to Spend (Daily)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (state.safeToSpend != null) {
                        Text(state.safeToSpend.toINR(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        Text("Budget data unavailable", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
        
        // Income vs Expenses
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Income", style = MaterialTheme.typography.labelSmall)
                        Text("+${state.currentStats.totalIncome.toINR()}", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4CAF50))
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Expenses", style = MaterialTheme.typography.labelSmall)
                        Text("-${state.currentStats.totalExpenses.toINR()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        // Recent Transactions
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Recent Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        
        items(state.recentTransactions) { tx ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(tx.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tx.transactionDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = if (tx.type == "income") "+${tx.amount.toINR()}" else "-${tx.amount.toINR()}",
                        color = if (tx.type == "income") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
