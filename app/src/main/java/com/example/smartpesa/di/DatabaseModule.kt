package com.example.smartpesa.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartpesa.data.local.dao.AccountDao
import com.example.smartpesa.data.local.dao.BudgetDao
import com.example.smartpesa.data.local.dao.CategoryDao
import com.example.smartpesa.data.local.dao.CategoryRuleDao
import com.example.smartpesa.data.local.dao.FulizaDao
import com.example.smartpesa.data.local.dao.LoanDao
import com.example.smartpesa.data.local.dao.MerchantCategoryHistoryDao
import com.example.smartpesa.data.local.dao.PayeeCategoryRuleDao
import com.example.smartpesa.data.local.dao.TransactionCostDao
import com.example.smartpesa.data.local.dao.TransactionDao
import com.example.smartpesa.data.local.database.SmartPesaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database and DAO instances
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSmartPesaDatabase(
        @ApplicationContext context: Context
    ): SmartPesaDatabase {
        return Room.databaseBuilder(
            context,
            SmartPesaDatabase::class.java,
            SmartPesaDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN mpesaMessage TEXT")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN accountName TEXT")
            db.execSQL("ALTER TABLE budgets ADD COLUMN accountName TEXT")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `accounts` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `openingBalance` REAL NOT NULL,
                    `phoneNumber` TEXT,
                    `isDefault` INTEGER NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_name` ON `accounts` (`name`)")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN currencyCode TEXT")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create category_rules table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `category_rules` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `categoryId` INTEGER NOT NULL,
                    `priority` INTEGER NOT NULL,
                    `enabled` INTEGER NOT NULL,
                    `merchantContains` TEXT,
                    `descriptionContains` TEXT,
                    `counterpartyEquals` TEXT,
                    `transactionTypeEquals` TEXT,
                    `amountGreaterThan` REAL,
                    `amountLessThan` REAL,
                    `accountNameEquals` TEXT,
                    `mpesaTypeContains` TEXT
                )
                """.trimIndent()
            )

            // Create merchant_category_history table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `merchant_category_history` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `normalizedMerchant` TEXT NOT NULL,
                    `categoryId` INTEGER NOT NULL,
                    `occurrenceCount` INTEGER NOT NULL,
                    `correctionCount` INTEGER NOT NULL,
                    `lastUsedAt` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL
                )
                """.trimIndent()
            )

            // Create indices for merchant_category_history
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_merchant_category_history_normalizedMerchant` ON `merchant_category_history` (`normalizedMerchant`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_merchant_category_history_categoryId` ON `merchant_category_history` (`categoryId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_merchant_category_history_normalizedMerchant_categoryId` ON `merchant_category_history` (`normalizedMerchant`, `categoryId`)")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add isWorkTransaction column for separating business/work transactions from personal
            db.execSQL("ALTER TABLE transactions ADD COLUMN isWorkTransaction INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create payee_category_rules table for auto-categorization
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `payee_category_rules` (
                    `payeeKey` TEXT PRIMARY KEY NOT NULL,
                    `categoryId` INTEGER NOT NULL,
                    `timesUsed` INTEGER NOT NULL,
                    `lastUsedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_payee_category_rules_categoryId` ON `payee_category_rules` (`categoryId`)")

            // Add isAutoCategorized column to track auto-categorized transactions
            db.execSQL("ALTER TABLE transactions ADD COLUMN isAutoCategorized INTEGER NOT NULL DEFAULT 0")
        }
    }


    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE fuliza RENAME TO fuliza_old")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `fuliza` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `currentBalance` REAL NOT NULL,
                    `availableLimit` REAL NOT NULL,
                    `totalAccessFees` REAL NOT NULL,
                    `dueDate` TEXT,
                    `repaymentHistory` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `fuliza` (
                    `id`, `currentBalance`, `availableLimit`, `totalAccessFees`, `dueDate`, `repaymentHistory`, `updatedAt`
                )
                SELECT
                    `id`,
                    `currentBalance`,
                    `availableLimit`,
                    COALESCE(`interestPaid`, 0.0) + COALESCE(`dailyCharges`, 0.0),
                    NULL,
                    `repaymentHistory`,
                    `updatedAt`
                FROM `fuliza_old`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE fuliza_old")
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE fuliza ADD COLUMN accessCharges TEXT NOT NULL DEFAULT '[]'")
        }
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: SmartPesaDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: SmartPesaDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideAccountDao(database: SmartPesaDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: SmartPesaDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideTransactionCostDao(database: SmartPesaDatabase): TransactionCostDao {
        return database.transactionCostDao()
    }

    @Provides
    @Singleton
    fun provideFulizaDao(database: SmartPesaDatabase): FulizaDao {
        return database.fulizaDao()
    }

    @Provides
    @Singleton
    fun provideLoanDao(database: SmartPesaDatabase): LoanDao {
        return database.loanDao()
    }

    @Provides
    @Singleton
    fun provideCategoryRuleDao(database: SmartPesaDatabase): CategoryRuleDao {
        return database.categoryRuleDao()
    }

    @Provides
    @Singleton
    fun provideMerchantCategoryHistoryDao(database: SmartPesaDatabase): MerchantCategoryHistoryDao {
        return database.merchantCategoryHistoryDao()
    }

    @Provides
    @Singleton
    fun providePayeeCategoryRuleDao(database: SmartPesaDatabase): PayeeCategoryRuleDao {
        return database.payeeCategoryRuleDao()
    }
}
