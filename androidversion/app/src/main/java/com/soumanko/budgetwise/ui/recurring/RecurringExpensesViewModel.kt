package com.soumanko.budgetwise.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soumanko.budgetwise.data.model.RecurringExpense
import com.soumanko.budgetwise.data.model.RecurringExpenseInsert
import com.soumanko.budgetwise.data.repository.RecurringExpenseRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class RecurringExpensesViewModel(
    private val repository: RecurringExpenseRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecurringExpensesUiState>(RecurringExpensesUiState.Loading)
    val uiState: StateFlow<RecurringExpensesUiState> = _uiState

    init {
        loadExpenses()
    }

    fun loadExpenses() {
        viewModelScope.launch {
            _uiState.value = RecurringExpensesUiState.Loading
            val result = repository.getActiveRecurringExpenses()
            if (result.isSuccess) {
                _uiState.value = RecurringExpensesUiState.Success(result.getOrNull() ?: emptyList())
            } else {
                _uiState.value = RecurringExpensesUiState.Error("Failed to load recurring expenses")
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(id)
            loadExpenses()
        }
    }

    fun toggleExpenseActive(expense: RecurringExpense) {
        viewModelScope.launch {
            repository.updateRecurringExpense(expense.copy(active = !expense.active))
            loadExpenses()
        }
    }

    fun upsertExpense(
        id: String?,
        name: String,
        amount: BigDecimal,
        category: String,
        frequency: String,
        nextDueDate: String,
        notes: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val user = supabaseClient.auth.currentUserOrNull() ?: return@launch
            if (id == null) {
                val insert = RecurringExpenseInsert(
                    userId = user.id,
                    name = name,
                    amount = amount,
                    category = category,
                    frequency = frequency,
                    nextDueDate = nextDueDate,
                    active = true,
                    notes = notes
                )
                repository.insertRecurringExpense(insert)
            } else {
                val existingResult = repository.getRecurringExpense(id)
                val existing = existingResult.getOrNull() ?: return@launch
                repository.updateRecurringExpense(existing.copy(
                    name = name,
                    amount = amount,
                    category = category,
                    frequency = frequency,
                    nextDueDate = nextDueDate,
                    notes = notes
                ))
            }
            loadExpenses()
            onSuccess()
        }
    }
}

sealed class RecurringExpensesUiState {
    object Loading : RecurringExpensesUiState()
    data class Success(val expenses: List<RecurringExpense>) : RecurringExpensesUiState()
    data class Error(val message: String) : RecurringExpensesUiState()
}

class RecurringExpensesViewModelFactory(
    private val repository: RecurringExpenseRepository,
    private val supabaseClient: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecurringExpensesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecurringExpensesViewModel(repository, supabaseClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
