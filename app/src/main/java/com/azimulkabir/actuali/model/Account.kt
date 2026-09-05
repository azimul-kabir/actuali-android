package com.azimulkabir.actuali.model

data class Account(
    val name: String,
    val balance: Int,
    val type: String,
    val offBudget: Boolean = false,
    val closed: Boolean = false,
    val balanceCents: Long = balance.toLong() * 100,
    val id: String = name,
)
