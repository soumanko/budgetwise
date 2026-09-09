package com.soumanko.budgetwise.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import com.soumanko.budgetwise.domain.finance.toINR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (String, String) -> Unit // category, amount
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Budget")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Filled.Add, contentDescription = "Add Budget")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is BudgetsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is BudgetsUiState.Error -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadBudgets() }) { Text("Retry") }
                    }
                }
                is BudgetsUiState.Success -> {
                    if (state.budgets.isEmpty()) {
                        Text("No budgets defined for this month.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.budgets, key = { it.budget.category }) { progress ->
                                BudgetProgressCard(
                                    progress = progress,
                                    onClick = { onNavigateToEdit(progress.budget.category, progress.budget.amount.toString()) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetProgressCard(progress: BudgetProgress, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(progress.budget.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("${progress.spent.toINR()} / ${progress.budget.amount.toINR()}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.percentage.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (progress.percentage > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (progress.percentage > 1f) "Exceeded by ${(progress.spent - progress.budget.amount).toINR()}" else "${progress.remaining.toINR()} remaining",
                style = MaterialTheme.typography.bodySmall,
                color = if (progress.percentage > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
