package com.example.smartpesa.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.userPreferencesDataStore

    val displayName: Flow<String> = stringFlow(DISPLAY_NAME_KEY, "")
    val themePreference: Flow<ThemePreference> = stringFlow(THEME_KEY, ThemePreference.SYSTEM.name).map { ThemePreference.valueOf(it) }
    val currencyPreference: Flow<String> = stringFlow(CURRENCY_KEY, CurrencyPreference.KES.code)
    val dateFormatPreference: Flow<String> = stringFlow(DATE_FORMAT_KEY, DateFormatPreference.LONG.pattern)
    val weekStartPreference: Flow<String> = stringFlow(WEEK_START_KEY, WeekStartPreference.MON.label)
    val defaultBudgetPeriodPreference: Flow<String> = stringFlow(BUDGET_PERIOD_KEY, BudgetPeriodPreference.MONTHLY.label)
    val languagePreference: Flow<String> = stringFlow(LANGUAGE_KEY, LanguagePreference.EN.code)
    val notificationsEnabled: Flow<Boolean> = booleanFlow(NOTIFICATIONS_ENABLED_KEY, true)
    val dailySummaryEnabled: Flow<Boolean> = booleanFlow(DAILY_SUMMARY_ENABLED_KEY, true)
    val budgetAlertsEnabled: Flow<Boolean> = booleanFlow(BUDGET_ALERTS_ENABLED_KEY, true)
    val largeTransactionAlertsEnabled: Flow<Boolean> = booleanFlow(LARGE_TX_ALERTS_ENABLED_KEY, true)
    val autoReadMpesaSms: Flow<Boolean> = booleanFlow(AUTO_READ_SMS_KEY, true)
    val hasCompletedPermissionSetup: Flow<Boolean> = booleanFlow(PERMISSION_SETUP_DONE_KEY, false)

    suspend fun setDisplayName(name: String) = editString(DISPLAY_NAME_KEY, name)
    suspend fun setThemePreference(value: ThemePreference) = editString(THEME_KEY, value.name)
    suspend fun setCurrencyPreference(value: String) = editString(CURRENCY_KEY, value)
    suspend fun setDateFormatPreference(value: String) = editString(DATE_FORMAT_KEY, value)
    suspend fun setWeekStartPreference(value: String) = editString(WEEK_START_KEY, value)
    suspend fun setDefaultBudgetPeriodPreference(value: String) = editString(BUDGET_PERIOD_KEY, value)
    suspend fun setLanguagePreference(value: String) = editString(LANGUAGE_KEY, value)
    suspend fun setNotificationsEnabled(value: Boolean) = editBool(NOTIFICATIONS_ENABLED_KEY, value)
    suspend fun setDailySummaryEnabled(value: Boolean) = editBool(DAILY_SUMMARY_ENABLED_KEY, value)
    suspend fun setBudgetAlertsEnabled(value: Boolean) = editBool(BUDGET_ALERTS_ENABLED_KEY, value)
    suspend fun setLargeTransactionAlertsEnabled(value: Boolean) = editBool(LARGE_TX_ALERTS_ENABLED_KEY, value)
    suspend fun setAutoReadMpesaSms(value: Boolean) = editBool(AUTO_READ_SMS_KEY, value)
    suspend fun setPermissionSetupDone(value: Boolean) = editBool(PERMISSION_SETUP_DONE_KEY, value)

    private fun stringFlow(key: Preferences.Key<String>, defaultValue: String): Flow<String> {
        return dataStore.data.map { preferences -> preferences[key] ?: defaultValue }
    }

    private fun booleanFlow(key: Preferences.Key<Boolean>, defaultValue: Boolean): Flow<Boolean> {
        return dataStore.data.map { preferences -> preferences[key] ?: defaultValue }
    }

    private suspend fun editString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { preferences ->
            if (value.isBlank()) preferences.remove(key) else preferences[key] = value.trim()
        }
    }

    private suspend fun editBool(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { preferences -> preferences[key] = value }
    }

    private companion object {
        val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val CURRENCY_KEY = stringPreferencesKey("currency")
        val DATE_FORMAT_KEY = stringPreferencesKey("date_format")
        val WEEK_START_KEY = stringPreferencesKey("week_start")
        val BUDGET_PERIOD_KEY = stringPreferencesKey("budget_period")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val DAILY_SUMMARY_ENABLED_KEY = booleanPreferencesKey("daily_summary_enabled")
        val BUDGET_ALERTS_ENABLED_KEY = booleanPreferencesKey("budget_alerts_enabled")
        val LARGE_TX_ALERTS_ENABLED_KEY = booleanPreferencesKey("large_tx_alerts_enabled")
        val AUTO_READ_SMS_KEY = booleanPreferencesKey("auto_read_sms")
        val PERMISSION_SETUP_DONE_KEY = booleanPreferencesKey("permission_setup_done")
    }
}
