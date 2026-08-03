package com.example.smartpesa.data.initialization

import com.example.smartpesa.data.local.entity.Category
import com.example.smartpesa.data.local.entity.TransactionType
import com.example.smartpesa.data.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes the database with pre-populated Kenyan budget categories
 * Creates main categories and sub-categories relevant to Kenyan budgeting
 */
@Singleton
class CategoryInitializer @Inject constructor(
    private val categoryRepository: CategoryRepository
) {

    /**
     * Initialize categories if database is empty
     * Should be called on app startup
     */
    suspend fun initializeIfNeeded() {
        val existingCategories = categoryRepository.getAllCategories().first()

        if (existingCategories.isEmpty()) {
            initializeCategories()
        }
    }

    /**
     * Pre-populate database with Kenyan budget categories
     * Includes main categories and relevant sub-categories
     */
    private suspend fun initializeCategories() {
        // EXPENSE CATEGORIES

        // Food & Dining (Green)
        val foodId = categoryRepository.insertCategory(
            Category(name = "Food & Dining", type = TransactionType.EXPENSE, color = "#4CAF50", icon = "Restaurant")
        )
        categoryRepository.insertCategories(listOf(
            Category(name = "Groceries", type = TransactionType.EXPENSE, color = "#4CAF50", icon = "ShoppingCart", parentCategoryId = foodId),
            Category(name = "Restaurants", type = TransactionType.EXPENSE, color = "#4CAF50", icon = "Restaurant", parentCategoryId = foodId),
            Category(name = "Takeout", type = TransactionType.EXPENSE, color = "#4CAF50", icon = "Fastfood", parentCategoryId = foodId),
            Category(name = "Market", type = TransactionType.EXPENSE, color = "#4CAF50", icon = "Storefront", parentCategoryId = foodId)
        ))

        // Transport (Blue)
        val transportId = categoryRepository.insertCategory(
            Category(name = "Transport", type = TransactionType.EXPENSE, color = "#2196F3", icon = "DirectionsCar")
        )
        categoryRepository.insertCategories(listOf(
            Category(name = "Matatu/Bus", type = TransactionType.EXPENSE, color = "#2196F3", icon = "DirectionsBus", parentCategoryId = transportId),
            Category(name = "Boda Boda", type = TransactionType.EXPENSE, color = "#2196F3", icon = "TwoWheeler", parentCategoryId = transportId),
            Category(name = "Uber/Taxi", type = TransactionType.EXPENSE, color = "#2196F3", icon = "LocalTaxi", parentCategoryId = transportId),
            Category(name = "Fuel", type = TransactionType.EXPENSE, color = "#2196F3", icon = "LocalGasStation", parentCategoryId = transportId),
            Category(name = "Parking", type = TransactionType.EXPENSE, color = "#2196F3", icon = "LocalParking", parentCategoryId = transportId)
        ))

        // Bills & Utilities (Orange)
        val billsId = categoryRepository.insertCategory(
            Category(name = "Bills & Utilities", type = TransactionType.EXPENSE, color = "#FF9800", icon = "Receipt")
        )
        categoryRepository.insertCategories(listOf(
            Category(name = "Electricity (KPLC)", type = TransactionType.EXPENSE, color = "#FF9800", icon = "ElectricBolt", parentCategoryId = billsId),
            Category(name = "Water", type = TransactionType.EXPENSE, color = "#FF9800", icon = "Water", parentCategoryId = billsId),
            Category(name = "Internet", type = TransactionType.EXPENSE, color = "#FF9800", icon = "Wifi", parentCategoryId = billsId),
            Category(name = "Airtime", type = TransactionType.EXPENSE, color = "#FF9800", icon = "Phone", parentCategoryId = billsId),
            Category(name = "Rent", type = TransactionType.EXPENSE, color = "#FF9800", icon = "Home", parentCategoryId = billsId)
        ))

        // Shopping (Purple)
        val shoppingId = categoryRepository.insertCategory(
            Category(name = "Shopping", type = TransactionType.EXPENSE, color = "#9C27B0", icon = "ShoppingBag")
        )
        categoryRepository.insertCategories(listOf(
            Category(name = "Clothing", type = TransactionType.EXPENSE, color = "#9C27B0", icon = "Checkroom", parentCategoryId = shoppingId),
            Category(name = "Electronics", type = TransactionType.EXPENSE, color = "#9C27B0", icon = "Devices", parentCategoryId = shoppingId),
            Category(name = "Personal Care", type = TransactionType.EXPENSE, color = "#9C27B0", icon = "Face", parentCategoryId = shoppingId),
            Category(name = "Household Items", type = TransactionType.EXPENSE, color = "#9C27B0", icon = "Weekend", parentCategoryId = shoppingId)
        ))

        // Health (Red)
        val healthId = categoryRepository.insertCategory(
            Category(name = "Health", type = TransactionType.EXPENSE, color = "#F44336", icon = "LocalHospital")
        )
        categoryRepository.insertCategories(listOf(
            Category(name = "Medical", type = TransactionType.EXPENSE, color = "#F44336", icon = "MedicalServices", parentCategoryId = healthId),
            Category(name = "Pharmacy", type = TransactionType.EXPENSE, color = "#F44336", icon = "LocalPharmacy", parentCategoryId = healthId),
            Category(name = "Insurance", type = TransactionType.EXPENSE, color = "#F44336", icon = "HealthAndSafety", parentCategoryId = healthId)
        ))

        // Entertainment (Pink)
        val entertainmentId = categoryRepository.insertCategory(
            Category(name = "Entertainment", type = TransactionType.EXPENSE, color = "#E91E63", icon = "Movie")
        )
        categoryRepository.insertCategories(listOf(
            Category(name = "Movies", type = TransactionType.EXPENSE, color = "#E91E63", icon = "Theaters", parentCategoryId = entertainmentId),
            Category(name = "Events", type = TransactionType.EXPENSE, color = "#E91E63", icon = "Event", parentCategoryId = entertainmentId),
            Category(name = "Hobbies", type = TransactionType.EXPENSE, color = "#E91E63", icon = "SportsEsports", parentCategoryId = entertainmentId),
            Category(name = "Sports", type = TransactionType.EXPENSE, color = "#E91E63", icon = "SportsSoccer", parentCategoryId = entertainmentId)
        ))

        // Education (Teal)
        val educationId = categoryRepository.insertCategory(
            Category(name = "Education", type = TransactionType.EXPENSE, color = "#009688", icon = "School")
        )
        categoryRepository.insertCategories(listOf(
            Category(name = "School Fees", type = TransactionType.EXPENSE, color = "#009688", icon = "AccountBalance", parentCategoryId = educationId),
            Category(name = "Books", type = TransactionType.EXPENSE, color = "#009688", icon = "MenuBook", parentCategoryId = educationId),
            Category(name = "Courses", type = TransactionType.EXPENSE, color = "#009688", icon = "Class", parentCategoryId = educationId),
            Category(name = "Stationery", type = TransactionType.EXPENSE, color = "#009688", icon = "Create", parentCategoryId = educationId)
        ))

        // Personal & Family (Deep Purple)
        val personalId = categoryRepository.insertCategory(
            Category(name = "Personal & Family", type = TransactionType.EXPENSE, color = "#673AB7", icon = "FamilyRestroom")
        )
        categoryRepository.insertCategories(listOf(
            Category(name = "Childcare", type = TransactionType.EXPENSE, color = "#673AB7", icon = "ChildCare", parentCategoryId = personalId),
            Category(name = "Gifts", type = TransactionType.EXPENSE, color = "#673AB7", icon = "CardGiftcard", parentCategoryId = personalId),
            Category(name = "Personal Development", type = TransactionType.EXPENSE, color = "#673AB7", icon = "Psychology", parentCategoryId = personalId)
        ))

        // INCOME CATEGORIES

        // Income (Green)
        val incomeId = categoryRepository.insertCategory(
            Category(name = "Income", type = TransactionType.INCOME, color = "#4CAF50", icon = "AccountBalance")
        )
        categoryRepository.insertCategories(listOf(
            Category(name = "Salary", type = TransactionType.INCOME, color = "#4CAF50", icon = "Payments", parentCategoryId = incomeId),
            Category(name = "Business", type = TransactionType.INCOME, color = "#4CAF50", icon = "BusinessCenter", parentCategoryId = incomeId),
            Category(name = "Freelance", type = TransactionType.INCOME, color = "#4CAF50", icon = "Work", parentCategoryId = incomeId),
            Category(name = "Gifts Received", type = TransactionType.INCOME, color = "#4CAF50", icon = "Redeem", parentCategoryId = incomeId),
            Category(name = "Investments", type = TransactionType.INCOME, color = "#4CAF50", icon = "TrendingUp", parentCategoryId = incomeId)
        ))

        // Other categories (Miscellaneous)
        categoryRepository.insertCategory(
            Category(name = "Other Expenses", type = TransactionType.EXPENSE, color = "#607D8B", icon = "MoreHoriz")
        )
        categoryRepository.insertCategory(
            Category(name = "Other Income", type = TransactionType.INCOME, color = "#607D8B", icon = "MoreHoriz")
        )
    }
}
