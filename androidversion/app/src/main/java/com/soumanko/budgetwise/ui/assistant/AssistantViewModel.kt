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
                val replyText = callAiBackend(text)
                
                val botMessage = ChatMessage((System.currentTimeMillis() + 1).toString(), replyText, false)
                _messages.value = _messages.value + botMessage
            } catch (e: Exception) {
                e.printStackTrace()
                val errorDesc = e.message ?: "Unknown error"
                val errorMessage = ChatMessage(
                    (System.currentTimeMillis() + 1).toString(), 
                    "AI Service Error: $errorDesc", 
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

    private suspend fun callAiBackend(userMessage: String): String {
        return try {
            withContext(Dispatchers.IO) {
                val session = supabaseClient.auth.currentSessionOrNull() 
                    ?: throw Exception("Authentication Failure: No active session")
                
                val url = URL(com.soumanko.budgetwise.AppConfig.AI_BACKEND_URL)
                android.util.Log.d("AssistantVM", "Sending AI request to: $url")
                
                val connection = try {
                    url.openConnection() as HttpURLConnection
                } catch (e: Exception) {
                    throw Exception("Network Failure: Cannot connect to server (${e.message})")
                }
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                val payload = JSONObject()
                payload.put("message", userMessage)

                try {
                    val out = OutputStreamWriter(connection.outputStream)
                    out.write(payload.toString())
                    out.close()
                } catch (e: Exception) {
                    throw Exception("Network Failure: Failed to send request (${e.message})")
                }

                val responseCode = try {
                    connection.responseCode
                } catch (e: Exception) {
                    throw Exception("Network Failure: Did not receive a response from the server (${e.message})")
                }
                
                android.util.Log.d("AssistantVM", "Received HTTP Status: $responseCode")

                if (responseCode in 200..299) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                    android.util.Log.d("AssistantVM", "Received successful response: $responseString")
                    try {
                        val jsonResponse = JSONObject(responseString)
                        if (jsonResponse.has("response")) {
                            jsonResponse.getString("response")
                        } else if (jsonResponse.has("reply")) {
                            jsonResponse.getString("reply")
                        } else if (jsonResponse.has("choices")) {
                            jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                        } else {
                            throw Exception("Malformed AI Response: Missing expected fields. Raw: $responseString")
                        }
                    } catch (e: Exception) {
                        if (e.message?.startsWith("Malformed AI Response") == true) throw e
                        throw Exception("Malformed AI Response: Failed to parse JSON (${e.message})")
                    }
                } else {
                    val errorStr = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
                    android.util.Log.e("AssistantVM", "API Error Body: $errorStr")
                    
                    when (responseCode) {
                        401, 403 -> throw Exception("Authentication Failure (HTTP $responseCode): $errorStr")
                        404 -> throw Exception("Endpoint/Deployment Failure (HTTP 404): The Vercel route was not found. $errorStr")
                        in 500..599 -> throw Exception("Backend Failure (HTTP $responseCode): $errorStr")
                        else -> throw Exception("HTTP $responseCode Error: $errorStr")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AssistantVM", "Exception in callAiBackend: ${e.javaClass.simpleName} - ${e.message}")
            throw e
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
