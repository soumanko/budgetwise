package com.soumanko.budgetwise.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class RecurringExpense(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val category: String,
    val frequency: String,
    @SerialName("next_due_date") val nextDueDate: String,
    val active: Boolean,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RecurringExpenseInsert(
    @SerialName("user_id") val userId: String,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val category: String,
    val frequency: String,
    @SerialName("next_due_date") val nextDueDate: String,
    val active: Boolean,
    val notes: String? = null
)
