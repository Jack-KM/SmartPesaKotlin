package com.example.smartpesa.ui.settings

import android.Manifest
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpesa.data.local.entity.Category
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 * Exposes capture mode status and category management
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Categories list
    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Capture mode status
    private val _captureStatus = MutableStateFlow(CaptureModeStatus())
    val captureStatus: StateFlow<CaptureModeStatus> = _captureStatus.asStateFlow()

    // Add/Edit Category dialog state
    private val _showCategoryDialog = MutableStateFlow(false)
    val showCategoryDialog: StateFlow<Boolean> = _showCategoryDialog.asStateFlow()

    private val _editingCategory = MutableStateFlow<Category?>(null)
    val editingCategory: StateFlow<Category?> = _editingCategory.asStateFlow()

    private val _categoryName = MutableStateFlow("")
    val categoryName: StateFlow<String> = _categoryName.asStateFlow()

    private val _categoryType = MutableStateFlow(TransactionType.EXPENSE)
    val categoryType: StateFlow<TransactionType> = _categoryType.asStateFlow()

    // App version
    val appVersion: String by lazy {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            "$versionName (Build $versionCode)"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    init {
        refreshCaptureStatus()
    }

    /**
     * Refresh capture mode status by checking permissions
     */
    fun refreshCaptureStatus() {
        val smsPermissionGranted = checkSmsPermission()
        val notificationListenerEnabled = com.example.smartpesa.util.NotificationListenerUtil.isNotificationListenerEnabled(context)
        val clipboardAvailable = true

        _captureStatus.value = CaptureModeStatus(
            smsPermissionGranted = smsPermissionGranted,
            notificationListenerEnabled = notificationListenerEnabled,
            clipboardAvailable = clipboardAvailable
        )
    }

    /**
     * Check if SMS permissions are granted
     */
    private fun checkSmsPermission(): Boolean {
        val receiveSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val readSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return receiveSms && readSms
    }

    /**
     * Show add category dialog
     */
    fun showAddCategoryDialog() {
        _editingCategory.value = null
        _categoryName.value = ""
        _categoryType.value = TransactionType.EXPENSE
        _showCategoryDialog.value = true
    }

    /**
     * Show edit category dialog
     */
    fun showEditCategoryDialog(category: Category) {
        _editingCategory.value = category
        _categoryName.value = category.name
        _categoryType.value = category.type
        _showCategoryDialog.value = true
    }

    /**
     * Hide category dialog
     */
    fun hideCategoryDialog() {
        _showCategoryDialog.value = false
        _editingCategory.value = null
        _categoryName.value = ""
        _categoryType.value = TransactionType.EXPENSE
    }

    /**
     * Update category name
     */
    fun onCategoryNameChanged(name: String) {
        _categoryName.value = name
    }

    /**
     * Update category type
     */
    fun onCategoryTypeChanged(type: TransactionType) {
        _categoryType.value = type
    }

    /**
     * Save category (add or update)
     */
    fun saveCategory() {
        val name = _categoryName.value.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            val editingCat = _editingCategory.value
            if (editingCat != null) {
                // Update existing category
                val updated = editingCat.copy(
                    name = name,
                    type = _categoryType.value
                )
                categoryRepository.updateCategory(updated)
            } else {
                // Add new category
                val newCategory = Category(
                    name = name,
                    type = _categoryType.value,
                    color = getDefaultColorForType(_categoryType.value)
                )
                categoryRepository.insertCategory(newCategory)
            }
            hideCategoryDialog()
        }
    }

    /**
     * Delete category
     */
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    /**
     * Get default color for transaction type
     */
    private fun getDefaultColorForType(type: TransactionType): String {
        return when (type) {
            TransactionType.INCOME -> "#4CAF50"  // Green
            TransactionType.EXPENSE -> "#F44336"  // Red
        }
    }
}

/**
 * Capture mode status data class
 */
data class CaptureModeStatus(
    val smsPermissionGranted: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val clipboardAvailable: Boolean = true
)
