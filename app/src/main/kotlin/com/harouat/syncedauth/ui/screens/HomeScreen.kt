package com.harouat.syncedauth.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.harouat.syncedauth.MainViewModel
import com.harouat.syncedauth.data.Account
import com.harouat.syncedauth.data.BackupCodec
import com.harouat.syncedauth.ui.components.CodeCard
import com.harouat.syncedauth.ui.components.GearButton
import com.harouat.syncedauth.ui.components.GradientButton
import com.harouat.syncedauth.ui.components.SearchPill
import com.harouat.syncedauth.ui.components.SecondaryButton
import com.harouat.syncedauth.ui.theme.LocalSaColors
import com.harouat.syncedauth.ui.theme.SaType
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onAddAccount: () -> Unit,
) {
    val sa = LocalSaColors.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val visible by vm.visibleAccounts.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val uiMessage by vm.uiMessage.collectAsStateWithLifecycle()

    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<List<Account>?>(null) }
    var pendingDelete by remember { mutableStateOf<Account?>(null) }

    // Shared wall-clock ticker: all cards recompute codes on the same second.
    var nowSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000 - System.currentTimeMillis() % 1000)
            nowSeconds = System.currentTimeMillis() / 1000
        }
    }

    // Export: SAF CreateDocument with proposed name, then stream JSON out.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            vm.viewModelScope.launch {
                runCatching {
                    val out = context.contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("no output stream")
                    out.use { it.write(vm.exportJson().toByteArray(Charsets.UTF_8)) }
                }
                    .onSuccess { vm.message("Backup saved successfully") }
                    .onFailure { vm.message("Couldn't save the backup") }
            }
        }
    }

    // Import: SAF OpenDocument, parse, then confirm replace.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.viewModelScope.launch {
                try {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("no input stream")
                    pendingImport = BackupCodec.decode(text)
                } catch (e: Exception) {
                    vm.message("Couldn't import the file — it may not be a valid backup")
                }
            }
        }
    }

    fun openExport() {
        settingsOpen = false
        exportLauncher.launch("syncedauth_backup_${System.currentTimeMillis()}.json")
    }

    fun openImport() {
        settingsOpen = false
        importLauncher.launch(arrayOf("*/*"))
    }

    fun haptic() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun copyCode(code: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SyncedAuth", code).apply {
            // Mark the clip sensitive: AOSP Android 13+ then skips its clipboard
            // preview overlay, so our snackbar is the only confirmation shown —
            // and the code can't be echoed by the clipboard UI.
            description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        cm.setPrimaryClip(clip)
        haptic()
        // Samsung One UI always shows its own "Copied to clipboard" toast (it
        // never adopted the AOSP suppression above), so skip our snackbar there
        // to avoid two confirmations stacked on screen.
        val oneUiShowsOwnToast = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        if (!oneUiShowsOwnToast) {
            scope.launch { snackbar.showSnackbar("Code copied", duration = SnackbarDuration.Short) }
        }
    }

    fun deleteWithUndo(account: Account) {
        haptic()
        vm.delete(account)
        scope.launch {
            val result = snackbar.showSnackbar(
                message = "Account deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
        }
    }

    fun pinAccount(account: Account) {
        haptic()
        vm.togglePin(account)
    }

    // ---- Long-press drag to reorder (sh.calvin.reorderable) ---------------
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = WindowInsets.safeDrawing.asPaddingValues(),
    ) { from, to ->
        vm.reorderInCache(from.index, to.index)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LaunchedEffect(uiMessage) {
        val msg = uiMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(msg, duration = SnackbarDuration.Short)
        vm.consumeMessage()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(sa.pageGradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Large title + settings gear
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SyncedAuth",
                    style = SaType.title,
                    color = sa.textNormal,
                )
                Spacer(Modifier.weight(1f))
                GearButton(icon = Icons.Outlined.Settings) { settingsOpen = true }
            }

            Box(Modifier.weight(1f)) {
                if (!loaded) {
                    // Keep the background plain until the first DB read lands,
                    // so the empty state never flashes on cold start.
                } else if (accounts.isEmpty()) {
                    EmptyState(onAdd = onAddAccount, onImport = { openImport() })
                } else if (visible.isEmpty() && query.isNotBlank()) {
                    NoResults()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(visible, key = { it.id }, contentType = { "card" }) { account ->
                            ReorderableItem(reorderState, key = account.id) { isDragging ->
                                Box(
                                    Modifier
                                        // Smooth fade/slide when a card is removed
                                        // from the list (undo restores it the same way).
                                        .animateItem()
                                        .graphicsLayer {
                                            if (isDragging) {
                                                scaleX = 1.03f
                                                scaleY = 1.03f
                                            }
                                        }
                                        .then(
                                            if (isDragging) {
                                                Modifier.shadow(
                                                    elevation = 22.dp,
                                                    shape = RoundedCornerShape(16.dp),
                                                    clip = false,
                                                    ambientColor = Color(0x22000000),
                                                    spotColor = Color(0x44000000),
                                                )
                                            } else Modifier
                                        )
                                        // Long-press anywhere on the card starts a reorder
                                        // drag; disabled while searching. A quick horizontal
                                        // swipe still reaches the card's own reveal gesture.
                                        .then(
                                            if (query.isBlank()) {
                                                Modifier.longPressDraggableHandle(
                                                    onDragStarted = {
                                                        haptics.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                    },
                                                    onDragStopped = { vm.commitReorder() },
                                                )
                                            } else Modifier
                                        )
                                ) {
                                    CodeCard(
                                        account = account,
                                        nowSeconds = nowSeconds,
                                        query = query,
                                        onCopy = { code -> copyCode(code) },
                                        onDelete = { haptic(); pendingDelete = account },
                                        onTogglePin = { pinAccount(account) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            ) { data ->
                Row(
                    Modifier
                        .clip(RoundedCornerShape(100))
                        .background(sa.snackbarBg)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = data.visuals.message,
                        style = SaType.body2Medium,
                        color = sa.snackbarText,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    data.visuals.actionLabel?.let { label ->
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = label,
                            style = SaType.body2Medium,
                            color = sa.accent,
                            modifier = Modifier.clickable { data.performAction() },
                        )
                    }
                }
            }

            // Bottom bar: search pill + gradient FAB (fixed, outside the scroll)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchPill(
                    value = query,
                    onValueChange = { vm.setQuery(it) },
                    placeholder = "Search accounts...",
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(54.dp)
                        .shadow(elevation = 10.dp, shape = CircleShape, spotColor = sa.focus, ambientColor = sa.accent)
                        .clip(CircleShape)
                        .background(sa.buttonGradient)
                        .clickable { onAddAccount() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }

    if (settingsOpen) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            sheetState = sheetState,
            containerColor = sa.card,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(text = "Settings", style = SaType.headline, color = sa.textNormal)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Accounts are stored locally on this device as JSON",
                    style = SaType.caption,
                    color = sa.textWeak,
                )
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, sa.cardBorder, RoundedCornerShape(16.dp)),
                ) {
                    SettingsRow(
                        icon = Icons.Outlined.FileDownload,
                        title = "Save data",
                        subtitle = "Export all your accounts to a file",
                        onClick = { openExport() },
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = sa.cardBorder,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SettingsRow(
                        icon = Icons.Outlined.FileUpload,
                        title = "Import data",
                        subtitle = "Replace your current accounts from a file",
                        onClick = { openImport() },
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "SyncedAuth v1.0 · ${accounts.size} accounts",
                    style = SaType.caption,
                    color = sa.textPlaceholder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    pendingDelete?.let { account ->
        ModalBottomSheet(
            onDismissRequest = { pendingDelete = null },
            containerColor = sa.card,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "Delete account", style = SaType.headline, color = sa.textNormal)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "“${account.issuer.ifBlank { account.name }}” will be removed from " +
                        "this device. You can undo it right after.",
                    style = SaType.body2,
                    color = sa.textWeak,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(sa.error)
                        .clickable {
                            pendingDelete = null
                            deleteWithUndo(account)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Delete", style = SaType.body1Bold, color = Color.White)
                }
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    label = "Cancel",
                    onClick = { pendingDelete = null },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    pendingImport?.let { imported ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            containerColor = sa.card,
            title = {
                Text(text = "Import data", style = SaType.headline, color = sa.textNormal)
            },
            text = {
                Text(
                    text = "Found ${imported.size} accounts. Importing will replace all of your current accounts. Continue?",
                    style = SaType.body2.copy(color = sa.textWeak),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val list = imported
                    pendingImport = null
                    vm.viewModelScope.launch {
                        runCatching { vm.importJson(BackupCodec.encode(list)) }
                            .onSuccess { count -> vm.message("Imported successfully ($count accounts)") }
                            .onFailure { vm.message("Couldn't import the file") }
                    }
                }) {
                    Text(text = "Import", style = SaType.body1Bold, color = sa.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text(text = "Cancel", style = SaType.body1Medium, color = sa.textWeak)
                }
            },
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val sa = LocalSaColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = sa.textNormal,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = SaType.body1Medium, color = sa.textNormal)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = SaType.body2, color = sa.textWeak)
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = sa.textPlaceholder,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit, onImport: () -> Unit) {
    val sa = LocalSaColors.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = Color(0xFFA8A5A3),
            modifier = Modifier.size(88.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(text = "No accounts yet", style = SaType.headline, color = sa.textNormal)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Add your first account to get started",
            style = SaType.body1,
            color = sa.textWeak,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        GradientButton(label = "Add account", onClick = onAdd, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        SecondaryButton(label = "Import data", onClick = onImport, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NoResults() {
    val sa = LocalSaColors.current
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = sa.textPlaceholder,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(text = "No results", style = SaType.body18.copy(color = sa.textWeak))
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Try a different search",
            style = SaType.body2,
            color = sa.textPlaceholder,
        )
    }
}
