package com.soumanko.budgetwise.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soumanko.budgetwise.data.model.Transaction
import com.soumanko.budgetwise.data.repository.AccountRepository
import com.soumanko.budgetwise.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TransactionsUiState {
    object LoadingInitial : TransactionsUiState()
    data class Success(
        val transactions: List<Transaction>,
        val isFetchingNextPage: Boolean = false,
        val isEndOfList: Boolean = false,
        val errorMessage: String? = null
    ) : TransactionsUiState()
    object Empty : TransactionsUiState()
    data class Error(val message: String) : TransactionsUiState()
}

class TransactionsViewModel(
    val repository: TransactionRepository,
    val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionsUiState>(TransactionsUiState.LoadingInitial)
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private var typeFilter: String = "all"
    private var categoryFilter: String = "all"
    private var searchQuery: String = ""

    private var lastCreatedAt: String? = null
    private var lastId: String? = null
    
    var primaryAccountId: String? = null
        private set

    private var isEndOfList = false
    private var isFetchingNext = false

    private val PAGE_SIZE = 20L

    init {
        loadInitial()
        viewModelScope.launch {
            repository.refreshSignal.collect {
                loadInitial()
            }
        }
    }

    fun setFilters(type: String, category: String, query: String) {
        if (typeFilter == type && categoryFilter == category && searchQuery == query) return
        typeFilter = type
        categoryFilter = category
        searchQuery = query
        loadInitial()
    }

    fun loadInitial() {
        viewModelScope.launch {
            if (primaryAccountId == null) {
                primaryAccountId = accountRepository.getPrimaryAccount().getOrNull()?.id
            }
            
            _uiState.value = TransactionsUiState.LoadingInitial
            lastCreatedAt = null
            lastId = null
            isEndOfList = false

            val result = repository.getTransactionsPage(
                lastCreatedAt = null,
                lastId = null,
                limitCount = PAGE_SIZE,
                type = typeFilter,
                category = categoryFilter,
                searchQuery = searchQuery
            )

            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                if (list.isEmpty()) {
                    _uiState.value = TransactionsUiState.Empty
                } else {
                    if (list.size < PAGE_SIZE) isEndOfList = true
                    lastCreatedAt = list.last().createdAt
                    lastId = list.last().id
                    
                    _uiState.value = TransactionsUiState.Success(
                        transactions = list,
                        isEndOfList = isEndOfList
                    )
                }
            } else {
                _uiState.value = TransactionsUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load")
            }
        }
    }

    fun loadNextPage() {
        if (isEndOfList || isFetchingNext || _uiState.value !is TransactionsUiState.Success) return
        val currentState = _uiState.value as TransactionsUiState.Success

        viewModelScope.launch {
            isFetchingNext = true
            _uiState.value = currentState.copy(isFetchingNextPage = true)

            val result = repository.getTransactionsPage(
                lastCreatedAt = lastCreatedAt,
                lastId = lastId,
                limitCount = PAGE_SIZE,
                type = typeFilter,
                category = categoryFilter,
                searchQuery = searchQuery
            )

            if (result.isSuccess) {
                val newItems = result.getOrNull() ?: emptyList()
                if (newItems.isEmpty()) {
                    isEndOfList = true
                    _uiState.value = currentState.copy(isFetchingNextPage = false, isEndOfList = true)
                } else {
                    if (newItems.size < PAGE_SIZE) isEndOfList = true
                    lastCreatedAt = newItems.last().createdAt
                    lastId = newItems.last().id
                    
                    val combinedList = currentState.transactions + newItems
                    _uiState.value = currentState.copy(
                        transactions = combinedList.distinctBy { it.id }, // prevent dupes defensively
                        isFetchingNextPage = false,
                        isEndOfList = isEndOfList
                    )
                }
            } else {
                _uiState.value = currentState.copy(
                    isFetchingNextPage = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
            isFetchingNext = false
        }
    }

    fun clearError() {
        val state = _uiState.value
        if (state is TransactionsUiState.Success) {
            _uiState.value = state.copy(errorMessage = null)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(id)
            if (result.isSuccess) {
                val state = _uiState.value
                if (state is TransactionsUiState.Success) {
                    val newList = state.transactions.filter { it.id != id }
                    if (newList.isEmpty()) {
                        _uiState.value = TransactionsUiState.Empty
                    } else {
                        _uiState.value = state.copy(transactions = newList)
                    }
                }
            } else {
                val state = _uiState.value
                if (state is TransactionsUiState.Success) {
                    _uiState.value = state.copy(errorMessage = "Failed to delete transaction")
                }
            }
        }
    }

    fun refreshAfterMutation() {
        loadInitial()
    }
}

class TransactionsViewModelFactory(
    private val repository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionsViewModel(repository, accountRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
