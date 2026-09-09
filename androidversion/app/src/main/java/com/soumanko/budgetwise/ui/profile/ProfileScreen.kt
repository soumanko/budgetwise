package com.soumanko.budgetwise.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("") }
    var monthlyBudget by remember { mutableStateOf("") }
    var lowBalanceThreshold by remember { mutableStateOf("") }
    
    var dataInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success && !dataInitialized) {
            val profile = (uiState as ProfileUiState.Success).profile
            fullName = profile.fullName
            currency = profile.currency
            monthlyBudget = profile.monthlyBudget.toString()
            lowBalanceThreshold = profile.lowBalanceThreshold.toString()
            dataInitialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings") },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProfileUiState.Error -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadProfile() }) { Text("Retry") }
                    }
                }
                is ProfileUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Personal Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Currency (e.g. USD, EUR)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = monthlyBudget,
                            onValueChange = { monthlyBudget = it },
                            label = { Text("Global Monthly Budget") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = lowBalanceThreshold,
                            onValueChange = { lowBalanceThreshold = it },
                            label = { Text("Low Balance Threshold") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val budget = monthlyBudget.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val threshold = lowBalanceThreshold.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                if (fullName.isNotBlank() && currency.isNotBlank()) {
                                    viewModel.updateProfile(
                                        fullName = fullName,
                                        currency = currency,
                                        monthlyBudget = budget,
                                        lowBalanceThreshold = threshold,
                                        onSuccess = { /* Optionally show toast */ }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}
