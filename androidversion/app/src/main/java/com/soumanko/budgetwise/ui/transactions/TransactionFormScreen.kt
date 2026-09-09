package com.soumanko.budgetwise.ui.transactions

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soumanko.budgetwise.data.model.TransactionInsert
import com.soumanko.budgetwise.domain.finance.Categories
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    viewModel: TransactionsViewModel,
    authUserId: String,
    transactionId: String? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val existingTx = if (transactionId != null) {
        val state = viewModel.uiState.value
        if (state is TransactionsUiState.Success) state.transactions.find { it.id == transactionId } else null
    } else null

    var type by remember { mutableStateOf(existingTx?.type ?: "expense") }
    var amount by remember { mutableStateOf(existingTx?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(existingTx?.category ?: "") }
    var date by remember { mutableStateOf(existingTx?.transactionDate ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var description by remember { mutableStateOf(existingTx?.description ?: "") }
    var merchant by remember { mutableStateOf(existingTx?.merchant ?: "") }
    var paymentMethod by remember { mutableStateOf(existingTx?.paymentMethod ?: "") }
    var notes by remember { mutableStateOf(existingTx?.notes ?: "") }

    var isSubmitting by remember { mutableStateOf(false) }

    // Date Picker state
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() // Simplification for initialization
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.of("UTC")).toLocalDate()
                        date = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == null) "Add Transaction" else "Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Segmented Button / TabRow
            TabRow(selectedTabIndex = if (type == "expense") 0 else 1) {
                Tab(
                    selected = type == "expense",
                    onClick = { 
                        type = "expense" 
                        if (!Categories.EXPENSE_CATEGORIES.contains(category)) category = ""
                    },
                    text = { Text("Expense") }
                )
                Tab(
                    selected = type == "income",
                    onClick = { 
                        type = "income" 
                        if (!Categories.INCOME_CATEGORIES.contains(category)) category = ""
                    },
                    text = { Text("Income") }
                )
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount *") },
                leadingIcon = { Text("₹", modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category Dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            val categories = if (type == "expense") Categories.EXPENSE_CATEGORIES else Categories.INCOME_CATEGORIES
            
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.weight(1f)
                )

                if (type == "expense") {
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Merchant") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Payment Method Dropdown
                var pmExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = pmExpanded,
                    onExpandedChange = { pmExpanded = !pmExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pmExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = pmExpanded,
                        onDismissRequest = { pmExpanded = false }
                    ) {
                        Categories.PAYMENT_METHODS.forEach { pm ->
                            DropdownMenuItem(
                                text = { Text(pm) },
                                onClick = {
                                    paymentMethod = pm
                                    pmExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Picker trigger
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date *") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull()
                    if (parsedAmount == null || parsedAmount <= 0) {
                        Toast.makeText(context, "Please enter a valid amount greater than 0", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (parsedAmount > 10000000) {
                        Toast.makeText(context, "Amount exceeds maximum limit", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (category.isBlank()) {
                        Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val accId = viewModel.primaryAccountId
                    if (accId == null) {
                        Toast.makeText(context, "No account selected. Please create an account first.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    coroutineScope.launch {
                        if (existingTx != null) {
                            val updated = existingTx.copy(
                                type = type,
                                amount = BigDecimal(amount),
                                category = category,
                                merchant = merchant.ifBlank { null },
                                paymentMethod = paymentMethod.ifBlank { null },
                                transactionDate = date,
                                description = description.ifBlank { null },
                                notes = notes.ifBlank { null }
                            )
                            viewModel.repository.updateTransaction(updated)
                        } else {
                            val insert = TransactionInsert(
                                userId = authUserId,
                                accountId = accId,
                                type = type,
                                amount = BigDecimal(amount),
                                category = category,
                                merchant = merchant.ifBlank { null },
                                paymentMethod = paymentMethod.ifBlank { null },
                                transactionDate = date,
                                description = description.ifBlank { null },
                                notes = notes.ifBlank { null }
                            )
                            viewModel.repository.addTransaction(insert)
                        }
                        viewModel.refreshAfterMutation()
                        isSubmitting = false
                        Toast.makeText(context, "Transaction saved", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "income") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (isSubmitting) "Saving..." else "Save Transaction")
            }
        }
    }
}
