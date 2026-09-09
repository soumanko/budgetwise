package com.soumanko.budgetwise.ui.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseFormScreen(
    viewModel: RecurringExpensesViewModel,
    onNavigateBack: () -> Unit,
    expenseId: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // In a real app we'd fetch the expense if editing, for brevity we'll just start empty if id is not null here.
    // Or we find it in the current list since we already loaded it.
    val existingExpense = if (uiState is RecurringExpensesUiState.Success) {
        (uiState as RecurringExpensesUiState.Success).expenses.find { it.id == expenseId }
    } else null

    var name by remember { mutableStateOf(existingExpense?.name ?: "") }
    var amountStr by remember { mutableStateOf(existingExpense?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(existingExpense?.category ?: "") }
    var frequency by remember { mutableStateOf(existingExpense?.frequency ?: "monthly") }
    var nextDueDate by remember { mutableStateOf(existingExpense?.nextDueDate ?: "") }
    var notes by remember { mutableStateOf(existingExpense?.notes ?: "") }

    val isSaving = uiState is RecurringExpensesUiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId != null) "Edit Recurring Expense" else "Add Recurring Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Basic dropdown replacement
            OutlinedTextField(
                value = frequency,
                onValueChange = { frequency = it },
                label = { Text("Frequency (e.g. monthly, yearly)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = nextDueDate,
                onValueChange = { nextDueDate = it },
                label = { Text("Next Due Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val amount = amountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    if (name.isNotBlank() && category.isNotBlank() && nextDueDate.isNotBlank() && amount > BigDecimal.ZERO) {
                        viewModel.upsertExpense(
                            id = expenseId,
                            name = name,
                            amount = amount,
                            category = category,
                            frequency = frequency,
                            nextDueDate = nextDueDate,
                            notes = notes.takeIf { it.isNotBlank() }
                        ) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Expense")
                }
            }
        }
    }
}
