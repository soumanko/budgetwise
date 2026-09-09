package com.soumanko.budgetwise.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class SavingsGoal(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("target_amount")
    @Serializable(with = BigDecimalSerializer::class) val targetAmount: BigDecimal,
    @SerialName("current_amount")
    @Serializable(with = BigDecimalSerializer::class) val currentAmount: BigDecimal,
    val deadline: String? = null,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SavingsGoalInsert(
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("target_amount")
    @Serializable(with = BigDecimalSerializer::class) val targetAmount: BigDecimal,
    @SerialName("current_amount")
    @Serializable(with = BigDecimalSerializer::class) val currentAmount: BigDecimal,
    val deadline: String? = null,
    val description: String? = null
)
