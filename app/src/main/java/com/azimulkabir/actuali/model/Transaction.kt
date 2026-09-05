package com.azimulkabir.actuali.model

data class Transaction(
    val id: String,
    val date: String,
    val payee: String,
    val category: String,
    val account: String,
    val amount: Int,
    val cleared: Boolean,
    val amountCents: Long = amount.toLong() * 100,
    val type: Type = if (amountCents >= 0) Type.INCOME else Type.EXPENSE,
    val transferAccount: String? = null,
    val notes: String = "",
)

enum class Type { EXPENSE, INCOME, TRANSFER }
