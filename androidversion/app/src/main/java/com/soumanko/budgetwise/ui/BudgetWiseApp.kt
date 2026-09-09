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
import com.soumanko.budgetwise.ui.auth.ForgotPasswordScreen
import com.soumanko.budgetwise.ui.auth.LoginScreen
import com.soumanko.budgetwise.ui.auth.SignupScreen
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
            var authRoute by remember { mutableStateOf("login") }
            
            when (authRoute) {
                "login" -> {
                    LoginScreen(
                        onLoginSuccess = { /* Automatically handled by session status */ },
                        onNavigateToSignup = { authRoute = "signup" },
                        onNavigateToForgotPassword = { authRoute = "forgot_password" },
                        viewModel = authViewModel
                    )
                }
                "signup" -> {
                    SignupScreen(
                        onNavigateToLogin = { authRoute = "login" },
                        viewModel = authViewModel
                    )
                }
                "forgot_password" -> {
                    ForgotPasswordScreen(
                        onNavigateToLogin = { authRoute = "login" },
                        viewModel = authViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppShell(navController: NavHostController, authViewModel: AuthViewModel) {
    val transactionRepository = remember { TransactionRepository(SupabaseClient.client.postgrest) }
    val accountRepository = remember { com.soumanko.budgetwise.data.repository.AccountRepository(SupabaseClient.client.postgrest) }
    val profileRepository = remember { com.soumanko.budgetwise.data.repository.ProfileRepository(SupabaseClient.client.postgrest) }
    val recurringExpenseRepository = remember { com.soumanko.budgetwise.data.repository.RecurringExpenseRepository(SupabaseClient.client.postgrest) }
    val budgetRepository = remember { com.soumanko.budgetwise.data.repository.BudgetRepository(SupabaseClient.client.postgrest) }
    val savingsGoalRepository = remember { com.soumanko.budgetwise.data.repository.SavingsGoalRepository(SupabaseClient.client.postgrest) }
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
                                popUpTo(navController.graph.findStartDestination().route ?: Screen.Dashboard.route) {
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
                com.soumanko.budgetwise.ui.dashboard.DashboardScreen(
                    viewModel = dashboardViewModel,
                    onLogout = { authViewModel.logout() },
                    onNavigateToBudgets = { navController.navigate("budgets") },
                    onNavigateToRecurring = { navController.navigate("recurring") },
                    onNavigateToSavingsGoals = { navController.navigate("savings_goals") },
                    onNavigateToAnalytics = { navController.navigate("analytics") },
                    onNavigateToProfile = { navController.navigate("profile") }
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
                val assistantViewModel: com.soumanko.budgetwise.ui.assistant.AssistantViewModel = viewModel(
                    factory = com.soumanko.budgetwise.ui.assistant.AssistantViewModelFactory(transactionRepository, budgetRepository, profileRepository, recurringExpenseRepository, SupabaseClient.client)
                )
                com.soumanko.budgetwise.ui.assistant.AssistantScreen(
                    viewModel = assistantViewModel
                )
            }
            composable("budgets") {
                val budgetsViewModel: com.soumanko.budgetwise.ui.budgets.BudgetsViewModel = viewModel(
                    factory = com.soumanko.budgetwise.ui.budgets.BudgetsViewModelFactory(budgetRepository, transactionRepository, SupabaseClient.client)
                )
                com.soumanko.budgetwise.ui.budgets.BudgetsScreen(
                    viewModel = budgetsViewModel,
                    onNavigateToCreate = { navController.navigate("budget_form") },
                    onNavigateToEdit = { category, amount -> navController.navigate("budget_form?category=$category&amount=$amount") }
                )
            }
            composable(
                "budget_form?category={category}&amount={amount}",
                arguments = listOf(
                    androidx.navigation.navArgument("category") { nullable = true; defaultValue = null },
                    androidx.navigation.navArgument("amount") { nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category")
                val amount = backStackEntry.arguments?.getString("amount")
                val budgetsViewModel: com.soumanko.budgetwise.ui.budgets.BudgetsViewModel = viewModel(
                    navController.getBackStackEntry("budgets"),
                    factory = com.soumanko.budgetwise.ui.budgets.BudgetsViewModelFactory(budgetRepository, transactionRepository, SupabaseClient.client)
                )
                com.soumanko.budgetwise.ui.budgets.BudgetFormScreen(
                    viewModel = budgetsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    initialCategory = category,
                    initialAmount = amount
                )
            }
            composable("recurring") {
                val recurringViewModel: com.soumanko.budgetwise.ui.recurring.RecurringExpensesViewModel = viewModel(
                    factory = com.soumanko.budgetwise.ui.recurring.RecurringExpensesViewModelFactory(recurringExpenseRepository, SupabaseClient.client)
                )
                com.soumanko.budgetwise.ui.recurring.RecurringExpensesScreen(
                    viewModel = recurringViewModel,
                    onNavigateToCreate = { navController.navigate("recurring_form") },
                    onNavigateToEdit = { id -> navController.navigate("recurring_form/$id") }
                )
            }
            composable("recurring_form") {
                val recurringViewModel: com.soumanko.budgetwise.ui.recurring.RecurringExpensesViewModel = viewModel(
                    navController.getBackStackEntry("recurring"),
                    factory = com.soumanko.budgetwise.ui.recurring.RecurringExpensesViewModelFactory(recurringExpenseRepository, SupabaseClient.client)
                )
                com.soumanko.budgetwise.ui.recurring.RecurringExpenseFormScreen(
                    viewModel = recurringViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("recurring_form/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val recurringViewModel: com.soumanko.budgetwise.ui.recurring.RecurringExpensesViewModel = viewModel(
                    navController.getBackStackEntry("recurring"),
                    factory = com.soumanko.budgetwise.ui.recurring.RecurringExpensesViewModelFactory(recurringExpenseRepository, SupabaseClient.client)
                )
                com.soumanko.budgetwise.ui.recurring.RecurringExpenseFormScreen(
                    viewModel = recurringViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    expenseId = id
                )
            }
            composable("savings_goals") {
                val goalsViewModel: com.soumanko.budgetwise.ui.goals.SavingsGoalsViewModel = viewModel(
                    factory = com.soumanko.budgetwise.ui.goals.SavingsGoalsViewModelFactory(savingsGoalRepository, SupabaseClient.client)
                )
                com.soumanko.budgetwise.ui.goals.SavingsGoalsScreen(
                    viewModel = goalsViewModel,
                    onNavigateToCreate = { navController.navigate("goal_form") }
                )
            }
            composable("goal_form") {
                val goalsViewModel: com.soumanko.budgetwise.ui.goals.SavingsGoalsViewModel = viewModel(
                    navController.getBackStackEntry("savings_goals"),
                    factory = com.soumanko.budgetwise.ui.goals.SavingsGoalsViewModelFactory(savingsGoalRepository, SupabaseClient.client)
                )
                com.soumanko.budgetwise.ui.goals.GoalFormScreen(
                    viewModel = goalsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("profile") {
                val profileViewModel: com.soumanko.budgetwise.ui.profile.ProfileViewModel = viewModel(
                    factory = com.soumanko.budgetwise.ui.profile.ProfileViewModelFactory(profileRepository, SupabaseClient.client)
                )
                com.soumanko.budgetwise.ui.profile.ProfileScreen(
                    viewModel = profileViewModel,
                    onLogout = { navController.navigate("login") { popUpTo(0) } }
                )
            }
            composable("analytics") {
                val analyticsViewModel: com.soumanko.budgetwise.ui.analytics.AnalyticsViewModel = viewModel(
                    factory = com.soumanko.budgetwise.ui.analytics.AnalyticsViewModelFactory(transactionRepository)
                )
                com.soumanko.budgetwise.ui.analytics.AnalyticsScreen(
                    viewModel = analyticsViewModel
                )
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Transactions : Screen("transactions", "Transactions", Icons.Filled.List)
    object Assistant : Screen("assistant", "Assistant", Icons.Filled.Person)
}
