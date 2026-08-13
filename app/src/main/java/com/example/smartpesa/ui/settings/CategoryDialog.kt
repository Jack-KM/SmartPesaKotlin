package com.example.smartpesa.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.smartpesa.data.local.entity.TransactionType

/**
 * Dialog for adding or editing a category
 * Shows name input and type selector (INCOME/EXPENSE)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val editingCategory by viewModel.editingCategory.collectAsState()
    val categoryName by viewModel.categoryName.collectAsState()
    val categoryType by viewModel.categoryType.collectAsState()
    val validationError by viewModel.validationError.collectAsState()

    val isEditing = editingCategory != null
    val title = if (isEditing) "Edit Category" else "Add Category"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category name input
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = viewModel::onCategoryNameChanged,
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g., Food, Transport, Salary") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category type selector
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup()
                    ) {
                        TransactionType.entries.forEach { type ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = categoryType == type,
                                        onClick = { viewModel.onCategoryTypeChanged(type) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = categoryType == type,
                                    onClick = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = type.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = when (type) {
                                            TransactionType.INCOME -> "Money coming in (salary, gifts, etc.)"
                                            TransactionType.EXPENSE -> "Money going out (purchases, bills, etc.)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveCategory()
                },
                enabled = categoryName.trim().isNotEmpty()
            ) {
                Text(if (isEditing) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
