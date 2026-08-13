package com.example.smartpesa.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.data.preferences.BudgetPeriodPreference
import com.example.smartpesa.data.preferences.DateFormatPreference
import com.example.smartpesa.data.preferences.LanguagePreference
import com.example.smartpesa.data.preferences.ThemePreference
import com.example.smartpesa.data.preferences.WeekStartPreference
import com.example.smartpesa.ui.components.CompactTopAppBar
import com.example.smartpesa.util.CurrencyFormatter
import kotlinx.coroutines.launch

private val ScreenBackground = Color(0xFF0F1216)
private val CardSurface = Color(0xFF1B2027)
private val DividerColor = Color(0x14FFFFFF)
private val PrimaryText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFF9AA3AD)
private val AccentIndigo = Color(0xFF7A7FF6)
private val DangerRed = Color(0xFFE35B68)
private val IncomeGreen = Color(0xFF7ED9A4)
private val ExpenseRose = Color(0xFFF2B8C0)

private enum class PickerKind { CURRENCY, DATE_FORMAT, WEEK_START, BUDGET_PERIOD, THEME, LANGUAGE }

data class PickerOption(val label: String, val value: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onMenuClick: () -> Unit = {},
    onOpenAccounts: () -> Unit = {},
    onOpenCaptureMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val displayName by viewModel.displayName.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val themePreference by viewModel.themePreference.collectAsState()
    val currencyPreference by viewModel.currencyPreference.collectAsState()
    val dateFormat by viewModel.dateFormat.collectAsState()
    val firstDayOfWeek by viewModel.firstDayOfWeek.collectAsState()
    val budgetPeriodDefault by viewModel.budgetPeriodDefault.collectAsState()
    val languagePreference by viewModel.languagePreference.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val dailySummaryEnabled by viewModel.dailySummaryEnabled.collectAsState()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsState()
    val largeTransactionAlertsEnabled by viewModel.largeTransactionAlertsEnabled.collectAsState()
    val autoReadSms by viewModel.autoReadMpesaSms.collectAsState()
    val captureStatus by viewModel.captureStatus.collectAsState()
    val transactionCount by viewModel.transactionCount.collectAsState()
    val appVersion = viewModel.appVersion

    var editableName by rememberSaveable { mutableStateOf(displayName) }
    var editingProfile by rememberSaveable { mutableStateOf(displayName.isBlank()) }
    var activePicker by remember { mutableStateOf<PickerKind?>(null) }
    var pendingCurrency by remember { mutableStateOf<String?>(null) }
    var clearAllDialog by remember { mutableStateOf(false) }
    var clearDeleteText by rememberSaveable { mutableStateOf("") }

    val notificationPermissionGranted = android.os.Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val smsPermissionGranted = captureStatus.smsPermissionGranted

    val profileDirty = editableName.trim() != displayName.trim()

    LaunchedEffect(displayName) {
        if (!profileDirty || !editingProfile) {
            editableName = displayName
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val error = viewModel.importBackup(uri)
                snackbarHostState.showSnackbar(error ?: "Backup imported")
            }
        }
    }

    fun shareText(subject: String, body: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(shareIntent, subject))
    }

    fun openAppSettings() {
        val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            CompactTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(title = "Profile") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarCircle(name = editableName.ifBlank { displayName.ifBlank { "SP" } })
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editableName,
                                onValueChange = { editableName = it.take(30) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                readOnly = !editingProfile,
                                label = { Text("Display name") },
                                placeholder = { Text("Enter your name") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (editingProfile && profileDirty && editableName.trim().isNotBlank()) {
                                                val trimmed = editableName.trim().take(30)
                                                scope.launch {
                                                    viewModel.saveDisplayName(trimmed)
                                                    snackbarHostState.showSnackbar("Name saved")
                                                }
                                                editingProfile = false
                                            } else {
                                                editingProfile = true
                                            }
                                        },
                                        enabled = if (editingProfile) profileDirty && editableName.trim().isNotBlank() else true
                                    ) {
                                        Icon(
                                            imageVector = if (editingProfile) Icons.Default.Check else Icons.Default.Edit,
                                            contentDescription = if (editingProfile) "Save name" else "Edit name"
                                        )
                                    }
                                }
                            )
                            Text(
                                text = "Used for greeting and reports",
                                color = SecondaryText,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Accounts") {
                    SettingsRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        tint = AccentIndigo,
                        label = "Manage accounts",
                        value = "${accounts.size} accounts",
                        onClick = onOpenAccounts
                    )
                }
            }

            item {
                SectionCard(title = "Preferences") {
                    SettingsRow(Icons.Default.AttachMoney, AccentIndigo, "Currency", currencyPreference, onClick = { activePicker = PickerKind.CURRENCY })
                    SettingsRow(Icons.Default.CalendarToday, IncomeGreen, "Date format", dateFormatLabel(dateFormat), onClick = { activePicker = PickerKind.DATE_FORMAT })
                    SettingsRow(Icons.Default.DateRange, ExpenseRose, "Week starts on", firstDayOfWeek, onClick = { activePicker = PickerKind.WEEK_START })
                    SettingsRow(Icons.Default.AccountBalanceWallet, AccentIndigo, "Default budget period", budgetPeriodDefault, onClick = { activePicker = PickerKind.BUDGET_PERIOD })
                    SettingsRow(Icons.Default.Palette, AccentIndigo, "Theme", themeLabel(themePreference), onClick = { activePicker = PickerKind.THEME })
                    SettingsRow(Icons.Default.Language, AccentIndigo, "Language", if (languagePreference == LanguagePreference.SW.code) "Swahili" else "English", onClick = { activePicker = PickerKind.LANGUAGE })
                }
            }

            item {
                SectionCard(title = "Notifications") {
                    SettingsToggleRow(
                        icon = Icons.Default.Notifications,
                        tint = AccentIndigo,
                        label = "Notifications",
                        checked = notificationsEnabled,
                        onCheckedChange = { checked ->
                            if (checked && !notificationPermissionGranted) {
                                viewModel.setNotificationsEnabled(false)
                                openAppSettings()
                                scope.launch { snackbarHostState.showSnackbar("Grant notification permission in app settings") }
                            } else {
                                viewModel.setNotificationsEnabled(checked)
                            }
                        }
                    )
                    HorizontalDivider(color = DividerColor)
                    SettingsToggleRow(
                        icon = Icons.Default.Notifications,
                        tint = IncomeGreen,
                        label = "Daily summary",
                        checked = dailySummaryEnabled,
                        enabled = notificationsEnabled,
                        onCheckedChange = viewModel::setDailySummaryEnabled
                    )
                    HorizontalDivider(color = DividerColor)
                    SettingsToggleRow(
                        icon = Icons.Default.Warning,
                        tint = ExpenseRose,
                        label = "Budget alerts",
                        checked = budgetAlertsEnabled,
                        enabled = notificationsEnabled,
                        onCheckedChange = viewModel::setBudgetAlertsEnabled
                    )
                    HorizontalDivider(color = DividerColor)
                    SettingsToggleRow(
                        icon = Icons.Default.Warning,
                        tint = DangerRed,
                        label = "Large transaction alerts",
                        checked = largeTransactionAlertsEnabled,
                        enabled = notificationsEnabled,
                        onCheckedChange = viewModel::setLargeTransactionAlertsEnabled
                    )
                }
            }

            item {
                SectionCard(title = "M-Pesa Import") {
                    SettingsToggleRow(
                        icon = Icons.Default.Message,
                        tint = AccentIndigo,
                        label = "Auto-read M-Pesa SMS",
                        checked = autoReadSms,
                        onCheckedChange = { checked ->
                            if (checked && !smsPermissionGranted) {
                                viewModel.setAutoReadMpesaSms(false)
                                onOpenCaptureMode()
                            } else {
                                viewModel.setAutoReadMpesaSms(checked)
                            }
                        }
                    )
                    if (!smsPermissionGranted) {
                        HorizontalDivider(color = DividerColor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SurfaceBadge(tint = ExpenseRose, icon = Icons.Default.Warning)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SMS permission missing", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                                Text("Grant access to read M-Pesa messages automatically.", color = SecondaryText, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = onOpenCaptureMode) { Text("Grant permission") }
                        }
                    }
                    HorizontalDivider(color = DividerColor)
                    SettingsRow(
                        icon = Icons.Default.Upload,
                        tint = AccentIndigo,
                        label = "Import SMS history",
                        value = "Open importer",
                        onClick = onOpenCaptureMode
                    )
                }
            }

            item {
                SectionCard(title = "Data") {
                    val hasTransactions = transactionCount > 0
                    SettingsActionRow(
                        icon = Icons.Default.Download,
                        tint = AccentIndigo,
                        label = "Export CSV",
                        enabled = hasTransactions,
                        hint = if (hasTransactions) null else "Needs at least 1 transaction",
                        onClick = {
                            scope.launch {
                                val csv = viewModel.buildCsvExport()
                                shareText("SmartPesa transactions.csv", csv)
                            }
                        }
                    )
                    HorizontalDivider(color = DividerColor)
                    SettingsActionRow(
                        icon = Icons.Default.Download,
                        tint = IncomeGreen,
                        label = "Backup JSON",
                        enabled = hasTransactions,
                        hint = if (hasTransactions) null else "Needs at least 1 transaction",
                        onClick = {
                            scope.launch {
                                val backup = viewModel.buildBackupJson()
                                shareText("SmartPesa backup.json", backup)
                            }
                        }
                    )
                    HorizontalDivider(color = DividerColor)
                    SettingsRow(
                        icon = Icons.Default.Upload,
                        tint = AccentIndigo,
                        label = "Import backup",
                        value = "JSON file",
                        onClick = {
                            importBackupLauncher.launch(arrayOf("application/json", "text/*"))
                        }
                    )
                    HorizontalDivider(color = DividerColor)
                    SettingsRow(
                        icon = Icons.Default.Delete,
                        tint = DangerRed,
                        label = "Clear all data",
                        value = "Permanent",
                        onClick = { clearAllDialog = true }
                    )
                }
            }

            item {
                SectionCard(title = "About") {
                    SettingsRow(Icons.Default.Info, AccentIndigo, "SmartPesa version", appVersion, onClick = null)
                    SettingsRow(Icons.Default.Info, AccentIndigo, "Support / FAQ", "Open", onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Support link not wired yet") }
                    })
                    SettingsRow(Icons.Default.Info, AccentIndigo, "Privacy policy", "Open", onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Privacy policy link not wired yet") }
                    })
                    SettingsRow(Icons.Default.Info, AccentIndigo, "Rate app", "Open", onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Rate app link not wired yet") }
                    })
                }
            }
        }
    }

    if (activePicker != null) {
        val options = remember(activePicker) { pickerOptions(activePicker!!) }
        ModalBottomSheet(
            onDismissRequest = { activePicker = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pickerTitle(activePicker!!), style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = PrimaryText)
                options.forEach { option ->
                    SettingsPickerRow(
                        label = option.label,
                        selected = option.value == currentPickerValue(activePicker!!, currencyPreference, dateFormat, firstDayOfWeek, budgetPeriodDefault, themePreference, languagePreference),
                        onClick = {
                            when (activePicker!!) {
                                PickerKind.CURRENCY -> pendingCurrency = option.value
                                PickerKind.DATE_FORMAT -> viewModel.setDateFormat(option.value)
                                PickerKind.WEEK_START -> viewModel.setFirstDayOfWeek(option.value)
                                PickerKind.BUDGET_PERIOD -> viewModel.setBudgetPeriodDefault(option.value)
                                PickerKind.THEME -> viewModel.setThemePreference(ThemePreference.valueOf(option.value))
                                PickerKind.LANGUAGE -> viewModel.setLanguagePreference(option.value)
                            }
                            activePicker = null
                        }
                    )
                }
            }
        }
    }

    if (pendingCurrency != null) {
        AlertDialog(
            onDismissRequest = { pendingCurrency = null },
            title = { Text("Change currency?") },
            text = { Text("Amounts are relabelled, not converted.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.setCurrencyPreference(pendingCurrency!!)
                    CurrencyFormatter.setCurrencyCode(pendingCurrency!!)
                    scope.launch { snackbarHostState.showSnackbar("Currency updated") }
                    pendingCurrency = null
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCurrency = null }) { Text("Cancel") }
            }
        )
    }

    if (clearAllDialog) {
        AlertDialog(
            onDismissRequest = { clearAllDialog = false; clearDeleteText = "" },
            title = { Text("Clear all data?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This removes transactions, budgets, loans, Fuliza, costs, categories, and accounts.")
                    OutlinedButton(onClick = {
                        scope.launch {
                            val backup = viewModel.buildBackupJson()
                            shareText("SmartPesa backup.json", backup)
                        }
                    }) {
                        Text("Export first")
                    }
                    OutlinedTextField(
                        value = clearDeleteText,
                        onValueChange = { clearDeleteText = it },
                        label = { Text("Type DELETE") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        clearAllDialog = false
                        clearDeleteText = ""
                        scope.launch { snackbarHostState.showSnackbar("All data cleared") }
                    },
                    enabled = clearDeleteText == "DELETE"
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { clearAllDialog = false; clearDeleteText = "" }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                color = PrimaryText,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    onClick: (() -> Unit)?
) {
    SettingsBaseRow(icon, tint, label, onClick) {
        Text(value, color = SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    enabled: Boolean,
    hint: String?,
    onClick: () -> Unit
) {
    SettingsBaseRow(icon, tint, label, if (enabled) onClick else null) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (enabled) "Share sheet" else "Disabled",
                color = if (enabled) SecondaryText else SecondaryText.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hint != null) {
                Text(hint, color = SecondaryText.copy(alpha = 0.7f), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsBaseRow(icon, tint, label, null, enabled = enabled) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SettingsPickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = PrimaryText)
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AccentIndigo)
        }
    }
}

@Composable
private fun SettingsBaseRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SurfaceBadge(tint = tint, icon = icon)
        Text(
            text = label,
            color = if (enabled) PrimaryText else PrimaryText.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SecondaryText)
        }
    }
}

@Composable
private fun SurfaceBadge(tint: Color, icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(tint.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AvatarCircle(name: String) {
    val initials = initialsFor(name)
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(AccentIndigo.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = PrimaryText, fontWeight = FontWeight.SemiBold)
    }
}

private fun initialsFor(value: String): String {
    val parts = value.trim().split(Regex("\\s+"))
    return parts.filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "SP" }
}

private fun dateFormatLabel(pattern: String): String = when (pattern) {
    DateFormatPreference.SLASH.pattern -> DateFormatPreference.SLASH.label
    DateFormatPreference.MONTH_DAY.pattern -> DateFormatPreference.MONTH_DAY.label
    else -> DateFormatPreference.LONG.label
}

private fun pickerTitle(kind: PickerKind): String = when (kind) {
    PickerKind.CURRENCY -> "Currency"
    PickerKind.DATE_FORMAT -> "Date format"
    PickerKind.WEEK_START -> "Week starts on"
    PickerKind.BUDGET_PERIOD -> "Default budget period"
    PickerKind.THEME -> "Theme"
    PickerKind.LANGUAGE -> "Language"
}

private fun pickerOptions(kind: PickerKind): List<PickerOption> = when (kind) {
    PickerKind.CURRENCY -> listOf("KES", "USD", "UGX", "TZS").map { PickerOption(it, it) }
    PickerKind.DATE_FORMAT -> DateFormatPreference.entries.map { PickerOption(it.label, it.pattern) }
    PickerKind.WEEK_START -> WeekStartPreference.entries.map { PickerOption(it.label, it.label) }
    PickerKind.BUDGET_PERIOD -> BudgetPeriodPreference.entries.map { PickerOption(it.label, it.label) }
    PickerKind.THEME -> ThemePreference.entries.map { PickerOption(it.name.lowercase().replaceFirstChar { char -> char.uppercaseChar().toString() }, it.name) }
    PickerKind.LANGUAGE -> LanguagePreference.entries.map { PickerOption(it.label, it.code) }
}

private fun currentPickerValue(
    kind: PickerKind,
    currency: String,
    dateFormat: String,
    weekStart: String,
    budgetPeriod: String,
    theme: ThemePreference,
    language: String
): String = when (kind) {
    PickerKind.CURRENCY -> currency
    PickerKind.DATE_FORMAT -> dateFormat
    PickerKind.WEEK_START -> weekStart
    PickerKind.BUDGET_PERIOD -> budgetPeriod
    PickerKind.THEME -> theme.name
    PickerKind.LANGUAGE -> language
}

private fun themeLabel(theme: ThemePreference): String = when (theme) {
    ThemePreference.SYSTEM -> "System"
    ThemePreference.LIGHT -> "Light"
    ThemePreference.DARK -> "Dark"
}
