package com.example.smartpesa.data.initialization

import com.example.smartpesa.data.local.entity.Account
import com.example.smartpesa.data.local.entity.AccountType
import com.example.smartpesa.data.repository.AccountRepository
import com.example.smartpesa.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountInitializer @Inject constructor(
    private val accountRepository: AccountRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend fun initializeIfNeeded() {
        val defaultCurrency = userPreferencesRepository.currencyPreference.first()
        val accounts = accountRepository.getAllAccounts().first()
        val requiredAccounts = listOf(
            Account(name = "M-Pesa", type = AccountType.MPESA, currencyCode = defaultCurrency, isDefault = true),
            Account(name = "Cash", type = AccountType.CASH, currencyCode = defaultCurrency),
            Account(name = "Airtel Money", type = AccountType.MPESA, currencyCode = defaultCurrency)
        )

        requiredAccounts.forEach { required ->
            val existing = accounts.firstOrNull { it.name.equals(required.name, ignoreCase = true) }
            if (existing == null) {
                accountRepository.insertAccount(required)
            } else if (existing.currencyCode.isNullOrBlank()) {
                accountRepository.updateAccount(existing.copy(currencyCode = defaultCurrency))
            }
        }

        if (accounts.none { it.isDefault } && accountRepository.getAllAccounts().first().any { it.name.equals("M-Pesa", ignoreCase = true) }) {
            val mpesa = accountRepository.getAllAccounts().first().firstOrNull { it.name.equals("M-Pesa", ignoreCase = true) }
            if (mpesa != null) {
                accountRepository.setDefaultAccount(mpesa.id)
            }
        }
    }
}
