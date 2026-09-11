package com.soumanko.budgetwise.ui.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soumanko.budgetwise.data.model.Transaction
import com.soumanko.budgetwise.data.repository.TransactionRepository
import com.soumanko.budgetwise.domain.finance.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReceiptUiState {
    object Loading : ReceiptUiState()
    data class Success(
        val transactions: List<Transaction>,
        val totalExpenses: java.math.BigDecimal,
        val paymentMethodCount: Int,
        val topCategory: String?
    ) : ReceiptUiState()
    data class Error(val message: String) : ReceiptUiState()
}

class ReceiptViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReceiptUiState>(ReceiptUiState.Loading)
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.Loading
            val today = DateUtils.getTodayDateStr()
            
            transactionRepository.getTransactionsByDateRange(today, today).fold(
                onSuccess = { transactions ->
                    val expenses = transactions.filter { it.type == "expense" }
                    val totalExpenses = expenses.sumOf { it.amount }
                    val paymentMethods = transactions.mapNotNull { it.paymentMethod }.distinct().size
                    val topCategory = expenses.groupBy { it.category }
                        .maxByOrNull { (_, list) -> list.sumOf { it.amount } }?.key
                        
                    _uiState.value = ReceiptUiState.Success(
                        transactions = transactions,
                        totalExpenses = totalExpenses,
                        paymentMethodCount = paymentMethods,
                        topCategory = topCategory
                    )
                },
                onFailure = { error ->
                    _uiState.value = ReceiptUiState.Error(error.message ?: "Failed to load receipt")
                }
            )
        }
    }
}

class ReceiptViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReceiptViewModel(transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
