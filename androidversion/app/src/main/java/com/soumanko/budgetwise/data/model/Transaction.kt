package com.soumanko.budgetwise.data.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import java.math.BigDecimal

@Stable
@Serializable
data class Transaction(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("account_id") val accountId: String? = null,
    val type: String, // "income" or "expense"
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val category: String,
    val subcategory: String? = null,
    val description: String? = null,
    val merchant: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("transaction_date") val transactionDate: String,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
