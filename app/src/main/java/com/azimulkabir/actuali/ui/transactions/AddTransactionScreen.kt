package com.azimulkabir.actuali.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.azimulkabir.actuali.model.Transaction
import com.azimulkabir.actuali.model.Type
import com.azimulkabir.actuali.ui.components.centsToInput
import com.azimulkabir.actuali.ui.components.parseInputCents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    editing: Transaction?,
    onBack: () -> Unit,
    onSave: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    accountOptions: List<String> = listOf("Everyday account", "Cash", "Credit card"),
    categoryOptions: List<String> = listOf("Groceries", "Dining", "Transport", "Rent"),
    payeeOptions: List<String> = emptyList(),
    defaultAccount: String? = null,
    hideDecimalPlaces: Boolean = false,
) {
    var amount by remember(editing) { mutableStateOf(editing?.amountCents?.let(::centsToInput) ?: "") }
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
    var date by remember(editing) { mutableStateOf(editing?.date ?: today()) }
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
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Type.entries.forEach { type ->
                    FilterChip(
                        selected = transactionType == type.displayName,
                        onClick = { transactionType = type.displayName },
                        label = { Text(type.displayName) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, prefix = { Text("৳") },
                supportingText = { if (hideDecimalPlaces) Text("Decimal places are hidden in lists") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth())
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
            OutlinedTextField(date, { date = it }, label = { Text("Date") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
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
                        date = date,
                        payee = payee,
                        category = category.ifBlank { "Uncategorized" },
                        account = account,
                        amount = ((parseInputCents(amount) ?: 0L) / 100L).toInt() * if (transactionType == "Income") 1 else -1,
                        cleared = cleared,
                        amountCents = (parseInputCents(amount) ?: 0L) * if (transactionType == "Income") 1 else -1,
                        type = Type.entries.first { it.displayName == transactionType },
                        transferAccount = transferAccount.takeIf { transactionType == "Transfer" },
                        notes = notes,
                    )
                )
            }, enabled = (parseInputCents(amount) ?: 0L) > 0 && account.isNotBlank() &&
                (transactionType != "Transfer" || transferAccount.isNotBlank()),
                modifier = Modifier.fillMaxWidth()) {
                Text(if (editing == null) "Add transaction" else "Save changes")
            }
        }
    }
}

private val Type.displayName: String get() = name.lowercase().replaceFirstChar(Char::uppercase)

private fun today(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    .format(java.util.Date())

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
