package com.soumanko.budgetwise.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soumanko.budgetwise.data.model.Transaction
import com.soumanko.budgetwise.domain.finance.toINR
import androidx.compose.ui.graphics.Color
import com.soumanko.budgetwise.ui.theme.IncomeGreen
import com.soumanko.budgetwise.ui.theme.ExpenseRed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Pagination trigger using derivedStateOf to prevent excessive recomposition/flow emissions
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState is TransactionsUiState.Success) {
            val state = uiState as TransactionsUiState.Success
            if (!state.isFetchingNextPage) {
                viewModel.loadNextPage()
            }
        }
    }

    var showDeleteDialog by remember { mutableStateOf<Transaction?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedType = "all"
    val selectedCategory = "all"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.setFilters(selectedType, selectedCategory, searchQuery)
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search transactions...") },
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is TransactionsUiState.LoadingInitial -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is TransactionsUiState.Error -> {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.loadInitial() }) { Text("Retry") }
                        }
                    }
                    is TransactionsUiState.Empty -> {
                        Text("No transactions found.", modifier = Modifier.align(Alignment.Center))
                    }
                    is TransactionsUiState.Success -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.transactions, key = { it.id }) { tx ->
                                TransactionItem(
                                    transaction = tx,
                                    onEdit = { onNavigateToEdit(tx.id) },
                                    onDelete = { showDeleteDialog = tx }
                                )
                            }
                            if (state.isFetchingNextPage) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Transaction?") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog?.let { viewModel.deleteTransaction(it.id) }
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isIncome = transaction.type == "income"
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val amountPrefix = if (isIncome) "+" else "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.category.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchant ?: transaction.category,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${transaction.category} · ${transaction.transactionDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix${transaction.amount.toINR()}",
                color = amountColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}
