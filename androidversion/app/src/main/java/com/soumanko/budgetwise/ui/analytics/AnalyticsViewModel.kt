package com.soumanko.budgetwise.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soumanko.budgetwise.data.repository.TransactionRepository
import com.soumanko.budgetwise.domain.finance.Calculations
import com.soumanko.budgetwise.domain.finance.CategorySpending
import com.soumanko.budgetwise.domain.finance.DailySpending
import com.soumanko.budgetwise.domain.finance.MonthlyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.value = AnalyticsUiState.Loading
            
            val monthStart = com.soumanko.budgetwise.domain.finance.DateUtils.getMonthStart()
            val monthEnd = com.soumanko.budgetwise.domain.finance.DateUtils.getMonthEnd()
            val result = transactionRepository.getTransactionsByDateRange(monthStart, monthEnd)
            
            if (result.isSuccess) {
                val transactions = result.getOrNull() ?: emptyList()
                val monthlyStats = Calculations.calculateMonthlyStats(transactions)
                val categorySpending = Calculations.calculateCategorySpending(transactions)
                val dailySpending = Calculations.calculateDailySpending(transactions)
                
                _uiState.value = AnalyticsUiState.Success(
                    monthlyStats = monthlyStats,
                    categorySpending = categorySpending,
                    dailySpending = dailySpending
                )
            } else {
                _uiState.value = AnalyticsUiState.Error("Failed to load transactions for analytics")
            }
        }
    }
}

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(
        val monthlyStats: MonthlyStats,
        val categorySpending: List<CategorySpending>,
        val dailySpending: List<DailySpending>
    ) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}

class AnalyticsViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
