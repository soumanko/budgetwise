package com.soumanko.budgetwise.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soumanko.budgetwise.domain.finance.CategorySpending
import com.soumanko.budgetwise.domain.finance.DailySpending
import com.soumanko.budgetwise.domain.finance.MonthlyStats
import com.soumanko.budgetwise.domain.finance.toINR
import com.soumanko.budgetwise.ui.theme.BudgetTurquoise
import com.soumanko.budgetwise.ui.theme.ExpenseRed
import com.soumanko.budgetwise.ui.theme.IncomeGreen
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Analytics") })
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
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // 1. SUMMARY
                        Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column {
                                Text("Total Spent", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.monthlyStats.totalExpenses.toINR(),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                                    ),
                                    color = ExpenseRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("+${state.monthlyStats.totalIncome.toINR()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IncomeGreen)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Savings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(state.monthlyStats.netSavings.toINR(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BudgetTurquoise)
                                }
                            }
                        }

                        // 2. INTERPRETATION
                        Text("Interpretation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        
                        val peakDay = state.dailySpending.maxByOrNull { it.amount }
                        if (peakDay != null && peakDay.amount > BigDecimal.ZERO) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔥", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Highest spending day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${peakDay.date} — ${peakDay.amount.toINR()}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ExpenseRed)
                                }
                            }
                        }

                        if (state.categorySpending.isNotEmpty()) {
                            val topCategory = state.categorySpending.maxByOrNull { it.amount }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("💡", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("AI Insight", style = MaterialTheme.typography.titleMedium, color = BudgetTurquoise, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "You've spent ${topCategory?.percentage}% of your expenses on ${topCategory?.category}. Consider rebalancing if this exceeds your target.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        // 3. ACTION
                        Text("Action", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onNavigateToBudgets,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BudgetTurquoise, contentColor = Color.Black)
                            ) {
                                Text("Review Budgets", style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedButton(
                                onClick = onNavigateToTransactions,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Audit Recent Transactions", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
