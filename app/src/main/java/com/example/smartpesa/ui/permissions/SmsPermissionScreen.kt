package com.example.smartpesa.ui.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Screen for requesting SMS permissions
 *
 * Uses ActivityResultContracts instead of Accompanist Permissions because:
 * - Built into Android, no extra dependency
 * - More direct API, better lifecycle integration
 * - Accompanist Permissions is being deprecated
 *
 * Note: SMS permissions are "sensitive permissions" requiring:
 * 1. Runtime permission request (this screen)
 * 2. Play Store declaration form explaining usage
 * 3. Clear user-facing rationale (provided below)
 */
@Composable
fun SmsPermissionScreen(
    onPermissionsGranted: () -> Unit,
    onManualEntryRequested: () -> Unit
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    // Permission launcher using ActivityResultContracts
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val readSmsGranted = permissions[Manifest.permission.READ_SMS] ?: false

        if (receiveSmsGranted && readSmsGranted) {
            onPermissionsGranted()
        } else {
            permissionDenied = true
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Message,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SMS Access Required",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (showRationale || permissionDenied) {
                // Detailed rationale - required by Play Store review
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Why SMS Access?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = """
                                SmartPesa needs SMS access to automatically read and parse M-Pesa transaction messages.

                                • Only M-Pesa SMS are processed
                                • No personal messages are read or stored
                                • Transactions are saved locally on your device
                                • No data is sent to external servers

                                This is the core feature of SmartPesa - automatic expense tracking from your M-Pesa SMS.
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (permissionDenied) {
                Text(
                    text = "Permission was denied. You can grant it in Settings or use manual entry.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Settings")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!showRationale && !permissionDenied) {
                Text(
                    text = "SmartPesa automatically tracks your expenses by reading M-Pesa transaction SMS.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showRationale = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Learn More")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (showRationale && !permissionDenied) {
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant SMS Access")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = onManualEntryRequested,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use Manual Entry Instead")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You can always enable automatic tracking later in Settings",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
