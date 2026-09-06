package com.azimulkabir.actua.ui.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.azimulkabir.actua.model.BudgetCategory
import com.azimulkabir.actua.model.BudgetGroup
import com.azimulkabir.actua.model.BudgetOverview
import com.azimulkabir.actua.ui.components.CalculatorAmountState
import com.azimulkabir.actua.ui.components.formatMoneyCents
import com.azimulkabir.actua.ui.components.RenameDialog
import com.azimulkabir.actua.ui.components.NewCategoryDialog
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

private val sampleGroups = listOf(
    BudgetGroup("Monthly bills", listOf(
        BudgetCategory("Rent", 35_000, 35_000),
        BudgetCategory("Electricity", 3_500, 2_700),
        BudgetCategory("Internet", 1_500, 1_500),
        BudgetCategory("Mobile phone", 1_000, 720),
    )),
    BudgetGroup("Daily spending", listOf(
        BudgetCategory("Groceries", 8_000, 4_760),
        BudgetCategory("Dining", 4_000, 1_900),
        BudgetCategory("Transport", 5_000, 4_100),
        BudgetCategory("Household", 2_500, 850),
    )),
    BudgetGroup("Quality of life", listOf(
        BudgetCategory("Health & fitness", 3_000, 1_250),
        BudgetCategory("Entertainment", 2_500, 2_800),
        BudgetCategory("Personal care", 2_000, 620),
    )),
    BudgetGroup("Savings goals", listOf(
        BudgetCategory("Emergency fund", 10_000, 0),
        BudgetCategory("Travel", 6_000, 0),
    )),
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier,
    groups: List<BudgetGroup> = sampleGroups,
    overview: BudgetOverview = BudgetOverview(1_245_000, 8_400_000, -5_620_000, 2_780_000),
    month: String = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date()),
    onMonthChange: (String) -> Unit = {},
    hideDecimalPlaces: Boolean = false,
    showHidden: Boolean = false,
    onShowHiddenChange: (Boolean) -> Unit = {},
    showSpent: Boolean = true,
    onShowSpentChange: (Boolean) -> Unit = {},
    showProgressBars: Boolean = false,
    onShowProgressBarsChange: (Boolean) -> Unit = {},
    showOverview: Boolean = true,
    onShowOverviewChange: (Boolean) -> Unit = {},
    showGroupTotals: Boolean = true,
    onShowGroupTotalsChange: (Boolean) -> Unit = {},
    hideFullySpent: Boolean = false,
    onHideFullySpentChange: (Boolean) -> Unit = {},
    onSetCategoryHidden: (String, String, Boolean) -> Boolean = { _, _, _ -> false },
    onSetGroupHidden: (String, Boolean) -> Boolean = { _, _ -> false },
    onRenameCategory: (String, String, String) -> Unit = { _, _, _ -> },
    onRenameGroup: (String, String) -> Unit = { _, _ -> },
    onCreateCategory: (String, String) -> Unit = { _, _ -> },
    onCreateGroup: (String) -> Unit = {},
    onShowCategoryTransactions: (String, Boolean) -> Unit = { _, _ -> },
    onTransferBudget: (String?, String?, String?, String?, Long) -> Unit = { _, _, _, _, _ -> },
    onSetBudgetAmount: (String, String, Long) -> Unit = { _, _, _ -> },
    onSetCategoryNote: (String, String) -> Unit = { _, _ -> },
    onSetCategoryCarryover: (String, Boolean) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val budgetUiPreferences = remember(context) {
        context.applicationContext.getSharedPreferences("budget_ui_preferences", android.content.Context.MODE_PRIVATE)
    }
    var selectedCategory by remember { mutableStateOf<BudgetCategory?>(null) }
    var selectedGroup by remember { mutableStateOf<BudgetGroup?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var collapsedGroups by remember {
        mutableStateOf(budgetUiPreferences.getStringSet("collapsed_groups", emptySet()).orEmpty().toSet())
    }
    fun saveCollapsedGroups(value: Set<String>) {
        collapsedGroups = value
        budgetUiPreferences.edit().putStringSet("collapsed_groups", value).apply()
    }
    var optionsExpanded by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Pair<BudgetGroup, BudgetCategory>?>(null) }
    var renamingCategory by remember { mutableStateOf<Pair<BudgetGroup, BudgetCategory>?>(null) }
    var renamingGroup by remember { mutableStateOf<BudgetGroup?>(null) }
    var creatingCategory by remember { mutableStateOf(false) }
    var creatingGroup by remember { mutableStateOf(false) }
    var movingBudget by remember { mutableStateOf<Pair<BudgetGroup, BudgetCategory>?>(null) }
    var categoryDetails by remember { mutableStateOf<Pair<BudgetGroup, BudgetCategory>?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        BudgetToolbar(
            month = month,
            onMonthChange = onMonthChange,
            optionsExpanded = optionsExpanded,
            showSpent = showSpent,
            showProgressBars = showProgressBars,
            showOverview = showOverview,
            showGroupTotals = showGroupTotals,
            hideFullySpent = hideFullySpent,
            showHidden = showHidden,
            onOptionsChange = { optionsExpanded = it },
            onAdd = { showAddSheet = true },
            onShowSpentChange = onShowSpentChange,
            onShowProgressBarsChange = onShowProgressBarsChange,
            onShowOverviewChange = onShowOverviewChange,
            onShowGroupTotalsChange = onShowGroupTotalsChange,
            onHideFullySpentChange = onHideFullySpentChange,
            onShowHiddenChange = onShowHiddenChange,
            onExpandAll = {
                saveCollapsedGroups(emptySet())
                optionsExpanded = false
            },
            onCollapseAll = {
                saveCollapsedGroups(groups.mapTo(mutableSetOf()) { it.name })
                optionsExpanded = false
            },
        )
        AnimatedVisibility(visible = showOverview) {
            BudgetOverviewRow(overview, showSpent = showSpent, hideDecimalPlaces = hideDecimalPlaces)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            groups.filter { showHidden || !it.hidden }.forEach { group ->
                val collapsed = group.name in collapsedGroups
                val visibleCategories = group.categories.filter { category ->
                    (showHidden || !category.hidden) &&
                        (!hideFullySpent || category.available != 0)
                }
                stickyHeader(key = "header-${group.name}") {
                    BudgetGroupHeader(
                        group = group.copy(categories = visibleCategories),
                        collapsed = collapsed,
                        showSpent = showSpent,
                        showTotals = showGroupTotals,
                        hideDecimalPlaces = hideDecimalPlaces,
                        onClick = {
                            saveCollapsedGroups(if (collapsed) {
                                collapsedGroups - group.name
                            } else {
                                collapsedGroups + group.name
                            })
                        },
                        onLongClick = { selectedGroup = group },
                    )
                }
                itemsIndexed(
                    visibleCategories,
                    key = { _, category -> "${group.name}-${category.name}" },
                ) { index, category ->
                    AnimatedVisibility(
                        visible = !collapsed,
                        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 3 },
                        exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { -it / 3 },
                    ) {
                        CategoryRow(
                            category = category,
                            showSpent = showSpent,
                            showProgressBar = showProgressBars,
                            showTopDivider = index > 0,
                            onLongClick = { selectedCategory = category },
                            onEditBudget = { editingBudget = group to category },
                            onOpenDetails = { categoryDetails = group to category },
                            onShowSpentTransactions = {
                                onShowCategoryTransactions(category.name, true)
                            },
                            hideDecimalPlaces = hideDecimalPlaces,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    selectedCategory?.let { category ->
        val parent = groups.first { category in it.categories }
        CategoryActionsSheet(
            category = category,
            onDismiss = { selectedCategory = null },
            onRename = { selectedCategory = null; renamingCategory = parent to category },
            onEditBudget = { selectedCategory = null; editingBudget = parent to category },
            onDetails = { selectedCategory = null; categoryDetails = parent to category },
            onTransactionsThisMonth = { selectedCategory = null; onShowCategoryTransactions(category.name, true) },
            onAllTransactions = { selectedCategory = null; onShowCategoryTransactions(category.name, false) },
            onMoveMoney = { selectedCategory = null; movingBudget = parent to category },
            hidden = category.hidden,
            onSetHidden = { hidden ->
                if (onSetCategoryHidden(parent.name, category.name, hidden)) selectedCategory = null
            },
        )
    }
    selectedGroup?.let { group ->
        GroupActionsSheet(
            group = group,
            onDismiss = { selectedGroup = null },
            onRename = { selectedGroup = null; renamingGroup = group },
            hidden = group.hidden,
            onSetHidden = { hidden ->
                if (onSetGroupHidden(group.name, hidden)) selectedGroup = null
            },
        )
    }
    if (showAddSheet) {
        AddBudgetSheet(onDismiss = { showAddSheet = false },
            onNewCategory = { showAddSheet = false; creatingCategory = true },
            onNewGroup = { showAddSheet = false; creatingGroup = true })
    }
    editingBudget?.let { (group, category) ->
        EditBudgetAmountSheet(
            category = category,
            onDismiss = { editingBudget = null },
            onSave = { amount ->
                onSetBudgetAmount(group.name, category.name, amount)
                editingBudget = null
            },
        )
    }
    renamingCategory?.let { (group, category) -> RenameDialog("Rename category", category.name,
        onDismiss = { renamingCategory = null }, onSave = { name ->
            onRenameCategory(group.name, category.name, name); renamingCategory = null
        }) }
    renamingGroup?.let { group -> RenameDialog("Rename group", group.name,
        onDismiss = { renamingGroup = null }, onSave = { name -> onRenameGroup(group.name, name); renamingGroup = null }) }
    if (creatingCategory) NewCategoryDialog(groups.map { it.name }, { creatingCategory = false }) { group, name ->
        onCreateCategory(group, name); creatingCategory = false
    }
    if (creatingGroup) RenameDialog("New category group", "", { creatingGroup = false }) { name ->
        onCreateGroup(name); creatingGroup = false
    }
    movingBudget?.let { (group, category) -> MoveBudgetSheet(group, category, groups,
        onDismiss = { movingBudget = null }, onSave = { targetGroup, targetCategory, amount ->
            if (category.available < 0) onTransferBudget(targetGroup, targetCategory, group.name, category.name, amount)
            else onTransferBudget(group.name, category.name, targetGroup, targetCategory, amount)
            movingBudget = null
        }) }
    categoryDetails?.let { (group, category) ->
        CategoryDetailsSheet(
            category = category,
            month = month,
            hideDecimalPlaces = hideDecimalPlaces,
            onDismiss = { categoryDetails = null },
            onSaveNote = { note -> onSetCategoryNote(category.id.orEmpty(), note) },
            onSetCarryover = { enabled -> onSetCategoryCarryover(category.id.orEmpty(), enabled) },
            onAssign = { amount ->
                onSetBudgetAmount(group.name, category.name, amount)
                categoryDetails = null
            },
        )
    }
}

@Composable
private fun BudgetToolbar(
    month: String,
    onMonthChange: (String) -> Unit,
    optionsExpanded: Boolean,
    showSpent: Boolean,
    showProgressBars: Boolean,
    showOverview: Boolean,
    showGroupTotals: Boolean,
    hideFullySpent: Boolean,
    showHidden: Boolean,
    onOptionsChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onShowSpentChange: (Boolean) -> Unit,
    onShowProgressBarsChange: (Boolean) -> Unit,
    onShowOverviewChange: (Boolean) -> Unit,
    onShowGroupTotalsChange: (Boolean) -> Unit,
    onHideFullySpentChange: (Boolean) -> Unit,
    onShowHiddenChange: (Boolean) -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
) {
    var monthPickerOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(shiftMonth(month, -1)) }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Previous month")
            }
            Text(
                formatMonth(month),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .clickable { monthPickerOpen = true }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            )
            IconButton(onClick = { onMonthChange(shiftMonth(month, 1)) }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Next month")
            }
        }
        Box {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp,
            ) {
                Row {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add category")
                    }
                    IconButton(onClick = { onOptionsChange(true) }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Budget display options")
                    }
                }
            }
            DropdownMenu(
                expanded = optionsExpanded,
                onDismissRequest = { onOptionsChange(false) },
            ) {
                ToggleMenuItem("Show overview", showOverview, onShowOverviewChange)
                ToggleMenuItem("Show spent column", showSpent, onShowSpentChange)
                ToggleMenuItem("Show progress bars", showProgressBars, onShowProgressBarsChange)
                ToggleMenuItem("Show group totals", showGroupTotals, onShowGroupTotalsChange)
                HorizontalDivider()
                ToggleMenuItem("Hide fully spent", hideFullySpent, onHideFullySpentChange)
                ToggleMenuItem("Show hidden categories and groups", showHidden, onShowHiddenChange)
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Expand all groups") }, onClick = onExpandAll)
                DropdownMenuItem(text = { Text("Collapse all groups") }, onClick = onCollapseAll)
            }
        }
    }
    if (monthPickerOpen) {
        BudgetMonthPicker(
            selectedMonth = month,
            onDismiss = { monthPickerOpen = false },
            onSelect = {
                onMonthChange(it)
                monthPickerOpen = false
            },
        )
    }
}

@Composable
private fun BudgetMonthPicker(
    selectedMonth: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val selected = remember(selectedMonth) { java.time.YearMonth.parse(selectedMonth) }
    var displayedYear by remember(selectedMonth) { mutableStateOf(selected.year) }
    val monthNames = remember {
        (1..12).map { monthNumber ->
            java.time.Month.of(monthNumber).getDisplayName(
                java.time.format.TextStyle.SHORT,
                java.util.Locale.getDefault(),
            )
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { displayedYear-- }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Previous year")
                }
                Text(
                    displayedYear.toString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { displayedYear++ }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Next year")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                monthNames.chunked(3).forEachIndexed { rowIndex, rowMonths ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowMonths.forEachIndexed { columnIndex, label ->
                            val monthNumber = rowIndex * 3 + columnIndex + 1
                            val isSelected = displayedYear == selected.year && monthNumber == selected.monthValue
                            TextButton(
                                onClick = {
                                    onSelect(java.time.YearMonth.of(displayedYear, monthNumber).toString())
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else androidx.compose.ui.graphics.Color.Transparent,
                                ),
                            ) {
                                Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSelect(java.time.YearMonth.now().toString())
            }) { Text("Current month") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun shiftMonth(month: String, amount: Long): String =
    java.time.YearMonth.parse(month).plusMonths(amount).toString()

private fun formatMonth(month: String): String = java.time.YearMonth.parse(month)
    .format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.getDefault()))

@Composable
private fun ToggleMenuItem(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
        onClick = { onChange(!checked) },
    )
}

@Composable
private fun BudgetOverviewRow(overview: BudgetOverview, showSpent: Boolean, hideDecimalPlaces: Boolean) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverviewCell(
                "To budget",
                overview.toBudgetCents?.let { formatMoneyCents(it, hideDecimalPlaces) } ?: "—",
                Modifier.weight(1.35f),
                Alignment.Start,
                positive = overview.toBudgetCents?.let { it > 0 } == true,
            )
            OverviewCell("Budgeted", formatMoneyCents(overview.budgetedCents, hideDecimalPlaces), Modifier.weight(1f), Alignment.End)
            if (showSpent) OverviewCell("Spent", formatMoneyCents(overview.spentCents, hideDecimalPlaces), Modifier.weight(1f), Alignment.End)
            OverviewCell("Balance", formatMoneyCents(overview.availableCents, hideDecimalPlaces), Modifier.weight(1f), Alignment.End,
                positive = overview.availableCents >= 0)
        }
    }
}

@Composable
private fun OverviewCell(
    label: String,
    amount: String,
    modifier: Modifier,
    alignment: Alignment.Horizontal,
    positive: Boolean = false,
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.68f), maxLines = 1)
        Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BudgetGroupHeader(
    group: BudgetGroup,
    collapsed: Boolean,
    showSpent: Boolean,
    showTotals: Boolean,
    hideDecimalPlaces: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) -90f else 0f,
        animationSpec = tween(220),
        label = "group chevron",
    )
    val budgeted = group.categories.sumOf { it.assignedCents }
    val spent = group.categories.sumOf { it.spentCents }
    val balance = group.categories.sumOf { it.balanceCents }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().combinedClickable(
                role = Role.Button, onClick = onClick, onLongClick = onLongClick,
            ).animateContentSize().padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1.35f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (collapsed) "Expand ${group.name}" else "Collapse ${group.name}",
                    modifier = Modifier.width(24.dp).rotate(rotation),
                )
                Text(if (group.hidden) "${group.name} · Hidden" else group.name,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    color = if (group.hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (showTotals) {
                AmountColumn("Budgeted", budgeted, Modifier.weight(1f), hideDecimalPlaces)
                if (showSpent) AmountColumn("Spent", -spent, Modifier.weight(1f), hideDecimalPlaces, muted = spent == 0L)
                AmountColumn("Balance", balance, Modifier.weight(1f), hideDecimalPlaces, balance = true)
            } else {
                Spacer(Modifier.weight(if (showSpent) 3f else 2f))
            }
        }
    }
}

@Composable
private fun AmountColumn(
    label: String,
    amount: Long,
    modifier: Modifier,
    hideDecimalPlaces: Boolean,
    balance: Boolean = false,
    muted: Boolean = false,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        if (balance) {
            BalancePill(amount, hideDecimalPlaces)
        } else {
            Text(formatMoneyCents(amount, hideDecimalPlaces), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryRow(
    category: BudgetCategory,
    showSpent: Boolean,
    showProgressBar: Boolean,
    showTopDivider: Boolean,
    onLongClick: () -> Unit,
    onEditBudget: () -> Unit,
    onOpenDetails: () -> Unit,
    onShowSpentTransactions: () -> Unit,
    hideDecimalPlaces: Boolean,
) {
    if (showTopDivider) {
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    }
    Column(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onOpenDetails, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (category.hidden) "${category.name} · Hidden" else category.name,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.35f),
                color = if (category.hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            CategoryAmount(category.assignedCents, Modifier.weight(1f).combinedClickable(
                role = Role.Button, onClick = onEditBudget, onLongClick = onLongClick,
            ), hideDecimalPlaces)
            if (showSpent) CategoryAmount(
                -category.spentCents,
                Modifier.weight(1f).clickable(
                    role = Role.Button,
                    onClick = onShowSpentTransactions,
                ),
                hideDecimalPlaces,
                muted = category.spentCents == 0L,
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                BalancePill(category.balanceCents, hideDecimalPlaces)
            }
        }
        AnimatedVisibility(visible = showProgressBar) {
            val fraction = if (category.assigned <= 0) 0f
            else (category.spent.toFloat() / category.assigned).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp)
                    .clip(RoundedCornerShape(100)),
                color = if (category.available < 0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBudgetAmountSheet(
    category: BudgetCategory,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    val calculator = remember(category) {
        CalculatorAmountState(category.assignedCents, allowsNegative = true)
    }
    var revision by remember { mutableStateOf(0) }
    fun refresh(action: () -> Unit) { action(); revision += 1 }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(category.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Budgeted amount", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Text(calculator.display, style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
            val keys = listOf(
                listOf("7", "8", "9", "÷"), listOf("4", "5", "6", "×"),
                listOf("1", "2", "3", "−"), listOf("±", "0", "⌫", "+"),
            )
            keys.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        Button(
                            onClick = {
                                refresh {
                                    when (key) {
                                        "±" -> calculator.toggleSign()
                                        "⌫" -> calculator.backspace()
                                        "+" -> calculator.operator(CalculatorAmountState.Operator.ADD)
                                        "−" -> calculator.operator(CalculatorAmountState.Operator.SUBTRACT)
                                        "×" -> calculator.operator(CalculatorAmountState.Operator.MULTIPLY)
                                        "÷" -> calculator.operator(CalculatorAmountState.Operator.DIVIDE)
                                        else -> calculator.digit(key.toInt())
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(key) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onSave(calculator.finish()) },
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    @Suppress("UNUSED_EXPRESSION") revision
}

@Composable
private fun CategoryAmount(amount: Long, modifier: Modifier, hideDecimalPlaces: Boolean, muted: Boolean = false) {
    Text(
        formatMoneyCents(amount, hideDecimalPlaces), style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Normal,
        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End, maxLines = 1, modifier = modifier,
    )
}

@Composable
private fun BalancePill(amount: Long, hideDecimalPlaces: Boolean) {
    val positive = amount > 0
    val negative = amount < 0
    Text(
        text = formatMoneyCents(amount, hideDecimalPlaces),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = when {
            positive -> MaterialTheme.colorScheme.primary
            negative -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveBudgetSheet(
    sourceGroup: BudgetGroup,
    source: BudgetCategory,
    groups: List<BudgetGroup>,
    onDismiss: () -> Unit,
    onSave: (String?, String?, Long) -> Unit,
) {
    val options = listOf<Pair<String?, String?>>(null to null) + groups.flatMap { group ->
        group.categories.filterNot { group.name == sourceGroup.name && it.name == source.name }.map { group.name to it.name }
    }
    var selected by remember(source) { mutableStateOf(options.first()) }
    var expanded by remember { mutableStateOf(false) }
    val calculator = remember(source) { CalculatorAmountState(kotlin.math.abs(source.balanceCents), allowsNegative = false) }
    var revision by remember { mutableStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(if (source.available < 0) "Cover overspending" else "Move money",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth()) {
                Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selected.second?.let { "${selected.first} · $it" } ?: "To Budget")
                }
                DropdownMenu(expanded, { expanded = false }) { options.forEach { option ->
                    DropdownMenuItem(text = { Text(option.second?.let { "${option.first} · $it" } ?: "To Budget") },
                        onClick = { selected = option; expanded = false })
                } }
            }
            Text(calculator.display, style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
            listOf(listOf("7", "8", "9", "÷"), listOf("4", "5", "6", "×"),
                listOf("1", "2", "3", "−"), listOf("0", "⌫", "+")).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key -> Button(onClick = {
                        when (key) {
                            "⌫" -> calculator.backspace()
                            "+" -> calculator.operator(CalculatorAmountState.Operator.ADD)
                            "−" -> calculator.operator(CalculatorAmountState.Operator.SUBTRACT)
                            "×" -> calculator.operator(CalculatorAmountState.Operator.MULTIPLY)
                            "÷" -> calculator.operator(CalculatorAmountState.Operator.DIVIDE)
                            else -> calculator.digit(key.toInt())
                        }; revision++
                    }, modifier = Modifier.weight(1f)) { Text(key) } }
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { calculator.finish().takeIf { it > 0 }?.let { onSave(selected.first, selected.second, it) } },
                    modifier = Modifier.weight(1f)) { Text("Move") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    @Suppress("UNUSED_EXPRESSION") revision
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDetailsSheet(
    category: BudgetCategory,
    month: String,
    hideDecimalPlaces: Boolean,
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit,
    onSetCarryover: (Boolean) -> Unit,
    onAssign: (Long) -> Unit,
) {
    var note by remember(category) { mutableStateOf(category.note) }
    var noteEditorOpen by remember(category) { mutableStateOf(false) }
    var rollover by remember(category) { mutableStateOf(category.carryoverEnabled) }
    val suggestions = remember(category) {
        buildList {
            category.history.firstOrNull()?.let { last ->
                val spent = kotlin.math.abs(minOf(last.spentCents, 0L))
                if (spent > 0) add("Spent last month" to spent)
                if (last.assignedCents != 0L) add("Assigned last month" to last.assignedCents)
            }
            val spending = category.history.map { kotlin.math.abs(minOf(it.spentCents, 0L)) }
            if (spending.size >= 2) {
                val average = (spending.sum().toDouble() / spending.size).toLong()
                if (average > 0) add("Average spent (${spending.size} months)" to average)
            }
            if (category.balanceCents != 0L) add("Reset available to zero" to
                (category.assignedCents - category.balanceCents))
            if (category.assignedCents != 0L) add("Set assigned to zero" to 0L)
        }.distinctBy { it.first }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(category.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(formatMonth(month), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Note", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Surface(
                onClick = { noteEditorOpen = true },
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = note.ifBlank { "Add note" },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (note.isBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    maxLines = if (note.isBlank()) 1 else 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Rollover overspending", fontWeight = FontWeight.SemiBold)
                    Text("Carry overspending into the next month instead of taking it from To Budget.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(rollover, { enabled -> rollover = enabled; onSetCarryover(enabled) })
            }
            Text("Quick assign", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Current assigned: ${formatMoneyCents(category.assignedCents, hideDecimalPlaces)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (suggestions.isEmpty()) Text("No suggestions available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            suggestions.forEach { (label, amount) ->
                Surface(onClick = { onAssign(amount) }, color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(label, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Text(formatMoneyCents(amount, hideDecimalPlaces), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (noteEditorOpen) {
        var noteDraft by remember(category, noteEditorOpen) { mutableStateOf(note) }
        AlertDialog(
            onDismissRequest = { noteEditorOpen = false },
            title = { Text(if (note.isBlank()) "Add note" else "Edit note") },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                    placeholder = { Text("Category note") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    note = noteDraft
                    onSaveNote(noteDraft)
                    noteEditorOpen = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { noteEditorOpen = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryActionsSheet(
    category: BudgetCategory,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onEditBudget: () -> Unit,
    onDetails: () -> Unit,
    onTransactionsThisMonth: () -> Unit,
    onAllTransactions: () -> Unit,
    onMoveMoney: () -> Unit,
    hidden: Boolean,
    onSetHidden: (Boolean) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(category.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            SheetAction("Rename category", onRename)
            SheetAction("Budget details", onDetails)
            SheetAction("Edit budgeted amount", onEditBudget)
            SheetAction("Transactions this month", onTransactionsThisMonth)
            SheetAction("All transactions", onAllTransactions)
            if (category.available != 0) SheetAction(if (category.available < 0) "Cover overspending" else "Move money", onMoveMoney)
            SheetAction(if (hidden) "Unhide category" else "Hide category", { onSetHidden(!hidden) }, destructive = !hidden)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupActionsSheet(group: BudgetGroup, onDismiss: () -> Unit, onRename: () -> Unit,
    hidden: Boolean, onSetHidden: (Boolean) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            SheetAction("Rename group", onRename)
            SheetAction(if (hidden) "Unhide group" else "Hide group", { onSetHidden(!hidden) }, destructive = !hidden)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetSheet(onDismiss: () -> Unit, onNewCategory: () -> Unit, onNewGroup: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 28.dp)) {
            Text("Add to budget", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            SheetAction("New category", onNewCategory)
            SheetAction("New category group", onNewGroup)
            SheetAction("Apply budget template", onDismiss)
        }
    }
}

@Composable
private fun SheetAction(
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    DropdownMenuItem(
        text = {
            Text(label, color = if (destructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface)
        },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}
