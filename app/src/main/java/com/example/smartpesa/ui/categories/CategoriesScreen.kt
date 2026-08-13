package com.example.smartpesa.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartpesa.data.local.entity.Category
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.ui.components.CompactTopAppBar
import com.example.smartpesa.ui.components.EmptyStateScreen
import com.example.smartpesa.ui.settings.CategoryDialog
import com.example.smartpesa.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onMenuClick: () -> Unit = {}
) {
    val categories by viewModel.categories.collectAsState()
    val showCategoryDialog by viewModel.showCategoryDialog.collectAsState()
    val validationError by viewModel.validationError.collectAsState()
    var selectedTab by remember { mutableStateOf(TransactionType.EXPENSE) }

    val filteredCategories = categories.filter { it.type == selectedTab }

    Scaffold(
        topBar = {
            CompactTopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddCategoryDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = if (selectedTab == TransactionType.INCOME) 0 else 1) {
                Tab(
                    selected = selectedTab == TransactionType.INCOME,
                    onClick = { selectedTab = TransactionType.INCOME },
                    text = { Text("Income") }
                )
                Tab(
                    selected = selectedTab == TransactionType.EXPENSE,
                    onClick = { selectedTab = TransactionType.EXPENSE },
                    text = { Text("Expense") }
                )
            }

            if (validationError != null) {
                Text(
                    text = validationError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (filteredCategories.isEmpty()) {
                EmptyStateScreen(
                    title = if (selectedTab == TransactionType.INCOME) "No income categories" else "No expense categories",
                    message = "Add categories here, then use them in transactions and budgets.",
                    actionLabel = "Add Category",
                    onAction = { viewModel.showAddCategoryDialog() },
                    icon = if (selectedTab == TransactionType.INCOME) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCategories, key = { it.id }) { category ->
                        CategoryRow(
                            category = category,
                            onEdit = { viewModel.showEditCategoryDialog(category) },
                            onDelete = { viewModel.deleteCategory(category) }
                        )
                    }
                }
            }
        }

        if (showCategoryDialog) {
            CategoryDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.hideCategoryDialog() }
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    imageVector = if (category.type == TransactionType.INCOME) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Text(text = category.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = category.type.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit category")
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete category", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Category") },
            text = { Text("Delete \"${category.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
