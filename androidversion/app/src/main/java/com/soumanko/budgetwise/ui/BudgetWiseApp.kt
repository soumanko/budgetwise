package com.soumanko.budgetwise.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.soumanko.budgetwise.data.remote.SupabaseClient
import com.soumanko.budgetwise.data.repository.TransactionRepository
import com.soumanko.budgetwise.ui.auth.AuthViewModel
import com.soumanko.budgetwise.ui.auth.LoginScreen
import com.soumanko.budgetwise.ui.dashboard.DashboardScreen
import com.soumanko.budgetwise.ui.dashboard.DashboardViewModel
import com.soumanko.budgetwise.ui.dashboard.DashboardViewModelFactory
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

@Composable
fun BudgetWiseApp() {
    val authViewModel: AuthViewModel = viewModel()
    val sessionStatus by authViewModel.sessionStatus.collectAsState()

    val navController = rememberNavController()

    when (sessionStatus) {
        is SessionStatus.Initializing -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SessionStatus.Authenticated -> {
            MainAppShell(navController = navController, authViewModel = authViewModel)
        }
        else -> {
            LoginScreen(
                onLoginSuccess = { /* Automatically handled by session status */ },
                viewModel = authViewModel
            )
        }
    }
}

@Composable
fun MainAppShell(navController: NavHostController, authViewModel: AuthViewModel) {
    val transactionRepository = remember { TransactionRepository(SupabaseClient.client.postgrest) }
    val accountRepository = remember { com.soumanko.budgetwise.data.repository.AccountRepository(SupabaseClient.client.postgrest) }
    val profileRepository = remember { com.soumanko.budgetwise.data.repository.ProfileRepository(SupabaseClient.client.postgrest) }
    val recurringExpenseRepository = remember { com.soumanko.budgetwise.data.repository.RecurringExpenseRepository(SupabaseClient.client.postgrest) }
    val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(transactionRepository, profileRepository, recurringExpenseRepository))

    val items = listOf(
        Screen.Dashboard,
        Screen.Transactions,
        Screen.Assistant
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onLogout = { authViewModel.logout() }
                )
            }
            composable(Screen.Transactions.route) {
                val transactionsViewModel: com.soumanko.budgetwise.ui.transactions.TransactionsViewModel = viewModel(factory = com.soumanko.budgetwise.ui.transactions.TransactionsViewModelFactory(transactionRepository, accountRepository))
                com.soumanko.budgetwise.ui.transactions.TransactionsScreen(
                    viewModel = transactionsViewModel,
                    onNavigateToCreate = { navController.navigate("transaction_form") },
                    onNavigateToEdit = { id -> navController.navigate("transaction_form/$id") }
                )
            }
            composable("transaction_form") {
                val transactionsViewModel: com.soumanko.budgetwise.ui.transactions.TransactionsViewModel = viewModel(
                    navController.getBackStackEntry(Screen.Transactions.route),
                    factory = com.soumanko.budgetwise.ui.transactions.TransactionsViewModelFactory(transactionRepository, accountRepository)
                )
                com.soumanko.budgetwise.ui.transactions.TransactionFormScreen(
                    viewModel = transactionsViewModel,
                    authUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "",
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("transaction_form/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val transactionsViewModel: com.soumanko.budgetwise.ui.transactions.TransactionsViewModel = viewModel(
                    navController.getBackStackEntry(Screen.Transactions.route),
                    factory = com.soumanko.budgetwise.ui.transactions.TransactionsViewModelFactory(transactionRepository, accountRepository)
                )
                com.soumanko.budgetwise.ui.transactions.TransactionFormScreen(
                    viewModel = transactionsViewModel,
                    authUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "",
                    transactionId = id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Assistant.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("AI Assistant Screen (Placeholder)")
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Transactions : Screen("transactions", "Transactions", Icons.Filled.List)
    object Assistant : Screen("assistant", "Assistant", Icons.Filled.Person)
}
