package com.example.smartpesa.data.preferences

enum class ThemePreference { SYSTEM, LIGHT, DARK }
enum class CurrencyPreference(val code: String) { KES("KES") }
enum class DateFormatPreference(val pattern: String, val label: String) {
    LONG("dd MMM yyyy", "dd MMM yyyy"),
    SLASH("dd/MM/yyyy", "dd/MM/yyyy"),
    MONTH_DAY("MMM d", "MMM d")
}
enum class WeekStartPreference(val label: String) { MON("Mon"), SAT("Sat"), SUN("Sun") }
enum class BudgetPeriodPreference(val label: String) { MONTHLY("Monthly"), WEEKLY("Weekly"), YEARLY("Yearly") }
enum class LanguagePreference(val code: String, val label: String) { EN("en", "English"), SW("sw", "Swahili") }
