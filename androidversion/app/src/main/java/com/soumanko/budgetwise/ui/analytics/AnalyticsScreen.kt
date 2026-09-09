package com.soumanko.budgetwise.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soumanko.budgetwise.domain.finance.toINR
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is AnalyticsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AnalyticsUiState.Error -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadAnalytics() }) { Text("Retry") }
                    }
                }
                is AnalyticsUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Monthly Stats
                        Text("Monthly Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCard("Income", "+${state.monthlyStats.totalIncome.toINR()}", Color(0xFF4CAF50), Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(16.dp))
                            StatCard("Expenses", "-${state.monthlyStats.totalExpenses.toINR()}", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCard("Net Savings", state.monthlyStats.netSavings.toINR(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(16.dp))
                            StatCard("Savings Rate", "${state.monthlyStats.savingsRate}%", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        // Category Spending
                        Text("Category Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (state.categorySpending.isEmpty()) {
                            Text("No expense data available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.categorySpending.forEach { category ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(category.category, style = MaterialTheme.typography.bodyLarge)
                                        LinearProgressIndicator(
                                            progress = { (category.percentage.toFloat() / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(category.amount.toINR(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text("${category.percentage}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Daily Spending (basic representation)
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Daily Spending", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (state.dailySpending.isEmpty()) {
                            Text("No daily data available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.dailySpending.takeLast(7).forEach { day -> // Show last 7 days roughly
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(day.date)
                                    Text("-${day.amount.toINR()}", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
