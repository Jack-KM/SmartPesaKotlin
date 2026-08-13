package com.example.smartpesa.data.initialization

import android.util.Log
import com.example.smartpesa.data.categorization.MerchantNormalizer
import com.example.smartpesa.data.local.entity.PayeeCategoryRule
import com.example.smartpesa.data.repository.CategoryRepository
import com.example.smartpesa.data.repository.PayeeCategoryRuleRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds default payee → category rules for common Kenyan merchants/services
 * Only creates rules that don't already exist (never overwrites user-learned rules)
 */
@Singleton
class PayeeCategoryRuleInitializer @Inject constructor(
    private val payeeCategoryRuleRepository: PayeeCategoryRuleRepository,
    private val categoryRepository: CategoryRepository,
    private val merchantNormalizer: MerchantNormalizer
) {

    companion object {
        private const val TAG = "PayeeCategoryRuleInit"

        // Default merchant → category mappings for common Kenyan services
        private val DEFAULT_MERCHANTS = listOf(
            // Utilities
            "KPLC" to "Electricity (KPLC)",
            "KPLC PREPAID" to "Electricity (KPLC)",
            "Kenya Power" to "Electricity (KPLC)",

            // Telecom
            "Safaricom" to "Airtime",
            "Airtel" to "Airtime",
            "Telkom" to "Airtime",

            // TV Subscriptions - map to Entertainment
            "DSTV" to "Entertainment",
            "GOtv" to "Entertainment",
            "GOTV" to "Entertainment",
            "Zuku" to "Entertainment",

            // Supermarkets - map to Groceries
            "Carrefour" to "Groceries",
            "Naivas" to "Groceries",
            "Quickmart" to "Groceries",
            "Quick Mart" to "Groceries",
            "Tuskys" to "Groceries",
            "Chandarana" to "Groceries",
            "Chandarana Foodplus" to "Groceries",
            "Shoprite" to "Groceries",
            "Game Stores" to "Groceries",

            // Internet/Data
            "Safaricom Home Fibre" to "Internet",
            "Zuku Fibre" to "Internet",
            "Faiba" to "Internet",

            // Water
            "Nairobi Water" to "Water",
            "Nairobi City Water" to "Water"
        )
    }

    /**
     * Initialize default payee rules if needed
     * Should be called on app startup after categories are initialized
     */
    suspend fun initializeIfNeeded() {
        try {
            val categories = categoryRepository.getAllCategories().first()

            if (categories.isEmpty()) {
                Log.w(TAG, "Categories not initialized yet, skipping payee rules")
                return
            }

            val existingRules = payeeCategoryRuleRepository.getAll().first()

            // Only seed defaults if no rules exist yet (fresh install)
            // This prevents overwriting user-learned rules
            if (existingRules.isEmpty()) {
                Log.d(TAG, "Seeding default payee rules for common Kenyan merchants")
                seedDefaultRules(categories)
            } else {
                Log.d(TAG, "Payee rules already exist, skipping default seed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize payee rules", e)
        }
    }

    /**
     * Seed default merchant → category rules
     */
    private suspend fun seedDefaultRules(categories: List<com.example.smartpesa.data.local.entity.Category>) {
        var seededCount = 0

        for ((merchantName, categoryName) in DEFAULT_MERCHANTS) {
            try {
                // Find the category ID
                val category = categories.find { it.name.equals(categoryName, ignoreCase = true) }

                if (category == null) {
                    Log.w(TAG, "Category '$categoryName' not found for merchant '$merchantName', skipping")
                    continue
                }

                // Normalize the merchant name using the same logic as PayeeKeyDeriver
                val normalizedPayeeKey = merchantNormalizer.normalize(merchantName)

                if (normalizedPayeeKey.isEmpty()) {
                    Log.w(TAG, "Merchant '$merchantName' normalized to empty string, skipping")
                    continue
                }

                // Check if rule already exists (shouldn't happen in fresh install, but be safe)
                val existingRule = payeeCategoryRuleRepository.getByPayeeKey(normalizedPayeeKey)

                if (existingRule == null) {
                    // Create the default rule
                    val rule = PayeeCategoryRule(
                        payeeKey = normalizedPayeeKey,
                        categoryId = category.id,
                        timesUsed = 1,
                        lastUsedAt = System.currentTimeMillis()
                    )

                    payeeCategoryRuleRepository.upsertRule(rule)
                    seededCount++
                    Log.d(TAG, "Seeded rule: '$merchantName' → '$normalizedPayeeKey' → '${category.name}'")
                } else {
                    Log.d(TAG, "Rule for '$normalizedPayeeKey' already exists, skipping")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to seed rule for merchant '$merchantName'", e)
            }
        }

        Log.i(TAG, "Seeded $seededCount default payee rules")
    }
}
