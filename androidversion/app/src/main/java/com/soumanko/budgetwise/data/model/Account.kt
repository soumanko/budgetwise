package com.soumanko.budgetwise.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Account(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("opening_balance") @Serializable(with = BigDecimalSerializer::class) val openingBalance: BigDecimal,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
