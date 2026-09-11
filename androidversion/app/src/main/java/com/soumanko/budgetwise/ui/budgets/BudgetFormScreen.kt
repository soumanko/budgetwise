package com.soumanko.budgetwise.ui.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
fun BudgetFormScreen(
    viewModel: BudgetsViewModel,
    onNavigateBack: () -> Unit,
    initialCategory: String? = null,
    initialAmount: String? = null
) {
    var category by remember { mutableStateOf(initialCategory ?: "") }
    var amountStr by remember { mutableStateOf(initialAmount ?: "") }
    
    val uiState by viewModel.uiState.collectAsState()
    val isSaving = uiState is BudgetsUiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialCategory != null) "Edit Budget" else "Create Budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize()) {
            var categoryExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = categoryExpanded && initialCategory == null,
                onExpandedChange = { if (initialCategory == null) categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { if (initialCategory == null) ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded && initialCategory == null,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    com.soumanko.budgetwise.domain.finance.Categories.EXPENSE_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val amount = amountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    if (category.isNotBlank() && amount > BigDecimal.ZERO) {
                        viewModel.upsertBudget(category, amount) {
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
                    Text("Save Budget")
                }
            }
            if (uiState is BudgetsUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text((uiState as BudgetsUiState.Error).message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
