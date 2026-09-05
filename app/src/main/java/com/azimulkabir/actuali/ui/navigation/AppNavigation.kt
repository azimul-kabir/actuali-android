package com.azimulkabir.actuali.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.azimulkabir.actuali.ui.accounts.AccountsScreen
import com.azimulkabir.actuali.ui.budget.BudgetScreen
import com.azimulkabir.actuali.ui.settings.SettingsScreen
import com.azimulkabir.actuali.ui.settings.ConnectionScreen
import com.azimulkabir.actuali.ui.settings.CreditCardsScreen
import com.azimulkabir.actuali.ui.transactions.AddTransactionScreen
import com.azimulkabir.actuali.ui.transactions.TransactionsScreen
import com.azimulkabir.actuali.ui.reports.ReportsScreen
import com.azimulkabir.actuali.model.Transaction
import com.azimulkabir.actuali.data.ActualiRepository
import com.azimulkabir.actuali.data.sync.ActualSyncRunner
import com.azimulkabir.actuali.data.sync.SyncRunResult
import com.azimulkabir.actuali.data.preferences.DisplayPreferences
import com.azimulkabir.actuali.ui.components.BalanceVisibility
import com.azimulkabir.actuali.ui.components.CurrencyDisplay

private enum class MainDestination(
    val label: String,
    val icon: ImageVector,
) {
    Budget("Budget", Icons.Outlined.PieChartOutline),
    Accounts("Accounts", Icons.Outlined.AccountBalanceWallet),
    Add("Add", Icons.Outlined.AddCircleOutline),
    Reports("Reports", Icons.Outlined.BarChart),
    More("More", Icons.Outlined.MoreHoriz),
}

private enum class DetailDestination { Main, Transactions, EditTransaction, Connection, CreditCards }

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    foregroundGeneration: Int = 0,
    onAppearanceChange: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val displayPreferences = remember { DisplayPreferences(context) }
    var repositoryVersion by remember { mutableStateOf(0) }
    val repository = remember(repositoryVersion) { ActualiRepository(context) }
    var dataVersion by remember { mutableStateOf(0) }
    var budgetMonth by rememberSaveable {
        mutableStateOf(java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date()))
    }
    val budgetGroups = remember(dataVersion, budgetMonth) { repository.budgetGroups(budgetMonth) }
    val budgetOverview = remember(dataVersion, budgetMonth) { repository.budgetOverview(budgetMonth) }
    val accounts = remember(dataVersion) { repository.accounts() }
    val transactions = remember(dataVersion) { repository.transactions() }
    val categoryNames = remember(dataVersion) { repository.categoryNames() }
    val payeeNames = remember(dataVersion) { repository.payeeNames() }
    val reportSnapshot = remember(dataVersion) { repository.reports() }
    val creditCards = remember(dataVersion) { repository.creditCards() }
    var destination by rememberSaveable {
        mutableStateOf(MainDestination.entries.firstOrNull { it.label == displayPreferences.startPage }
            ?: MainDestination.Accounts)
    }
    var detail by rememberSaveable { mutableStateOf(DetailDestination.Main) }
    var transactionAccount by rememberSaveable { mutableStateOf<String?>(null) }
    var transactionCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var transactionMonth by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var hideDecimalPlaces by remember { mutableStateOf(displayPreferences.hideDecimalPlaces) }
    var currencyCode by remember { mutableStateOf(displayPreferences.currencyCode) }
    var currencySymbolOnly by remember { mutableStateOf(displayPreferences.currencySymbolOnly) }
    var showHiddenCategories by remember { mutableStateOf(displayPreferences.showHiddenCategories) }
    var showSpentColumn by remember { mutableStateOf(displayPreferences.showSpentColumn) }
    var showBudgetProgressBars by remember { mutableStateOf(displayPreferences.showBudgetProgressBars) }
    var showBudgetOverview by remember { mutableStateOf(displayPreferences.showBudgetOverview) }
    var showGroupTotals by remember { mutableStateOf(displayPreferences.showGroupTotals) }
    var hideFullySpentCategories by remember { mutableStateOf(displayPreferences.hideFullySpentCategories) }
    var hideBalances by remember { mutableStateOf(displayPreferences.hideBalances) }
    var appearance by remember { mutableStateOf(displayPreferences.appearance) }
    var startPage by remember { mutableStateOf(displayPreferences.startPage) }
    var defaultAccount by remember { mutableStateOf(displayPreferences.defaultAccount) }
    var groupTransactionsByDate by remember { mutableStateOf(displayPreferences.groupTransactionsByDate) }
    var showAccountsMonthlySummary by remember { mutableStateOf(displayPreferences.showAccountsMonthlySummary) }
    var conventionalAmountEntry by remember { mutableStateOf(displayPreferences.conventionalAmountEntry) }
    BalanceVisibility.hidden = hideBalances
    CurrencyDisplay.code = currencyCode
    CurrencyDisplay.symbolOnly = currencySymbolOnly
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun mutate(label: String, action: () -> Boolean): Boolean = runCatching(action).fold(
        onSuccess = { changed ->
            if (changed) dataVersion += 1 else errorMessage = "$label could not be completed."
            changed
        },
        onFailure = { error ->
            errorMessage = error.message?.takeIf(String::isNotBlank) ?: "$label failed."
            false
        },
    )

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    LaunchedEffect(foregroundGeneration) {
        val result = runCatching { withContext(Dispatchers.IO) { ActualSyncRunner.run(context) } }
            .onFailure { errorMessage = it.message ?: "Automatic sync failed." }
            .getOrNull()
        if (result is SyncRunResult.Success) dataVersion += 1
    }

    BackHandler(enabled = detail != DetailDestination.Main || destination != MainDestination.Budget) {
        when {
            detail != DetailDestination.Main -> {
                detail = DetailDestination.Main
                editingTransaction = null
                if (destination == MainDestination.Add) destination = MainDestination.Accounts
            }
            else -> destination = MainDestination.Budget
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item && detail == DetailDestination.Main,
                        onClick = {
                            if (item == MainDestination.Add && !repository.isUsingActualBudget) {
                                destination = MainDestination.More
                                detail = DetailDestination.Connection
                                return@NavigationBarItem
                            }
                            destination = item
                            detail = if (item == MainDestination.Add) DetailDestination.EditTransaction
                            else DetailDestination.Main
                            if (item == MainDestination.Add) editingTransaction = null
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (detail) {
            DetailDestination.Transactions -> TransactionsScreen(
                accountName = transactionAccount,
                categoryName = transactionCategory,
                month = transactionMonth,
                onBack = { detail = DetailDestination.Main },
                onEdit = {
                    editingTransaction = it
                    detail = DetailDestination.EditTransaction
                },
                modifier = contentModifier,
                transactions = transactions,
                hideDecimalPlaces = hideDecimalPlaces,
                groupTransactionsByDate = groupTransactionsByDate,
                onGroupTransactionsByDateChange = {
                    displayPreferences.groupTransactionsByDate = it
                    groupTransactionsByDate = it
                },
                onSetCleared = { transaction, cleared ->
                    mutate("Updating transaction") { repository.setTransactionCleared(transaction.id, cleared) }
                },
                onDelete = { transaction ->
                    mutate("Deleting transaction") { repository.deleteTransaction(transaction.id) }
                },
            )
            DetailDestination.EditTransaction -> AddTransactionScreen(
                editing = editingTransaction,
                onBack = {
                    detail = DetailDestination.Main
                    if (destination == MainDestination.Add) destination = MainDestination.Accounts
                },
                onSave = {
                    if (runCatching { repository.saveTransaction(it) }.fold(
                            onSuccess = { true },
                            onFailure = { error ->
                                errorMessage = error.message?.takeIf(String::isNotBlank) ?: "Saving transaction failed."
                                false
                            },
                        )) {
                        dataVersion += 1
                        detail = DetailDestination.Main
                        destination = MainDestination.Accounts
                    }
                },
                onDelete = { transaction ->
                    if (mutate("Deleting transaction") { repository.deleteTransaction(transaction.id) }) {
                        editingTransaction = null
                        detail = DetailDestination.Transactions
                    }
                },
                modifier = contentModifier,
                    accountOptions = accounts.filter { !it.closed }.map { it.name },
                    categoryOptions = categoryNames,
                    payeeOptions = payeeNames,
                    defaultAccount = defaultAccount,
                    hideDecimalPlaces = hideDecimalPlaces,
                    conventionalAmountEntry = conventionalAmountEntry,
            )
            DetailDestination.Connection -> ConnectionScreen(
                onBack = { detail = DetailDestination.Main },
                onBeforeBudgetReplacement = { repository.close() },
                onBudgetInstalled = {
                    repositoryVersion += 1
                    dataVersion += 1
                },
                modifier = contentModifier,
            )
            DetailDestination.CreditCards -> CreditCardsScreen(
                cards = creditCards,
                accounts = accounts,
                hideDecimalPlaces = hideDecimalPlaces,
                onBack = { detail = DetailDestination.Main },
                onSave = { accountId, day, offset, limit ->
                    mutate("Saving credit card") { repository.setCreditCard(accountId, day, offset, limit) }
                },
                onRemove = { accountId ->
                    mutate("Removing credit card") { repository.setCreditCard(accountId, null) }
                },
                modifier = contentModifier,
            )
            DetailDestination.Main -> if (!repository.isUsingActualBudget && destination != MainDestination.More) {
                NoBudgetScreen(contentModifier) {
                    destination = MainDestination.More
                    detail = DetailDestination.Connection
                }
            } else when (destination) {
                MainDestination.Budget -> BudgetScreen(
                    contentModifier,
                    groups = budgetGroups,
                    overview = budgetOverview,
                    month = budgetMonth,
                    onMonthChange = { budgetMonth = it },
                    hideDecimalPlaces = hideDecimalPlaces,
                    showHidden = showHiddenCategories,
                    onShowHiddenChange = {
                        displayPreferences.showHiddenCategories = it
                        showHiddenCategories = it
                    },
                    showSpent = showSpentColumn,
                    onShowSpentChange = {
                        displayPreferences.showSpentColumn = it
                        showSpentColumn = it
                    },
                    showProgressBars = showBudgetProgressBars,
                    onShowProgressBarsChange = {
                        displayPreferences.showBudgetProgressBars = it
                        showBudgetProgressBars = it
                    },
                    showOverview = showBudgetOverview,
                    onShowOverviewChange = {
                        displayPreferences.showBudgetOverview = it
                        showBudgetOverview = it
                    },
                    showGroupTotals = showGroupTotals,
                    onShowGroupTotalsChange = {
                        displayPreferences.showGroupTotals = it
                        showGroupTotals = it
                    },
                    hideFullySpent = hideFullySpentCategories,
                    onHideFullySpentChange = {
                        displayPreferences.hideFullySpentCategories = it
                        hideFullySpentCategories = it
                    },
                    onSetCategoryHidden = { group, category, hidden ->
                        mutate(if (hidden) "Hiding category" else "Showing category") {
                            repository.setCategoryHidden(group, category, hidden)
                        }
                    },
                    onSetGroupHidden = { group, hidden ->
                        mutate(if (hidden) "Hiding group" else "Showing group") {
                            repository.setCategoryGroupHidden(group, hidden)
                        }
                    },
                    onRenameCategory = { group, category, name ->
                        mutate("Renaming category") { repository.renameCategory(group, category, name) }
                    },
                    onRenameGroup = { group, name ->
                        mutate("Renaming group") { repository.renameCategoryGroup(group, name) }
                    },
                    onShowCategoryTransactions = { category, thisMonth ->
                        transactionAccount = null
                        transactionCategory = category
                        transactionMonth = if (thisMonth) budgetMonth else null
                        detail = DetailDestination.Transactions
                    },
                    onTransferBudget = { fromGroup, fromCategory, toGroup, toCategory, amount ->
                        mutate("Moving budget") {
                            repository.transferBudget(fromGroup, fromCategory, toGroup, toCategory, amount, budgetMonth)
                        }
                    },
                    onCreateCategory = { group, name ->
                        mutate("Creating category") { repository.createCategory(group, name) }
                    },
                    onCreateGroup = { name ->
                        mutate("Creating group") { repository.createCategoryGroup(name) }
                    },
                    onSetBudgetAmount = { group, category, amount ->
                        mutate("Updating budget") { repository.setBudgetAmount(group, category, amount, budgetMonth) }
                    },
                )
                MainDestination.Accounts -> AccountsScreen(
                    modifier = contentModifier,
                    accounts = accounts,
                    transactions = transactions,
                    hideDecimalPlaces = hideDecimalPlaces,
                    showMonthlySummary = showAccountsMonthlySummary,
                    creditCards = creditCards,
                    onAccountClick = {
                        transactionAccount = it
                        transactionCategory = null; transactionMonth = null
                        detail = DetailDestination.Transactions
                    },
                    onAllAccountsClick = {
                        transactionAccount = null
                        transactionCategory = null; transactionMonth = null
                        detail = DetailDestination.Transactions
                    },
                    onCloseAccount = { account ->
                        mutate(if (account.closed) "Reopening account" else "Closing account") {
                            repository.setAccountClosed(account.name, !account.closed)
                        }
                    },
                    onRenameAccount = { account, name ->
                        mutate("Renaming account") { repository.renameAccount(account.name, name) }
                    },
                    onCreateAccount = { name, offBudget, balance ->
                        mutate("Creating account") { repository.createAccount(name, offBudget, balance) }
                    },
                )
                MainDestination.Add -> Unit
                MainDestination.Reports -> ReportsScreen(reportSnapshot, hideDecimalPlaces, contentModifier)
                MainDestination.More -> SettingsScreen(
                    modifier = contentModifier,
                    onConnectionClick = { detail = DetailDestination.Connection },
                    hideDecimalPlaces = hideDecimalPlaces,
                    onHideDecimalPlacesChange = {
                        displayPreferences.hideDecimalPlaces = it
                        hideDecimalPlaces = it
                    },
                    currencyCode = currencyCode,
                    onCurrencyCodeChange = {
                        displayPreferences.currencyCode = it
                        currencyCode = it
                    },
                    currencySymbolOnly = currencySymbolOnly,
                    onCurrencySymbolOnlyChange = {
                        displayPreferences.currencySymbolOnly = it
                        currencySymbolOnly = it
                    },
                    hideBalances = hideBalances,
                    onHideBalancesChange = {
                        displayPreferences.hideBalances = it
                        hideBalances = it
                    },
                    appearance = appearance,
                    onAppearanceChange = {
                        displayPreferences.appearance = it
                        appearance = it
                        onAppearanceChange(it)
                    },
                    startPage = startPage,
                    onStartPageChange = {
                        displayPreferences.startPage = it
                        startPage = it
                    },
                    accountOptions = accounts.filterNot { it.closed }.map { it.name },
                    defaultAccount = defaultAccount,
                    onDefaultAccountChange = {
                        displayPreferences.defaultAccount = it
                        defaultAccount = it
                    },
                    groupTransactionsByDate = groupTransactionsByDate,
                    onGroupTransactionsByDateChange = {
                        displayPreferences.groupTransactionsByDate = it
                        groupTransactionsByDate = it
                    },
                    showAccountsMonthlySummary = showAccountsMonthlySummary,
                    onShowAccountsMonthlySummaryChange = {
                        displayPreferences.showAccountsMonthlySummary = it
                        showAccountsMonthlySummary = it
                    },
                    onCreditCardsClick = { detail = DetailDestination.CreditCards },
                    conventionalAmountEntry = conventionalAmountEntry,
                    onConventionalAmountEntryChange = {
                        displayPreferences.conventionalAmountEntry = it
                        conventionalAmountEntry = it
                    },
                )
            }
        }
    }
}

@Composable
private fun NoBudgetScreen(modifier: Modifier, onConnect: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No budget open", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        Text(
            "Connect to your Actual server and download a budget to begin.",
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        Button(onClick = onConnect) { Text("Connect to Actual") }
    }
}
