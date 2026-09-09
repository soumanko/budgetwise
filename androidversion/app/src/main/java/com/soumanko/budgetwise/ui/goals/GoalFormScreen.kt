package com.soumanko.budgetwise.ui.goals

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
fun GoalFormScreen(
    viewModel: SavingsGoalsViewModel,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }
    var initialAmountStr by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val isSaving = uiState is SavingsGoalsUiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Savings Goal") },
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
                label = { Text("Goal Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = targetAmountStr,
                onValueChange = { targetAmountStr = it },
                label = { Text("Target Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = initialAmountStr,
                onValueChange = { initialAmountStr = it },
                label = { Text("Initial Amount Saved (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = deadline,
                onValueChange = { deadline = it },
                label = { Text("Target Date (YYYY-MM-DD) (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val target = targetAmountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val current = initialAmountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    if (name.isNotBlank() && target > BigDecimal.ZERO) {
                        viewModel.createGoal(
                            name = name,
                            targetAmount = target,
                            currentAmount = current,
                            deadline = deadline.takeIf { it.isNotBlank() },
                            description = description.takeIf { it.isNotBlank() }
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
                    Text("Save Goal")
                }
            }
        }
    }
}
