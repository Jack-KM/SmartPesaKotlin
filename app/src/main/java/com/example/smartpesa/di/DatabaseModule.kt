package com.example.smartpesa.di

import android.content.Context
import androidx.room.Room
import com.example.smartpesa.data.local.dao.BudgetDao
import com.example.smartpesa.data.local.dao.CategoryDao
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
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
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
    fun provideBudgetDao(database: SmartPesaDatabase): BudgetDao {
        return database.budgetDao()
    }
}
