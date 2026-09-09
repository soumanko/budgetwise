package com.soumanko.budgetwise.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soumanko.budgetwise.data.model.Profile
import com.soumanko.budgetwise.data.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = repository.getProfile()
            if (result.isSuccess) {
                _uiState.value = ProfileUiState.Success(result.getOrNull()!!)
            } else {
                _uiState.value = ProfileUiState.Error("Failed to load profile")
            }
        }
    }

    fun updateProfile(
        fullName: String,
        currency: String,
        monthlyBudget: BigDecimal,
        lowBalanceThreshold: BigDecimal,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ProfileUiState.Success) {
                val updatedProfile = currentState.profile.copy(
                    fullName = fullName,
                    currency = currency,
                    monthlyBudget = monthlyBudget,
                    lowBalanceThreshold = lowBalanceThreshold
                )
                repository.updateProfile(updatedProfile)
                loadProfile()
                onSuccess()
            }
        }
    }
    
    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            supabaseClient.auth.signOut()
            onLogoutSuccess()
        }
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: Profile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModelFactory(
    private val repository: ProfileRepository,
    private val supabaseClient: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository, supabaseClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
