package com.soumanko.budgetwise.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soumanko.budgetwise.data.repository.BudgetRepository
import com.soumanko.budgetwise.data.repository.ProfileRepository
import com.soumanko.budgetwise.data.repository.RecurringExpenseRepository
import com.soumanko.budgetwise.data.repository.TransactionRepository
import com.soumanko.budgetwise.domain.finance.FinanceCalculations
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ChatMessage(val id: String, val text: String, val isUser: Boolean)

class AssistantViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val profileRepository: ProfileRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    init {
        _messages.value = listOf(
            ChatMessage("0", "Hello! I'm your BudgetWise AI Assistant. How can I help you manage your finances today?", false)
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isTyping.value) return

        val userMessage = ChatMessage(System.currentTimeMillis().toString(), text, true)
        _messages.value = _messages.value + userMessage
        _isTyping.value = true

        viewModelScope.launch {
            try {
                val contextData = buildContextData()
                val replyText = callAiBackend(text, contextData)
                
                val botMessage = ChatMessage((System.currentTimeMillis() + 1).toString(), replyText, false)
                _messages.value = _messages.value + botMessage
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMessage = ChatMessage(
                    (System.currentTimeMillis() + 1).toString(), 
                    "Sorry, I'm having trouble connecting to the AI service right now. Please ensure the backend is deployed and try again.", 
                    false
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isTyping.value = false
            }
        }
    }

    private suspend fun buildContextData(): String {
        return withContext(Dispatchers.IO) {
            try {
                val txResult = transactionRepository.getTransactions()
                val profileResult = profileRepository.getProfile()
                
                val transactions = txResult.getOrNull() ?: emptyList()
                val profile = profileResult.getOrNull()
                
                val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val currentMonthTx = transactions.filter { it.transactionDate.startsWith(currentMonth) }
                
                val stats = FinanceCalculations.calculateMonthlyStats(currentMonthTx)
                val balance = FinanceCalculations.calculateBalance(transactions)
                val categorySpending = FinanceCalculations.calculateCategorySpending(currentMonthTx)
                
                val contextObj = JSONObject()
                contextObj.put("total_balance", balance.toPlainString())
                contextObj.put("monthly_income", stats.totalIncome.toPlainString())
                contextObj.put("monthly_expenses", stats.totalExpenses.toPlainString())
                
                val topCategories = categorySpending.take(3).joinToString { "${it.category}: ₹${it.amount}" }
                contextObj.put("top_spending_categories", topCategories)
                
                if (profile != null) {
                    contextObj.put("user_name", profile.fullName)
                    contextObj.put("currency", profile.currency)
                }

                contextObj.toString()
            } catch (e: Exception) {
                "{}"
            }
        }
    }

    private suspend fun callAiBackend(userMessage: String, contextData: String): String {
        return withContext(Dispatchers.IO) {
            val session = supabaseClient.auth.currentSessionOrNull() 
                ?: throw Exception("No authenticated session")
            
            val url = URL("https://wpwfaztaxqvhuubdspou.supabase.co/functions/v1/chat")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            connection.doOutput = true

            val messagesArray = JSONArray()
            
            // Add system context
            val systemObj = JSONObject()
            systemObj.put("role", "system")
            systemObj.put("content", "You are the BudgetWise AI financial assistant. You MUST base all your answers on the following user financial data: $contextData. Use the ₹ symbol for currency. Do not invent balances or data.")
            messagesArray.put(systemObj)

            // Add chat history (last 5 messages)
            _messages.value.takeLast(5).forEach { msg ->
                val msgObj = JSONObject()
                msgObj.put("role", if (msg.isUser) "user" else "assistant")
                msgObj.put("content", msg.text)
                messagesArray.put(msgObj)
            }

            // The newest user message is already in _messages.value, so it was added in the loop above.
            
            val payload = JSONObject()
            payload.put("messages", messagesArray)

            val out = OutputStreamWriter(connection.outputStream)
            out.write(payload.toString())
            out.close()

            if (connection.responseCode in 200..299) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                // Parse the expected format from your edge function
                // Assuming it returns { "reply": "message" } or similar
                try {
                    val jsonResponse = JSONObject(responseString)
                    if (jsonResponse.has("reply")) {
                        jsonResponse.getString("reply")
                    } else if (jsonResponse.has("choices")) {
                        jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                    } else {
                        responseString
                    }
                } catch (e: Exception) {
                    responseString
                }
            } else {
                val errorStr = connection.errorStream.bufferedReader().use { it.readText() }
                throw Exception("API Error: ${connection.responseCode} $errorStr")
            }
        }
    }
}

class AssistantViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val profileRepository: ProfileRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val supabaseClient: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssistantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssistantViewModel(
                transactionRepository,
                budgetRepository,
                profileRepository,
                recurringExpenseRepository,
                supabaseClient
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
