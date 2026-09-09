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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Pagination trigger
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (uiState is TransactionsUiState.Success) {
                    val state = uiState as TransactionsUiState.Success
                    if (lastIndex != null && lastIndex >= state.transactions.size - 3) {
                        viewModel.loadNextPage()
                    }
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
                title = { Text("Transactions") },
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
                    }
                }
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
    Card(modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description ?: transaction.merchant ?: transaction.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${transaction.category} • ${transaction.transactionDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (transaction.type == "income") "+${transaction.amount.toINR()}" else "-${transaction.amount.toINR()}",
                    color = if (transaction.type == "income") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
