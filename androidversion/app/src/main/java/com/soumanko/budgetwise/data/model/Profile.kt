package com.soumanko.budgetwise.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Profile(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val currency: String,
    @SerialName("monthly_budget") @Serializable(with = BigDecimalSerializer::class) val monthlyBudget: BigDecimal,
    @SerialName("low_balance_threshold") @Serializable(with = BigDecimalSerializer::class) val lowBalanceThreshold: BigDecimal,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
