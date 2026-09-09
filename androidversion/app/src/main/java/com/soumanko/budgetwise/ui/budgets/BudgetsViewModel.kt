package com.soumanko.budgetwise.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soumanko.budgetwise.data.model.Budget
import com.soumanko.budgetwise.data.model.BudgetInsert
import com.soumanko.budgetwise.data.repository.BudgetRepository
import com.soumanko.budgetwise.data.repository.TransactionRepository
import com.soumanko.budgetwise.domain.finance.Calculations
import com.soumanko.budgetwise.domain.finance.DateUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class BudgetProgress(
    val budget: Budget,
    val spent: BigDecimal,
    val remaining: BigDecimal,
    val percentage: Float
)

class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<BudgetsUiState>(BudgetsUiState.Loading)
    val uiState: StateFlow<BudgetsUiState> = _uiState

    init {
        loadBudgets()
    }

    fun loadBudgets() {
        viewModelScope.launch {
            _uiState.value = BudgetsUiState.Loading
            val currentMonth = DateUtils.getMonthStart()
            
            val budgetsResult = budgetRepository.getBudgetsForMonth(currentMonth)
            if (budgetsResult.isFailure) {
                _uiState.value = BudgetsUiState.Error("Failed to load budgets")
                return@launch
            }
            
            val transactionsResult = transactionRepository.getTransactionsByDateRange(
                startDate = currentMonth,
                endDate = DateUtils.getMonthEnd()
            )
            
            val budgets = budgetsResult.getOrNull() ?: emptyList()
            val transactions = transactionsResult.getOrNull() ?: emptyList()
            
            val categorySpending = Calculations.calculateCategorySpending(transactions)
            
            val progressList = budgets.map { budget ->
                val spent = categorySpending.find { it.category == budget.category }?.amount ?: BigDecimal.ZERO
                val remaining = (budget.amount - spent).max(BigDecimal.ZERO)
                val pct = if (budget.amount > BigDecimal.ZERO) {
                    spent.toFloat() / budget.amount.toFloat()
                } else {
                    0f
                }
                BudgetProgress(budget, spent, remaining, pct)
            }
            
            _uiState.value = BudgetsUiState.Success(progressList)
        }
    }

    fun upsertBudget(category: String, amount: BigDecimal, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = supabaseClient.auth.currentUserOrNull()
            if (user == null) {
                _uiState.value = BudgetsUiState.Error("User not authenticated")
                return@launch
            }
            
            val insert = BudgetInsert(
                userId = user.id,
                category = category,
                amount = amount,
                month = DateUtils.getMonthStart()
            )
            
            val result = budgetRepository.upsertBudget(insert)
            if (result.isSuccess) {
                loadBudgets()
                onSuccess()
            } else {
                _uiState.value = BudgetsUiState.Error("Failed to save budget")
            }
        }
    }
}

sealed class BudgetsUiState {
    object Loading : BudgetsUiState()
    data class Success(val budgets: List<BudgetProgress>) : BudgetsUiState()
    data class Error(val message: String) : BudgetsUiState()
}

class BudgetsViewModelFactory(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val supabaseClient: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetsViewModel(budgetRepository, transactionRepository, supabaseClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
