package com.soumanko.budgetwise.ui.statement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soumanko.budgetwise.data.model.Transaction
import com.soumanko.budgetwise.data.repository.TransactionRepository
import com.soumanko.budgetwise.domain.finance.DateUtils
import com.soumanko.budgetwise.utils.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

sealed class StatementUiState {
    object Idle : StatementUiState()
    object Loading : StatementUiState()
    data class Success(
        val transactions: List<Transaction>,
        val startDate: String,
        val endDate: String,
        val totalIncome: BigDecimal,
        val totalExpenses: BigDecimal,
        val netChange: BigDecimal,
        val topCategory: String?,
        val pdfUri: String? = null
    ) : StatementUiState()
    data class Error(val message: String) : StatementUiState()
}

class StatementViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatementUiState>(StatementUiState.Idle)
    val uiState: StateFlow<StatementUiState> = _uiState.asStateFlow()

    fun generateStatement(startDate: String, endDate: String) {
        viewModelScope.launch {
            _uiState.value = StatementUiState.Loading
            
            transactionRepository.getTransactionsByDateRange(startDate, endDate).fold(
                onSuccess = { transactions ->
                    val expenses = transactions.filter { it.type == "expense" }
                    val incomes = transactions.filter { it.type == "income" }
                    
                    val totalExpenses = expenses.sumOf { it.amount }
                    val totalIncome = incomes.sumOf { it.amount }
                    val netChange = totalIncome.subtract(totalExpenses)
                    
                    val topCategory = expenses.groupBy { it.category }
                        .maxByOrNull { (_, list) -> list.sumOf { it.amount } }?.key
                        
                    _uiState.value = StatementUiState.Success(
                        transactions = transactions.sortedBy { it.transactionDate },
                        startDate = startDate,
                        endDate = endDate,
                        totalIncome = totalIncome,
                        totalExpenses = totalExpenses,
                        netChange = netChange,
                        topCategory = topCategory
                    )
                },
                onFailure = { error ->
                    _uiState.value = StatementUiState.Error(error.message ?: "Failed to generate statement")
                }
            )
        }
    }
    
    fun exportToPdf(context: Context, state: StatementUiState.Success, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                PdfGenerator.generateFinancialStatementPdf(context, state)
            }
            if (uri != null) {
                val currentState = _uiState.value
                if (currentState is StatementUiState.Success) {
                    _uiState.value = currentState.copy(pdfUri = uri.toString())
                }
                onComplete(uri.toString())
            } else {
                _uiState.value = StatementUiState.Error("Failed to generate PDF")
            }
        }
    }
}

class StatementViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatementViewModel(transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
