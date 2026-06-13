package com.vaultx.admin.data.model

data class Plan(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val basePrice: Double? = null,
    val durationDays: Int = 30,
    val features: List<String> = emptyList(),
    val isActive: Boolean = true,
    val isRecommended: Boolean = false,
    val orderIndex: Int = 0
)
