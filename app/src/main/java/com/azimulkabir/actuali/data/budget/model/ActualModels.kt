package com.azimulkabir.actuali.data.budget.model

enum class ActualAccountType {
    CHECKING,
    SAVINGS,
    CREDIT,
    INVESTMENT,
    MORTGAGE,
    DEBT,
    OTHER;

    companion object {
        fun fromDatabase(value: String?): ActualAccountType = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: CHECKING
    }
}

data class ActualAccount(
    val id: String,
    val name: String,
    val type: ActualAccountType,
    val offBudget: Boolean,
    val closed: Boolean,
    val sortOrder: Int,
    val balanceCents: Long,
)

data class ActualPayee(
    val id: String,
    val name: String,
    val transferAccountId: String?,
)

data class ActualCategory(
    val id: String,
    val name: String,
    val groupId: String,
    val isIncome: Boolean,
    val hidden: Boolean,
    val sortOrder: Double,
)

data class ActualCategoryGroup(
    val id: String,
    val name: String,
    val isIncome: Boolean,
    val hidden: Boolean,
    val sortOrder: Double,
    val categories: List<ActualCategory>,
)

data class ActualTransaction(
    val id: String,
    val accountId: String,
    val date: Int,
    val amountCents: Long,
    val payeeId: String?,
    val payeeName: String?,
    val categoryId: String?,
    val categoryName: String?,
    val notes: String?,
    val cleared: Boolean,
    val reconciled: Boolean,
    val transferId: String?,
    val isParent: Boolean,
    val parentId: String?,
    val tombstone: Boolean,
    val sortOrder: Double?,
    val importedPayee: String?,
    val scheduleId: String?,
    val transferAccountId: String?,
    val startingBalance: Boolean = false,
    val splitPortions: List<SplitPortion> = emptyList(),
) {
    data class SplitPortion(val categoryName: String?, val amountCents: Long)
}
