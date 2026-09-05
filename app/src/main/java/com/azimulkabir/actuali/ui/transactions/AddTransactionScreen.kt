package com.azimulkabir.actuali.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.azimulkabir.actuali.model.Transaction
import com.azimulkabir.actuali.model.Type
import com.azimulkabir.actuali.ui.components.centsToInput
import com.azimulkabir.actuali.ui.components.CalculatorAmountSheet
import com.azimulkabir.actuali.ui.components.formatDate
import com.azimulkabir.actuali.ui.components.parseStoredDate
import com.azimulkabir.actuali.ui.components.storageDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    editing: Transaction?,
    onBack: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier,
    accountOptions: List<String> = listOf("Everyday account", "Cash", "Credit card"),
    categoryOptions: List<String> = listOf("Groceries", "Dining", "Transport", "Rent"),
    payeeOptions: List<String> = emptyList(),
    defaultAccount: String? = null,
    hideDecimalPlaces: Boolean = false,
    conventionalAmountEntry: Boolean = false,
) {
    var amountCents by remember(editing) { mutableStateOf(abs(editing?.amountCents ?: 0L)) }
    var showCalculator by remember { mutableStateOf(false) }
    var confirmDelete by remember(editing) { mutableStateOf(false) }
    var payee by remember(editing) { mutableStateOf(editing?.payee ?: "") }
    var category by remember(editing) { mutableStateOf(editing?.category ?: "") }
    var account by remember(editing, accountOptions) {
        mutableStateOf(
            if (editing?.type == Type.TRANSFER && editing.amountCents >= 0) {
                editing.transferAccount ?: editing.account
            } else editing?.account ?: defaultAccount?.takeIf(accountOptions::contains)
                ?: accountOptions.firstOrNull().orEmpty()
        )
    }
    var transferAccount by remember(editing) {
        mutableStateOf(
            if (editing?.type == Type.TRANSFER && editing.amountCents >= 0) editing.account
            else editing?.transferAccount.orEmpty()
        )
    }
    var date by remember(editing) { mutableStateOf(editing?.date?.let(::parseStoredDate) ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var notes by remember(editing) { mutableStateOf(editing?.notes ?: "") }
    var cleared by remember(editing) { mutableStateOf(editing?.cleared ?: false) }
    var transactionType by remember(editing) {
        mutableStateOf((editing?.type ?: Type.EXPENSE).displayName)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Cancel")
            }
            Text(if (editing == null) "Add transaction" else "Edit transaction",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Type.entries.forEach { type ->
                    FilterChip(
                        selected = transactionType == type.displayName,
                        onClick = { transactionType = type.displayName },
                        label = {
                            Text(
                                type.displayName,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = "৳${centsToInput(amountCents)}", onValueChange = {}, readOnly = true,
                    label = { Text("Amount") }, singleLine = true,
                    trailingIcon = { Icon(Icons.Outlined.Calculate, contentDescription = null) },
                    supportingText = { if (hideDecimalPlaces) Text("Decimal places are hidden in lists") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(Modifier.matchParentSize().clickable { showCalculator = true })
            }
            if (transactionType != Type.TRANSFER.displayName) {
                PickerTextField(
                    label = "Payee", value = payee, options = payeeOptions,
                    onValueChange = { payee = it }, editable = true,
                )
            }
            PickerTextField(
                label = "Category", value = category, options = categoryOptions,
                onValueChange = { category = it }, editable = true,
            )
            PickerTextField(
                label = if (transactionType == Type.TRANSFER.displayName) "From" else "Account",
                value = account, options = accountOptions,
                onValueChange = {
                    account = it
                    if (transferAccount == it) transferAccount = ""
                }, editable = true,
            )
            if (transactionType == Type.TRANSFER.displayName) {
                PickerTextField(
                    label = "To", value = transferAccount,
                    options = accountOptions.filterNot { it == account },
                    onValueChange = { transferAccount = it }, editable = true,
                )
            }
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = formatDate(date), onValueChange = {}, readOnly = true,
                    label = { Text("Date") }, singleLine = true,
                    trailingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(Modifier.matchParentSize().clickable { showDatePicker = true })
            }
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 2,
                modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Cleared", modifier = Modifier.weight(1f))
                Switch(checked = cleared, onCheckedChange = { cleared = it })
            }
            Button(onClick = {
                onSave(
                    Transaction(
                        id = editing?.id.orEmpty(),
                        date = storageDate(date),
                        payee = payee,
                        category = category.ifBlank { "Uncategorized" },
                        account = account,
                        amount = (amountCents / 100L).toInt() * if (transactionType == "Income") 1 else -1,
                        cleared = cleared,
                        amountCents = amountCents * if (transactionType == "Income") 1 else -1,
                        type = Type.entries.first { it.displayName == transactionType },
                        transferAccount = transferAccount.takeIf { transactionType == "Transfer" },
                        notes = notes,
                    )
                )
            }, enabled = amountCents > 0 && account.isNotBlank() &&
                (transactionType != "Transfer" || transferAccount.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)) {
                Text(if (editing == null) "Add transaction" else "Save changes")
            }
            if (editing != null) {
                FilledTonalButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            }
            FilledTonalButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Cancel")
            }
        }
    }
    if (showCalculator) CalculatorAmountSheet(
        title = if (editing == null) "Transaction amount" else "Edit amount",
        initialCents = amountCents,
        conventionalAmountEntry = conventionalAmountEntry,
        onDismiss = { showCalculator = false },
        onApply = { amountCents = it; showCalculator = false },
    )
    if (confirmDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete transaction?") },
            text = { Text("This transaction will be removed from your budget.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(editing)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}

private val Type.displayName: String get() = name.lowercase().replaceFirstChar(Char::uppercase)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerTextField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    editable: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = remember(value, options) {
        if (!editable || value.isBlank()) options.distinct()
        else options.filter { it.contains(value, ignoreCase = true) }.distinct()
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                if (editable) {
                    onValueChange(it)
                    expanded = true
                }
            },
            label = { Text(label) },
            singleLine = true,
            readOnly = !editable,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(
                    type = if (editable) {
                        ExposedDropdownMenuAnchorType.PrimaryEditable
                    } else {
                        ExposedDropdownMenuAnchorType.PrimaryNotEditable
                    },
                    enabled = true,
                )
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            filtered.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
            if (editable && value.isNotBlank() && value !in options) {
                DropdownMenuItem(
                    text = { Text("Use “$value”") },
                    onClick = { expanded = false },
                )
            }
        }
    }
}
