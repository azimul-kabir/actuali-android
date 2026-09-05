package com.azimulkabir.actuali.data

import android.content.Context
import com.azimulkabir.actuali.data.budget.ActualBudgetDatabase
import com.azimulkabir.actuali.data.budget.ActiveBudgetStore
import com.azimulkabir.actuali.data.budget.ActualTransactionForm
import com.azimulkabir.actuali.data.budget.ActualTransactionFormService
import com.azimulkabir.actuali.data.budget.ActualTransactionType
import com.azimulkabir.actuali.data.budget.ActualTransactionWriter
import com.azimulkabir.actuali.data.budget.ActualEntityWriter
import com.azimulkabir.actuali.data.budget.ActualBudgetWriter
import com.azimulkabir.actuali.data.budget.BudgetFileManager
import com.azimulkabir.actuali.model.Account
import com.azimulkabir.actuali.model.BudgetCategory
import com.azimulkabir.actuali.model.BudgetGroup
import com.azimulkabir.actuali.model.BudgetOverview
import com.azimulkabir.actuali.model.Transaction
import com.azimulkabir.actuali.model.Type
import com.azimulkabir.actuali.model.ReportCategory
import com.azimulkabir.actuali.model.ReportMonth
import com.azimulkabir.actuali.model.ReportSnapshot
import com.azimulkabir.actuali.data.sync.ActualSyncScheduler

class ActualiRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scheduleSync = { ActualSyncScheduler.scheduleMutation(appContext) }
    private val actualDatabase: ActualBudgetDatabase? = BudgetFileManager(context).let { files ->
        val budgets = files.listLocalBudgets()
        val selectedId = ActiveBudgetStore(context).budgetId
        val selected = budgets.firstOrNull { it.id == selectedId } ?: budgets.firstOrNull()
        selected?.let { metadata ->
            runCatching { ActualBudgetDatabase.open(files.databaseFile(metadata.id)) }.getOrNull()
        }
    }
    private val actualWriter = actualDatabase?.let { ActualTransactionWriter(it, onWrite = scheduleSync) }
    private val actualEntities = actualDatabase?.let { ActualEntityWriter(it, onWrite = scheduleSync) }
    private val actualBudgets = actualDatabase?.let { ActualBudgetWriter(it, onWrite = scheduleSync) }
    private val actualForms = actualDatabase?.let { db ->
        ActualTransactionFormService(db, requireNotNull(actualWriter))
    }

    val isUsingActualBudget: Boolean get() = actualDatabase != null

    fun close() {
        actualDatabase?.close()
    }

    fun categoryNames(): List<String> = actualDatabase?.fetchCategoryGroups()
        ?.flatMap { it.categories }
        ?.filterNot { it.hidden }
        ?.map { it.name }
        ?: emptyList()

    fun payeeNames(): List<String> = actualDatabase?.fetchPayees()
        ?.filter { it.transferAccountId == null && it.name != "Unknown" }
        ?.map { it.name }
        ?: emptyList()

    fun budgetGroups(month: String = currentMonth()): List<BudgetGroup> {
        actualDatabase?.let { db ->
            val budget = db.fetchBudgetMonth(month)
            return (budget.categories + budget.hiddenCategories).groupBy { it.groupId }.values
                .sortedBy { it.first().groupSortOrder }
                .map { rows ->
                    BudgetGroup(rows.first().groupName, rows.sortedBy { it.categorySortOrder }.map {
                        BudgetCategory(
                            it.categoryName,
                            centsToDisplayUnits(it.budgetedCents),
                            centsToDisplayUnits(-it.spentCents),
                            centsToDisplayUnits(it.availableCents),
                            it.budgetedCents,
                            it.categoryId,
                            it.availableCents,
                            it.hidden,
                            -it.spentCents,
                        )
                    }, hidden = rows.first().groupHidden)
                }
        }
        return emptyList()
    }

    fun budgetOverview(month: String = currentMonth()): BudgetOverview {
        actualDatabase?.let { db ->
            val budget = db.fetchBudgetMonth(month)
            return BudgetOverview(
                toBudgetCents = budget.toBudgetCents,
                budgetedCents = budget.categories.sumOf { it.budgetedCents },
                spentCents = budget.categories.sumOf { it.spentCents },
                availableCents = budget.categories.sumOf { it.availableCents },
            )
        }
        return BudgetOverview(null, 0, 0, 0)
    }

    fun accounts(): List<Account> {
        actualDatabase?.let { db ->
            return db.fetchAccounts().map {
                Account(
                    name = it.name,
                    balance = centsToDisplayUnits(it.balanceCents),
                    type = it.type.name.lowercase().replaceFirstChar(Char::uppercase),
                    offBudget = it.offBudget,
                    closed = it.closed,
                    balanceCents = it.balanceCents,
                )
            }
        }
        return emptyList()
    }

    fun transactions(): List<Transaction> {
        actualDatabase?.let { db ->
            val accountNames = db.fetchAccounts().associate { it.id to it.name }
            return db.fetchTransactions().map {
                Transaction(
                    id = it.id,
                    date = it.date.toString(),
                    payee = it.payeeName ?: if (it.isParent) "Split" else "",
                    category = it.categoryName ?: if (it.isParent) "Split" else "Uncategorized",
                    account = accountNames[it.accountId] ?: "Unknown",
                    amount = centsToDisplayUnits(it.amountCents),
                    cleared = it.cleared,
                    amountCents = it.amountCents,
                    type = when {
                        it.transferId != null -> Type.TRANSFER
                        it.amountCents >= 0 -> Type.INCOME
                        else -> Type.EXPENSE
                    },
                    transferAccount = it.transferAccountId?.let(accountNames::get),
                    notes = it.notes.orEmpty(),
                )
            }
        }
        return emptyList()
    }

    fun reports(): ReportSnapshot {
        val db = actualDatabase ?: return ReportSnapshot(emptyList(), emptyList(), 0)
        val accounts = db.fetchAccounts()
        val onBudgetIds = accounts.filter { !it.offBudget && !it.closed }.mapTo(mutableSetOf()) { it.id }
        val rows = db.fetchTransactionsForReports().filter { it.accountId in onBudgetIds }
        val currentMonth = currentMonth()
        val monthRows = rows.filter { it.transferId == null }.groupBy { dateMonth(it.date) }
        val end = java.time.YearMonth.parse(currentMonth)
        val months = (5 downTo 0).map { offset ->
            val month = end.minusMonths(offset.toLong()).toString()
            val transactions = monthRows[month].orEmpty()
            ReportMonth(
                month,
                transactions.filter { it.amountCents >= 0 }.sumOf { it.amountCents },
                transactions.filter { it.amountCents < 0 }.sumOf { -it.amountCents },
            )
        }
        val categories = rows.asSequence()
            .filter { it.transferId == null && it.amountCents < 0 && dateMonth(it.date) == currentMonth }
            .groupBy { it.categoryName ?: "Uncategorized" }
            .map { (name, transactions) -> ReportCategory(name, transactions.sumOf { -it.amountCents }) }
            .sortedByDescending { it.spentCents }
        return ReportSnapshot(months, categories, accounts.filterNot { it.closed }.sumOf { it.balanceCents })
    }

    fun saveTransaction(transaction: Transaction) {
        actualDatabase?.let { db ->
            val account = db.fetchAccounts().firstOrNull { it.name == transaction.account && !it.closed }
                ?: error("Select an account")
            val category = db.fetchCategoryGroups().flatMap { it.categories }
                .firstOrNull { it.name == transaction.category && !it.hidden }
            if (transaction.category.isNotBlank() && transaction.category != "Uncategorized" && category == null) {
                error("Select a category from the list")
            }
            val transferAccount = transaction.transferAccount?.let { name ->
                db.fetchAccounts().firstOrNull { it.name == name && !it.closed }
                    ?: error("Select a destination account")
            }
            val original = transaction.id.takeIf(String::isNotBlank)?.let(db::fetchTransaction)
            actualForms!!.save(
                ActualTransactionForm(
                    accountId = account.id,
                    type = when (transaction.type) {
                        Type.EXPENSE -> ActualTransactionType.EXPENSE
                        Type.INCOME -> ActualTransactionType.INCOME
                        Type.TRANSFER -> ActualTransactionType.TRANSFER
                    },
                    amount = com.azimulkabir.actuali.ui.components.centsToInput(kotlin.math.abs(transaction.amountCents)),
                    payeeName = transaction.payee,
                    transferToAccountId = transferAccount?.id,
                    categoryId = category?.id,
                    notes = transaction.notes,
                    date = parseDate(transaction.date),
                    cleared = transaction.cleared,
                ),
                original = original,
            )
            return
        }
        error("Connect to Actual and download a budget before adding transactions")
    }

    fun setAccountClosed(name: String, closed: Boolean): Boolean {
        val db = actualDatabase ?: return false
        val account = db.fetchAccounts().firstOrNull { it.name == name } ?: return false
        actualEntities!!.setAccountClosed(account.id, closed)
        return true
    }

    fun renameAccount(oldName: String, newName: String): Boolean {
        val account = actualDatabase?.fetchAccounts()?.firstOrNull { it.name == oldName } ?: return false
        actualEntities!!.renameAccount(account.id, newName); return true
    }

    fun renameCategory(groupName: String, oldName: String, newName: String): Boolean {
        val category = actualDatabase?.fetchCategoryGroups()?.firstOrNull { it.name == groupName }
            ?.categories?.firstOrNull { it.name == oldName } ?: return false
        actualEntities!!.renameCategory(category.id, newName); return true
    }

    fun renameCategoryGroup(oldName: String, newName: String): Boolean {
        val group = actualDatabase?.fetchCategoryGroups()?.firstOrNull { it.name == oldName } ?: return false
        actualEntities!!.renameCategoryGroup(group.id, newName); return true
    }

    fun createAccount(name: String, offBudget: Boolean, startingBalance: String): Boolean {
        val cents = ActualTransactionFormService.cents(startingBalance.ifBlank { "0" }) ?: return false
        actualEntities?.createAccount(name, offBudget, cents) ?: return false
        return true
    }

    fun createCategory(groupName: String, name: String): Boolean {
        val group = actualDatabase?.fetchCategoryGroups()?.firstOrNull { it.name == groupName } ?: return false
        actualEntities!!.createCategory(name, group.id); return true
    }

    fun createCategoryGroup(name: String): Boolean {
        actualEntities?.createCategoryGroup(name) ?: return false
        return true
    }

    fun setCategoryHidden(groupName: String, categoryName: String, hidden: Boolean): Boolean {
        val db = actualDatabase ?: return false
        val group = db.fetchCategoryGroups().firstOrNull { it.name == groupName } ?: return false
        val category = group.categories.firstOrNull { it.name == categoryName } ?: return false
        actualEntities!!.setCategoryHidden(category.id, hidden)
        return true
    }

    fun setCategoryGroupHidden(groupName: String, hidden: Boolean): Boolean {
        val group = actualDatabase?.fetchCategoryGroups()?.firstOrNull { it.name == groupName } ?: return false
        actualEntities!!.setCategoryGroupHidden(group.id, hidden)
        return true
    }

    fun setBudgetAmount(groupName: String, categoryName: String, amountCents: Long, month: String = currentMonth()): Boolean {
        val db = actualDatabase ?: return false
        val category = db.fetchCategoryGroups().firstOrNull { it.name == groupName }
            ?.categories?.firstOrNull { it.name == categoryName } ?: return false
        actualBudgets!!.setAmount(month, category.id, amountCents)
        return true
    }

    fun transferBudget(fromGroup: String?, fromCategory: String?, toGroup: String?, toCategory: String?,
        amountCents: Long, month: String = currentMonth()): Boolean {
        val db = actualDatabase ?: return false
        val groups = db.fetchCategoryGroups()
        val from = if (fromGroup == null || fromCategory == null) null else
            groups.firstOrNull { it.name == fromGroup }?.categories?.firstOrNull { it.name == fromCategory } ?: return false
        val to = if (toGroup == null || toCategory == null) null else
            groups.firstOrNull { it.name == toGroup }?.categories?.firstOrNull { it.name == toCategory } ?: return false
        actualBudgets!!.transfer(month, from?.id, to?.id, amountCents)
        return true
    }

    fun setTransactionCleared(id: String, cleared: Boolean): Boolean {
        val transaction = actualDatabase?.fetchTransaction(id) ?: return false
        actualWriter!!.updateTransaction(transaction.copy(cleared = cleared), setOf("cleared"))
        return true
    }

    fun deleteTransaction(id: String): Boolean {
        val transaction = actualDatabase?.fetchTransaction(id) ?: return false
        actualWriter!!.deleteTransaction(transaction)
        return true
    }

    private fun parseDate(value: String): Int {
        value.filter(Char::isDigit).toIntOrNull()?.takeIf { it in 19000101..29991231 }?.let { return it }
        error("Enter a valid date as YYYY-MM-DD")
    }

    private fun centsToDisplayUnits(cents: Long): Int =
        (cents / 100L).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

    private fun currentMonth(): String = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        .format(java.util.Date())

    private fun dateMonth(date: Int): String = "%04d-%02d".format(date / 10_000, date / 100 % 100)
}
