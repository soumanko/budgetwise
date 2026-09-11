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
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import java.math.BigDecimal

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val balance: BigDecimal,
        val currentStats: MonthlyStats,
        val previousStats: MonthlyStats,
        val categorySpending: List<CategorySpending>,
        val safeToSpend: BigDecimal?,
        val recentTransactions: List<Transaction>,
        val todayTransactions: List<Transaction>
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
    
    private var fetchJob: Job? = null

    init {
        loadData()
        viewModelScope.launch {
            transactionRepository.refreshSignal.collect {
                loadData()
            }
        }
    }

    fun loadData() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (_uiState.value !is DashboardUiState.Success) {
                _uiState.value = DashboardUiState.Loading
            }
            
            val monthStart = DateUtils.getMonthStart()
            val monthEnd = DateUtils.getMonthEnd()
            val prevMonthStart = DateUtils.getPreviousMonthDateStr()
            val prevMonthEnd = DateUtils.getPreviousMonthEndStr()

            // Run network reads concurrently
            val balanceDeferred = async { transactionRepository.getTotalBalance() }
            val txDeferred = async { transactionRepository.getTransactionsByDateRange(prevMonthStart, monthEnd) }
            val recentDeferred = async { transactionRepository.getTransactionsPage(lastCreatedAt = null, lastId = null, limitCount = 8) }
            val profileDeferred = async { profileRepository.getProfile() }
            val recurringDeferred = async { recurringExpenseRepository.getActiveRecurringExpenses() }

            val balanceResult = balanceDeferred.await()
            if (balanceResult.isFailure) {
                _uiState.value = DashboardUiState.Error(
                    balanceResult.exceptionOrNull()?.message ?: "Failed to fetch balance"
                )
                return@launch
            }
            val balance = balanceResult.getOrNull() ?: BigDecimal.ZERO
            
            val txResult = txDeferred.await()
            val recentResult = recentDeferred.await()
            val profileResult = profileDeferred.await()
            val recurringResult = recurringDeferred.await()
            
            if (txResult.isSuccess && recentResult.isSuccess) {
                val transactions = txResult.getOrNull() ?: emptyList()
                val recentTransactions = recentResult.getOrNull() ?: emptyList()
                
                if (transactions.isEmpty() && recentTransactions.isEmpty() && balance.compareTo(BigDecimal.ZERO) == 0) {
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
                
                // Safe to spend calculation semantics
                // Do not fabricate if optional inputs fail
                val safeToSpend = if (profileResult.isSuccess && recurringResult.isSuccess) {
                    val profile = profileResult.getOrNull()
                    if (profile != null) {
                        val recurringExpenses = recurringResult.getOrNull() ?: emptyList()
                        val monthlyBudget = profile.monthlyBudget
                        val upcomingRecurringTotal = recurringExpenses.fold(BigDecimal.ZERO) { acc, exp -> acc.add(exp.amount) }
                        val daysRemaining = DateUtils.getDaysRemainingInMonth()
                        
                        FinanceCalculations.calculateSafeToSpend(
                            balance = balance,
                            daysRemaining = daysRemaining,
                            upcomingRecurringTotal = upcomingRecurringTotal,
                            monthlyBudget = monthlyBudget
                        )
                    } else null
                } else {
                    null
                }

                val todayStr = DateUtils.getTodayDateStr()
                val todayTransactions = currentMonthTx.filter { it.transactionDate == todayStr }

                _uiState.value = DashboardUiState.Success(
                    balance = balance,
                    currentStats = currentStats,
                    previousStats = previousStats,
                    categorySpending = categorySpending,
                    safeToSpend = safeToSpend,
                    recentTransactions = recentTransactions,
                    todayTransactions = todayTransactions
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
