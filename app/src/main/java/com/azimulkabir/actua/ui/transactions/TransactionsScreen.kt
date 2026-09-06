package com.azimulkabir.actua.ui.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.azimulkabir.actua.model.Transaction
import com.azimulkabir.actua.model.Account
import com.azimulkabir.actua.model.CreditCardStatus
import com.azimulkabir.actua.ui.components.formatMoneyCents
import com.azimulkabir.actua.ui.components.formatStoredDate
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

private val sampleTransactions = listOf(
    Transaction("1", "Today", "Agora Super Shop", "Groceries", "Everyday account", -2_450, true),
    Transaction("2", "Today", "Salary", "Income", "Everyday account", 72_000, true),
    Transaction("3", "Today", "Pathao", "Transport", "Credit card", -380, false),
    Transaction("4", "Yesterday", "DESCO", "Electricity", "Everyday account", -2_700, true),
    Transaction("5", "Yesterday", "Coffee World", "Dining", "Credit card", -620, false),
    Transaction("6", "1 Sep 2026", "Landlord", "Rent", "Everyday account", -35_000, true),
    Transaction("7", "1 Sep 2026", "ISP", "Internet", "Everyday account", -1_500, true),
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    accountName: String?,
    categoryName: String? = null,
    month: String? = null,
    onBack: () -> Unit,
    onEdit: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    transactions: List<Transaction> = sampleTransactions,
    hideDecimalPlaces: Boolean = false,
    groupTransactionsByDate: Boolean = true,
    onGroupTransactionsByDateChange: (Boolean) -> Unit = {},
    onSetCleared: (Transaction, Boolean) -> Unit = { _, _ -> },
    onDelete: (Transaction) -> Unit = {},
    account: Account? = null,
    creditCard: CreditCardStatus? = null,
    onSaveAccountNote: (String) -> Unit = {},
) {
    var search by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var hideCleared by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Transaction?>(null) }
    var accountNote by remember(account) { mutableStateOf(account?.note.orEmpty()) }

    val visible = transactions.filter {
        (accountName == null || it.account == accountName) &&
            (categoryName == null || it.category == categoryName) &&
            (month == null || it.date.filter(Char::isDigit).startsWith(month.replace("-", ""))) &&
            (!hideCleared || !it.cleared) &&
            (search.isBlank() || listOf(it.payee, it.category, it.account).any { text ->
                text.contains(search, ignoreCase = true)
            })
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(categoryName ?: accountName ?: "All accounts", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { showSearch = !showSearch }) {
                Icon(Icons.Outlined.Search, contentDescription = "Search transactions")
            }
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Transaction options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    ToggleItem("Group by date", groupTransactionsByDate, onGroupTransactionsByDateChange)
                    ToggleItem("Hide cleared transactions", hideCleared) { hideCleared = it }
                }
            }
        }
        AnimatedVisibility(
            visible = showSearch,
            enter = fadeIn(tween(180)) + expandVertically(tween(240)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(200)),
        ) {
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                placeholder = { Text("Search transactions") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            account?.let { selectedAccount ->
                item("account-details") {
                    AccountDetails(selectedAccount, creditCard, accountNote, { accountNote = it },
                        { onSaveAccountNote(accountNote) }, hideDecimalPlaces)
                }
            }
            item("transaction-total") {
                val total = visible.sumOf { it.amountCents }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text("${visible.size} transactions", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Amount(total, FontWeight.Bold, hideDecimalPlaces)
                }
            }
            if (groupTransactionsByDate) {
                visible.groupBy { it.date }.forEach { (date, transactions) ->
                    stickyHeader(key = date) {
                        Text(formatTransactionDate(date), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 20.dp, vertical = 8.dp))
                    }
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionRow(transaction, hideDecimalPlaces, showDate = false, onClick = { onEdit(transaction) },
                            onLongClick = { selected = transaction })
                    }
                }
            } else {
                items(visible, key = { it.id }) { transaction ->
                    TransactionRow(transaction, hideDecimalPlaces, showDate = true, onClick = { onEdit(transaction) },
                        onLongClick = { selected = transaction })
                }
            }
        }
    }
    selected?.let { transaction ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(transaction.payee, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                Action("Edit transaction") { selected = null; onEdit(transaction) }
                Action(if (transaction.cleared) "Mark uncleared" else "Mark cleared") {
                    selected = null
                    onSetCleared(transaction, !transaction.cleared)
                }
                Action("Delete transaction", destructive = true) {
                    selected = null
                    onDelete(transaction)
                }
            }
        }
    }
}

@Composable
private fun AccountDetails(account: Account, card: CreditCardStatus?, note: String,
    onNoteChange: (String) -> Unit, onSaveNote: () -> Unit, hideDecimals: Boolean) {
    var balanceExpanded by remember(account.id) { mutableStateOf(true) }
    val balanceArrowRotation by animateFloatAsState(
        targetValue = if (balanceExpanded) 180f else 0f,
        animationSpec = tween(250),
        label = "Balance disclosure",
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = { balanceExpanded = !balanceExpanded },
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Working balance",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        formatMoneyCents(account.balanceCents, hideDecimals),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = if (balanceExpanded) "Collapse balance details" else "Expand balance details",
                        modifier = Modifier.padding(start = 8.dp).size(20.dp).rotate(balanceArrowRotation),
                    )
                }
                AnimatedVisibility(
                    visible = balanceExpanded,
                    enter = fadeIn(tween(180)) + expandVertically(tween(260)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(220)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        card?.availableCreditCents?.let { DetailAmount("Available credit", it, hideDecimals) }
                        HorizontalDivider()
                        DetailAmount("Cleared", account.clearedCents, hideDecimals)
                        DetailAmount("Uncleared", account.unclearedCents, hideDecimals)
                        DetailAmount("Reconciled", account.reconciledCents, hideDecimals)
                        card?.config?.limitCents?.let { DetailAmount("Credit limit", it, hideDecimals) }
                    }
                }
            }
        }
        card?.let {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Billing cycle", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(it.cycle.dueSummary(), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                    DetailAmount("Cycle spend", it.cycleSpendCents, hideDecimals)
                }
            }
        }
        Text("Account note", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(note, onNoteChange, modifier = Modifier.fillMaxWidth(), minLines = 2,
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = { Text("Add note", style = MaterialTheme.typography.bodyMedium) })
        Button(onClick = onSaveNote, modifier = Modifier.fillMaxWidth()) { Text("Save note") }
    }
}

@Composable
private fun DetailAmount(label: String, amount: Long, hideDecimals: Boolean, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal)
        Text(formatMoneyCents(amount, hideDecimals), style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ToggleItem(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    DropdownMenuItem(text = { Text(label) }, trailingIcon = {
        Checkbox(checked = checked, onCheckedChange = null)
    }, onClick = { onChange(!checked) })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionRow(transaction: Transaction, hideDecimalPlaces: Boolean,
    showDate: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(horizontal = 20.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.width(10.dp), shape = CircleShape,
            color = if (transaction.cleared) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant) { Spacer(Modifier.width(10.dp)) }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(transaction.payee, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text("${transaction.category} • ${transaction.account}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Amount(transaction.amountCents, FontWeight.SemiBold, hideDecimalPlaces)
            if (showDate) Text(formatTransactionDate(transaction.date), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 42.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
}

internal fun formatTransactionDate(value: String): String {
    return formatStoredDate(value)
}

@Composable
private fun Amount(value: Long, weight: FontWeight, hideDecimalPlaces: Boolean) {
    Text(formatMoneyCents(value, hideDecimalPlaces, showPositiveSign = true), style = MaterialTheme.typography.bodyMedium, fontWeight = weight, textAlign = TextAlign.End,
        color = if (value >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun Action(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label, color = if (destructive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface) }, onClick = onClick, modifier = Modifier.fillMaxWidth())
}
