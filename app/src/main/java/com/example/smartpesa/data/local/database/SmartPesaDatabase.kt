package com.example.smartpesa.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.smartpesa.data.local.dao.BudgetDao
import com.example.smartpesa.data.local.dao.CategoryDao
import com.example.smartpesa.data.local.dao.TransactionDao
import com.example.smartpesa.data.local.entity.Budget
import com.example.smartpesa.data.local.entity.Category
import com.example.smartpesa.data.local.entity.Transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * SmartPesa Room Database
 * Includes TypeConverters for LocalDateTime
 */
@Database(
    entities = [
        Transaction::class,
        Category::class,
        Budget::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SmartPesaDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        const val DATABASE_NAME = "smartpesa_db"
    }
}

/**
 * Type converters for Room database
 * Handles LocalDateTime <-> String conversion
 */
class Converters {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let {
            LocalDateTime.parse(it, formatter)
        }
    }
}
