package com.soumanko.budgetwise.ui.statement

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soumanko.budgetwise.domain.finance.DateUtils
import com.soumanko.budgetwise.domain.finance.toINR
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementScreen(
    viewModel: StatementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Date Range Picker State
    var showDatePicker by remember { mutableStateOf(true) }
    val dateRangePickerState = rememberDateRangePickerState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Statement") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (!showDatePicker && uiState is StatementUiState.Success) {
                val state = uiState as StatementUiState.Success
                BottomAppBar(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.exportToPdf(context, state) { uriStr ->
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(uriStr))
                                    type = "application/pdf"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Statement")
                                context.startActivity(shareIntent)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Export & Share", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export PDF")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (showDatePicker) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val startMillis = dateRangePickerState.selectedStartDateMillis
                            val endMillis = dateRangePickerState.selectedEndDateMillis
                            if (startMillis != null && endMillis != null) {
                                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val startStr = formatter.format(Date(startMillis))
                                val endStr = formatter.format(Date(endMillis))
                                viewModel.generateStatement(startStr, endStr)
                                showDatePicker = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                    ) {
                        Text("Generate Statement")
                    }
                }
            } else {
                when (val state = uiState) {
                    is StatementUiState.Idle -> { }
                    is StatementUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is StatementUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showDatePicker = true }) {
                                Text("Back")
                            }
                        }
                    }
                    is StatementUiState.Success -> {
                        StatementPreview(state)
                    }
                }
            }
        }
    }
}

@Composable
fun StatementPreview(state: StatementUiState.Success) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text("BUDGETWISE FINANCIAL STATEMENT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Period: ${state.startDate} to ${state.endDate}", style = MaterialTheme.typography.bodyMedium)
            Text("Generated: ${DateUtils.getTodayDateStr()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("SUMMARY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Income", color = com.soumanko.budgetwise.ui.theme.IncomeGreen)
                Text("+${state.totalIncome.toINR()}", color = com.soumanko.budgetwise.ui.theme.IncomeGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Expenses", color = com.soumanko.budgetwise.ui.theme.ExpenseRed)
                Text("-${state.totalExpenses.toINR()}", color = com.soumanko.budgetwise.ui.theme.ExpenseRed, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Net Activity", fontWeight = FontWeight.Bold)
                val netColor = if (state.netChange >= java.math.BigDecimal.ZERO) com.soumanko.budgetwise.ui.theme.IncomeGreen else com.soumanko.budgetwise.ui.theme.ExpenseRed
                val prefix = if (state.netChange >= java.math.BigDecimal.ZERO) "+" else ""
                Text("$prefix${state.netChange.toINR()}", fontWeight = FontWeight.Bold, color = netColor)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("TRANSACTIONS (${state.transactions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        items(state.transactions) { tx ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tx.merchant ?: tx.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("${tx.transactionDate} · ${tx.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val isIncome = tx.type == "income"
                Text(
                    text = "${if (isIncome) "+" else "-"}${tx.amount.toINR()}",
                    color = if (isIncome) com.soumanko.budgetwise.ui.theme.IncomeGreen else com.soumanko.budgetwise.ui.theme.ExpenseRed,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        if (state.transactions.isEmpty()) {
            item {
                Text("No transactions in this period.", modifier = Modifier.padding(vertical = 16.dp))
            }
        }
    }
}
