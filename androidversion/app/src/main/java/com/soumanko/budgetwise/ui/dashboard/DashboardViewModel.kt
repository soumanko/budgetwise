package com.soumanko.budgetwise.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soumanko.budgetwise.data.model.Transaction
import com.soumanko.budgetwise.data.repository.ProfileRepository
import com.soumanko.budgetwise.data.repository.RecurringExpenseRepository
import com.soumanko.budgetwise.data.repository.TransactionRepository
import com.soumanko.budgetwise.domain.finance.Calculations
import com.soumanko.budgetwise.domain.finance.CategorySpending
import com.soumanko.budgetwise.domain.finance.DateUtils
import com.soumanko.budgetwise.domain.finance.FinanceCalculations
import com.soumanko.budgetwise.domain.finance.MonthlyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val balance: BigDecimal,
        val currentStats: MonthlyStats,
        val previousStats: MonthlyStats,
        val categorySpending: List<CategorySpending>,
        val safeToSpend: BigDecimal?,
        val recentTransactions: List<Transaction>
    ) : DashboardUiState()
    object Empty : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            transactionRepository.refreshSignal.collect {
                loadData()
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            
            // Get Balance from RPC
            val balanceResult = transactionRepository.getTotalBalance()
            if (balanceResult.isFailure) {
                _uiState.value = DashboardUiState.Error(
                    balanceResult.exceptionOrNull()?.message ?: "Failed to fetch balance"
                )
                return@launch
            }
            val balance = balanceResult.getOrNull() ?: BigDecimal.ZERO

            // Get transactions for current and previous month only
            val monthStart = DateUtils.getMonthStart()
            val monthEnd = DateUtils.getMonthEnd()
            val prevMonthStart = DateUtils.getPreviousMonthDateStr()
            val prevMonthEnd = DateUtils.getPreviousMonthEndStr()
            
            val txResult = transactionRepository.getTransactionsByDateRange(prevMonthStart, monthEnd)
            
            // Get Global Recent Transactions
            val recentResult = transactionRepository.getTransactionsPage(
                lastCreatedAt = null, lastId = null, limitCount = 8
            )

            // Get Profile and Recurring Expenses for SafeToSpend
            val profileResult = profileRepository.getProfile()
            val recurringResult = recurringExpenseRepository.getActiveRecurringExpenses()
            
            if (txResult.isSuccess && recentResult.isSuccess) {
                val transactions = txResult.getOrNull() ?: emptyList()
                val recentTransactions = recentResult.getOrNull() ?: emptyList()
                
                if (transactions.isEmpty() && recentTransactions.isEmpty() && balance == BigDecimal.ZERO) {
                    _uiState.value = DashboardUiState.Empty
                    return@launch
                }

                // Filter for current month
                val currentMonthTx = transactions.filter { 
                    it.transactionDate in monthStart..monthEnd 
                }

                // Filter for previous month
                val prevMonthTx = transactions.filter { 
                    it.transactionDate in prevMonthStart..prevMonthEnd 
                }

                // Calculations
                val currentStats = Calculations.calculateMonthlyStats(currentMonthTx)
                val previousStats = Calculations.calculateMonthlyStats(prevMonthTx)
                val categorySpending = Calculations.calculateCategorySpending(currentMonthTx)
                
                // Safe to spend calculation
                val profile = profileResult.getOrNull()
                val recurringExpenses = recurringResult.getOrNull() ?: emptyList()
                
                val monthlyBudget = profile?.monthlyBudget ?: BigDecimal.ZERO
                val upcomingRecurringTotal = recurringExpenses.fold(BigDecimal.ZERO) { acc, exp -> acc.add(exp.amount) }
                val daysRemaining = DateUtils.getDaysRemainingInMonth()
                
                val safeToSpend = FinanceCalculations.calculateSafeToSpend(
                    balance = balance,
                    daysRemaining = daysRemaining,
                    upcomingRecurringTotal = upcomingRecurringTotal,
                    monthlyBudget = monthlyBudget
                )

                _uiState.value = DashboardUiState.Success(
                    balance = balance,
                    currentStats = currentStats,
                    previousStats = previousStats,
                    categorySpending = categorySpending,
                    safeToSpend = safeToSpend,
                    recentTransactions = recentTransactions
                )

            } else {
                _uiState.value = DashboardUiState.Error(
                    txResult.exceptionOrNull()?.message ?: recentResult.exceptionOrNull()?.message ?: "Failed to load dashboard data"
                )
            }
        }
    }
}

class DashboardViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(transactionRepository, profileRepository, recurringExpenseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
