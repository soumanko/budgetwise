package com.soumanko.budgetwise.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Budget(
    val id: String,
    @SerialName("user_id") val userId: String,
    val category: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val month: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class BudgetInsert(
    @SerialName("user_id") val userId: String,
    val category: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val month: String
)
