package com.azimulkabir.actuali.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onConnectionClick: () -> Unit = {},
    hideDecimalPlaces: Boolean = false,
    onHideDecimalPlacesChange: (Boolean) -> Unit = {},
    hideBalances: Boolean = false,
    onHideBalancesChange: (Boolean) -> Unit = {},
    appearance: String = "System",
    onAppearanceChange: (String) -> Unit = {},
    startPage: String = "Accounts",
    onStartPageChange: (String) -> Unit = {},
    accountOptions: List<String> = emptyList(),
    defaultAccount: String? = null,
    onDefaultAccountChange: (String?) -> Unit = {},
    groupTransactionsByDate: Boolean = true,
    onGroupTransactionsByDateChange: (Boolean) -> Unit = {},
    showAccountsMonthlySummary: Boolean = true,
    onShowAccountsMonthlySummaryChange: (Boolean) -> Unit = {},
    onCreditCardsClick: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "More",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp),
        )
        SettingsSection("Data")
        SettingsRow("Connection & Data", "Actual server, budgets, local data and backups", true, onConnectionClick)
        SettingsSection("Transactions & Automation")
        SettingsChoice("Default account", defaultAccount ?: "None", listOf("None") + accountOptions) {
            onDefaultAccountChange(it.takeUnless { value -> value == "None" })
        }
        SettingsToggle("Group transactions by date", "Use dated sections in transaction lists", groupTransactionsByDate, onGroupTransactionsByDateChange)
        SettingsToggle(
            "Account monthly summary",
            "Show Income, Expenses and Net at the top of Accounts",
            showAccountsMonthlySummary,
            onShowAccountsMonthlySummaryChange,
        )
        SettingsRow("Credit Cards & Billing Cycles", "Cycle spend, due dates and credit limits", true, onCreditCardsClick)
        SettingsSection("Display")
        SettingsChoice("Appearance", appearance, listOf("System", "Light", "Dark"), onAppearanceChange)
        SettingsChoice("Start page", startPage, listOf("Budget", "Accounts", "Add", "Reports", "More"), onStartPageChange)
        SettingsToggle("Hide decimal places", "Round displayed amounts without changing their values", hideDecimalPlaces, onHideDecimalPlacesChange)
        SettingsSection("Privacy")
        SettingsToggle("Hide balances", "Mask budget, account and transaction amounts", hideBalances, onHideBalancesChange)
        SettingsSection("Information")
        ListItem(
            headlineContent = { Text("Actuali for Android") },
            supportingContent = { Text("Actual-compatible local budget and sync client") },
        )
    }
}

@Composable
private fun SettingsChoice(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }) { Text(value) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.distinct().forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            expanded = false
                            onChange(option)
                        })
                    }
                }
            }
        },
    )
}

@Composable
private fun SettingsToggle(
    label: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(detail) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
    )
}

@Composable
private fun SettingsSection(label: String) {
    HorizontalDivider()
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp))
}

@Composable
private fun SettingsRow(label: String, detail: String, enabled: Boolean = false, onClick: () -> Unit = {}) {
    ListItem(headlineContent = { Text(label) }, supportingContent = {
        Text(if (enabled) detail else "$detail · Coming with backend port")
    }, trailingContent = {
        if (enabled) Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }, modifier = Modifier.clickable(enabled = enabled, onClick = onClick))
}
