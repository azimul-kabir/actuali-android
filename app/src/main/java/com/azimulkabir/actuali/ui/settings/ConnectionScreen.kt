package com.azimulkabir.actuali.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.azimulkabir.actuali.data.network.ActualServerClient
import com.azimulkabir.actuali.data.network.RemoteBudgetFile
import com.azimulkabir.actuali.data.budget.ActiveBudgetStore
import com.azimulkabir.actuali.data.budget.BudgetDownloadException
import com.azimulkabir.actuali.data.budget.BudgetDownloadService
import com.azimulkabir.actuali.data.budget.BudgetFileManager
import com.azimulkabir.actuali.data.budget.BackupItem
import com.azimulkabir.actuali.data.budget.BackupService
import com.azimulkabir.actuali.data.security.BudgetEncryptionKeyStore
import com.azimulkabir.actuali.data.security.CredentialStore
import com.azimulkabir.actuali.data.sync.ActualSyncRunner
import com.azimulkabir.actuali.data.sync.SyncRunResult
import com.azimulkabir.actuali.data.sync.SyncStatusStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ConnectionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onBeforeBudgetReplacement: () -> Unit = {},
    onBudgetInstalled: () -> Unit = {},
) {
    val context = LocalContext.current
    val credentials = remember { CredentialStore(context) }
    val client = remember { ActualServerClient() }
    val files = remember { BudgetFileManager(context) }
    val activeBudget = remember { ActiveBudgetStore(context) }
    val downloader = remember { BudgetDownloadService(client, files, BudgetEncryptionKeyStore(context)) }
    val backupService = remember { BackupService(context, files) }
    val scope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf(credentials.serverUrl) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(credentials.token() != null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var remoteBudgets by remember { mutableStateOf<List<RemoteBudgetFile>>(emptyList()) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var encryptionPassword by remember { mutableStateOf("") }
    val syncStatusStore = remember { SyncStatusStore(context) }
    var syncStatus by remember { mutableStateOf(syncStatusStore.read()) }
    var syncing by remember { mutableStateOf(false) }
    var backups by remember { mutableStateOf<List<BackupItem>>(emptyList()) }
    var backupBusy by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<BackupItem?>(null) }

    fun refreshBackups() {
        val budgetId = activeBudget.budgetId
        backups = if (budgetId == null) emptyList() else runCatching {
            backupService.availableBackups(budgetId)
        }.getOrDefault(emptyList())
    }

    fun loadBudgets() {
        val token = credentials.token() ?: return
        loading = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { client.listFiles(serverUrl, token) } }
                .onSuccess { remoteBudgets = it; message = if (it.isEmpty()) "No budgets found." else null }
                .onFailure { message = it.message ?: "Could not load budgets." }
            loading = false
        }
    }

    fun connectWithPassword() {
        loading = true
        message = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val normalized = client.normalizeServerUrl(serverUrl)
                    normalized to client.login(normalized, password)
                }
            }.onSuccess { (url, token) ->
                credentials.saveConnection(url, token)
                serverUrl = url
                password = ""
                connected = true
                message = "Connected"
                loadBudgets()
            }.onFailure { message = it.message ?: "Could not connect to the server." }
            loading = false
        }
    }

    val localNetworkPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) connectWithPassword()
        else message = "Local network access is required because this server resolves to a private network address."
    }

    LaunchedEffect(connected) {
        if (connected && remoteBudgets.isEmpty()) loadBudgets()
        refreshBackups()
    }

    pendingRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { if (!backupBusy) pendingRestore = null },
            title = { Text(if (backup is BackupItem.Latest) "Revert budget?" else "Restore backup?") },
            text = {
                Text(
                    if (backup is BackupItem.Latest) {
                        "Replace the restored budget with the version that was active immediately before the restore?"
                    } else if (connected) {
                        "Your current budget will be saved first. Restoring disconnects this budget from server sync; download it again later to resume syncing."
                    } else {
                        "Your current budget will be saved first, then replaced by this backup."
                    },
                )
            },
            confirmButton = {
                TextButton(enabled = !backupBusy, onClick = {
                    val budgetId = activeBudget.budgetId ?: return@TextButton
                    backupBusy = true
                    onBeforeBudgetReplacement()
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) {
                            backupService.restore(
                                budgetId,
                                when (backup) {
                                    BackupItem.Latest -> BackupService.LATEST_ID
                                    is BackupItem.Archive -> backup.id
                                },
                            )
                        } }.onSuccess {
                            message = if (backup is BackupItem.Latest) "Original budget restored." else "Backup restored. Server sync is disconnected."
                        }.onFailure { message = it.message ?: "Could not restore this backup." }
                        pendingRestore = null
                        backupBusy = false
                        refreshBackups()
                        onBudgetInstalled()
                    }
                }) { Text(if (backup is BackupItem.Latest) "Revert" else "Restore") }
            },
            dismissButton = { TextButton(enabled = !backupBusy, onClick = { pendingRestore = null }) { Text("Cancel") } },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text("Connection & data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = serverUrl, onValueChange = { serverUrl = it }, label = { Text("Server URL") },
                placeholder = { Text("https://actual.example.com") }, singleLine = true,
                enabled = !connected && !loading, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            if (!connected) {
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Password") },
                    singleLine = true, enabled = !loading, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 37 && ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_LOCAL_NETWORK,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            localNetworkPermission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                        } else {
                            connectWithPassword()
                        }
                    },
                    enabled = serverUrl.isNotBlank() && password.isNotBlank() && !loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.padding(end = 10.dp))
                    Text(if (loading) "Connecting…" else "Connect")
                }
            } else {
                Text("● Connected", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = {
                    credentials.clear()
                    connected = false
                    remoteBudgets = emptyList()
                    message = "Disconnected"
                }, modifier = Modifier.fillMaxWidth()) { Text("Disconnect") }
            }
            message?.let {
                Text(it, color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Text("Your token is encrypted with Android Keystore and remains on this device.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (connected) {
                Text("Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val last = syncStatus.lastSuccessMillis.takeIf { it > 0 }?.let {
                    java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))
                } ?: "Never"
                Text("Last successful sync: $last", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                syncStatus.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedButton(enabled = !syncing && !loading && downloadingId == null,
                    modifier = Modifier.fillMaxWidth(), onClick = {
                        syncing = true; message = null
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) {
                                syncStatusStore.started(); ActualSyncRunner.run(context)
                            } }.onSuccess { result ->
                                when (result) {
                                    is SyncRunResult.Success -> {
                                        syncStatusStore.succeeded(result.outcome)
                                        message = "Synced ${result.outcome.sentMessages} up, ${result.outcome.receivedMessages} down"
                                        onBudgetInstalled()
                                    }
                                    SyncRunResult.NotConfigured -> {
                                        syncStatusStore.stoppedWithoutSync(); message = "Download and select a budget first."
                                    }
                                    SyncRunResult.EncryptionKeyUnavailable -> {
                                        syncStatusStore.failed(IllegalStateException("Unlock this encrypted budget before syncing"))
                                        message = "Unlock this encrypted budget before syncing."
                                    }
                                }
                            }.onFailure { error -> syncStatusStore.failed(error); message = error.message ?: "Sync failed." }
                            syncStatus = syncStatusStore.read(); syncing = false
                        }
                    }) {
                    if (syncing) CircularProgressIndicator(Modifier.padding(end = 8.dp))
                    Text(if (syncing) "Syncing…" else "Sync now")
                }
                Text("Budgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (remoteBudgets.any { it.encryptedKeyId != null }) {
                    OutlinedTextField(
                        value = encryptionPassword,
                        onValueChange = { encryptionPassword = it },
                        label = { Text("Budget encryption password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                remoteBudgets.forEach { remote ->
                    val local = files.listLocalBudgets().firstOrNull { it.cloudFileId == remote.fileId }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(remote.name, fontWeight = FontWeight.Medium)
                            Text(
                                when {
                                    activeBudget.budgetId == local?.id -> "Active"
                                    local != null -> "Downloaded"
                                    remote.encryptedKeyId != null -> "Encrypted"
                                    else -> "Available"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            enabled = downloadingId == null,
                            onClick = {
                                val token = credentials.token() ?: return@OutlinedButton
                                onBeforeBudgetReplacement()
                                downloadingId = remote.fileId
                                message = null
                                scope.launch {
                                    runCatching {
                                        withContext(Dispatchers.IO) {
                                            if (remote.encryptedKeyId != null) {
                                                if (encryptionPassword.isNotBlank()) {
                                                    downloader.unlock(serverUrl, token, remote.fileId, encryptionPassword)
                                                }
                                            }
                                            downloader.download(serverUrl, token, remote)
                                        }
                                    }.onSuccess { metadata ->
                                        activeBudget.budgetId = metadata.id
                                        message = "${remote.name} is downloaded and active."
                                        onBudgetInstalled()
                                    }.onFailure { error ->
                                        message = when (error) {
                                            BudgetDownloadException.EncryptionPasswordRequired -> "Enter the budget encryption password."
                                            else -> error.message ?: "Could not download the budget."
                                        }
                                        onBudgetInstalled()
                                    }
                                    downloadingId = null
                                }
                            },
                        ) {
                            if (downloadingId == remote.fileId) CircularProgressIndicator(Modifier.padding(end = 8.dp))
                            Text(if (local == null) "Download" else if (activeBudget.budgetId == local.id) "Refresh" else "Use")
                        }
                    }
                }
                OutlinedButton(onClick = { loadBudgets() }, enabled = !loading && downloadingId == null,
                    modifier = Modifier.fillMaxWidth()) { Text("Refresh budget list") }
            }

            activeBudget.budgetId?.let { budgetId ->
                Text("Local backups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Backups stay on this device. Restoring a server budget intentionally disconnects that restored copy from sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !backupBusy && downloadingId == null && !syncing,
                    onClick = {
                        backupBusy = true; message = null
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { backupService.makeBackup(budgetId) } }
                                .onSuccess { message = "Backup created." }
                                .onFailure { message = it.message ?: "Could not create a backup." }
                            backupBusy = false; refreshBackups()
                        }
                    },
                ) {
                    if (backupBusy) CircularProgressIndicator(Modifier.padding(end = 8.dp))
                    Text(if (backupBusy) "Working…" else "Create backup now")
                }
                backups.forEach { backup ->
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(), enabled = !backupBusy,
                        onClick = { pendingRestore = backup },
                    ) {
                        Text(when (backup) {
                            BackupItem.Latest -> "Revert to pre-restore version"
                            is BackupItem.Archive -> "Restore ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date.from(backup.modifiedAt))}"
                        })
                    }
                }
                if (backups.isEmpty()) Text("No local backups yet.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
