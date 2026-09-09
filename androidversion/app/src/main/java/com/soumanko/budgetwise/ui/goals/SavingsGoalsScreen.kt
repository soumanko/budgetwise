package com.soumanko.budgetwise.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soumanko.budgetwise.data.model.SavingsGoal
import java.math.BigDecimal
import com.soumanko.budgetwise.domain.finance.toINR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: SavingsGoalsViewModel,
    onNavigateToCreate: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedGoalForContribution by remember { mutableStateOf<SavingsGoal?>(null) }
    var contributionAmountStr by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals") },
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Goal")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Filled.Add, contentDescription = "Add Goal")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is SavingsGoalsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SavingsGoalsUiState.Error -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadGoals() }) { Text("Retry") }
                    }
                }
                is SavingsGoalsUiState.Success -> {
                    if (state.goals.isEmpty()) {
                        Text("No savings goals found.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.goals, key = { it.id }) { goal ->
                                SavingsGoalCard(
                                    goal = goal,
                                    onContribute = { selectedGoalForContribution = goal }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedGoalForContribution != null) {
        val goal = selectedGoalForContribution!!
        AlertDialog(
            onDismissRequest = { 
                selectedGoalForContribution = null
                contributionAmountStr = ""
            },
            title = { Text("Contribute to ${goal.name}") },
            text = {
                Column {
                    Text("Add funds to this savings goal.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = contributionAmountStr,
                        onValueChange = { contributionAmountStr = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = contributionAmountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    if (amt > BigDecimal.ZERO) {
                        viewModel.contributeToGoal(goal.id, amt, goal.currentAmount) {
                            selectedGoalForContribution = null
                            contributionAmountStr = ""
                        }
                    }
                }) { Text("Contribute") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    selectedGoalForContribution = null 
                    contributionAmountStr = ""
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SavingsGoalCard(goal: SavingsGoal, onContribute: () -> Unit) {
    val pct = if (goal.targetAmount > BigDecimal.ZERO) {
        goal.currentAmount.toFloat() / goal.targetAmount.toFloat()
    } else 0f

    Card(modifier = Modifier.fillMaxWidth().clickable { onContribute() }) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("${goal.currentAmount.toINR()} / ${goal.targetAmount.toINR()}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { pct.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (goal.deadline != null) {
                Text("Target Date: ${goal.deadline}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
