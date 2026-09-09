package com.soumanko.budgetwise.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Insight(
    val type: String,
    val title: String,
    val description: String,
    val severity: String,
    val data: Map<String, String>? = null
)
