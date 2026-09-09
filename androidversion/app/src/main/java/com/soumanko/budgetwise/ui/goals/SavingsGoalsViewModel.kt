package com.soumanko.budgetwise.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soumanko.budgetwise.data.model.SavingsGoal
import com.soumanko.budgetwise.data.model.SavingsGoalInsert
import com.soumanko.budgetwise.data.repository.SavingsGoalRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SavingsGoalsViewModel(
    private val repository: SavingsGoalRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<SavingsGoalsUiState>(SavingsGoalsUiState.Loading)
    val uiState: StateFlow<SavingsGoalsUiState> = _uiState

    init {
        loadGoals()
    }

    fun loadGoals() {
        viewModelScope.launch {
            _uiState.value = SavingsGoalsUiState.Loading
            val result = repository.getSavingsGoals()
            if (result.isSuccess) {
                _uiState.value = SavingsGoalsUiState.Success(result.getOrNull() ?: emptyList())
            } else {
                _uiState.value = SavingsGoalsUiState.Error("Failed to load savings goals")
            }
        }
    }

    fun createGoal(
        name: String,
        targetAmount: BigDecimal,
        currentAmount: BigDecimal,
        deadline: String?,
        description: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val user = supabaseClient.auth.currentUserOrNull() ?: return@launch
            val insert = SavingsGoalInsert(
                userId = user.id,
                name = name,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                deadline = deadline,
                description = description
            )
            repository.createSavingsGoal(insert)
            loadGoals()
            onSuccess()
        }
    }

    fun contributeToGoal(id: String, amount: BigDecimal, currentAmount: BigDecimal, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val newAmount = currentAmount + amount
            repository.updateGoalAmount(id, newAmount)
            loadGoals()
            onSuccess()
        }
    }
}

sealed class SavingsGoalsUiState {
    object Loading : SavingsGoalsUiState()
    data class Success(val goals: List<SavingsGoal>) : SavingsGoalsUiState()
    data class Error(val message: String) : SavingsGoalsUiState()
}

class SavingsGoalsViewModelFactory(
    private val repository: SavingsGoalRepository,
    private val supabaseClient: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavingsGoalsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavingsGoalsViewModel(repository, supabaseClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
