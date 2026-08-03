package com.example.smartpesa.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.data.local.entity.TransactionType

/**
 * Settings screen with capture mode status and category management
 * Surfaces existing capture-mode system rather than rebuilding it
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val captureStatus by viewModel.captureStatus.collectAsState()
    val showCategoryDialog by viewModel.showCategoryDialog.collectAsState()

    // Refresh capture status when screen is visible
    LaunchedEffect(Unit) {
        viewModel.refreshCaptureStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddCategoryDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Capture Mode Section
            item {
                CaptureModeSection(
                    status = captureStatus,
                    onRefreshStatus = { viewModel.refreshCaptureStatus() },
                    onOpenSmsSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    onOpenNotificationSettings = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }

            // Categories Section
            item {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (categories.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No categories yet",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add categories to organize your transactions and set budgets",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(categories) { category ->
                    CategoryListItem(
                        category = category,
                        onEdit = { viewModel.showEditCategoryDialog(category) },
                        onDelete = { viewModel.deleteCategory(category) }
                    )
                }
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                AboutSection(appVersion = viewModel.appVersion)
            }
        }

        // Category dialog
        if (showCategoryDialog) {
            CategoryDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.hideCategoryDialog() }
            )
        }
    }
}

/**
 * Capture Mode section showing current status and enable buttons
 */
@Composable
private fun CaptureModeSection(
    status: CaptureModeStatus,
    onRefreshStatus: () -> Unit,
    onOpenSmsSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaction Capture",
                style = MaterialTheme.typography.titleLarge
            )

            IconButton(onClick = onRefreshStatus) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh status")
            }
        }

        // SMS Auto-capture
        CaptureModeStatusCard(
            title = "SMS Auto-capture",
            description = if (status.smsPermissionGranted) {
                "Active - M-Pesa SMS messages are being captured automatically"
            } else {
                "Inactive - Grant SMS permission to enable automatic capture"
            },
            icon = Icons.Default.Message,
            isActive = status.smsPermissionGranted,
            actionText = if (status.smsPermissionGranted) "Enabled" else "Enable",
            onAction = if (status.smsPermissionGranted) null else onOpenSmsSettings
        )

        // Notification Listener
        CaptureModeStatusCard(
            title = "Notification Listener",
            description = if (status.notificationListenerEnabled) {
                "Active - M-Pesa notifications are being captured automatically"
            } else {
                "Inactive - Grant notification access to enable automatic capture from M-Pesa notifications"
            },
            icon = Icons.Default.Notifications,
            isActive = status.notificationListenerEnabled,
            actionText = if (status.notificationListenerEnabled) "Enabled" else "Enable",
            onAction = if (status.notificationListenerEnabled) null else onOpenNotificationSettings,
            enabled = true
        )

        // Clipboard
        CaptureModeStatusCard(
            title = "Manual Paste",
            description = "Always available - Copy and paste M-Pesa SMS manually",
            icon = Icons.Default.ContentPaste,
            isActive = status.clipboardAvailable,
            actionText = "Always Active",
            onAction = null
        )
    }
}

/**
 * Capture mode status card - reuses design from CaptureModeCard
 */
@Composable
private fun CaptureModeStatusCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    actionText: String,
    onAction: (() -> Unit)?,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive && enabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isActive && enabled)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive && enabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isActive && enabled)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onAction != null) {
                Button(
                    onClick = onAction,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(actionText)
                }
            } else {
                Badge(
                    containerColor = if (isActive)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Category list item with edit/delete actions
 */
@Composable
private fun CategoryListItem(
    category: com.example.smartpesa.data.local.entity.Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (category.type == TransactionType.INCOME)
                        Icons.Default.TrendingUp
                    else
                        Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = category.type.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit category")
                }

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete category",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete \"${category.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * About section with app version
 */
@Composable
private fun AboutSection(appVersion: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column {
                    Text(
                        text = "SmartPesa",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Version $appVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
