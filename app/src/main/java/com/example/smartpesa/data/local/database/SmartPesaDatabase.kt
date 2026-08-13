package com.example.smartpesa.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.smartpesa.data.local.dao.BudgetDao
import com.example.smartpesa.data.local.dao.AccountDao
import com.example.smartpesa.data.local.dao.CategoryDao
import com.example.smartpesa.data.local.dao.CategoryRuleDao
import com.example.smartpesa.data.local.dao.FulizaDao
import com.example.smartpesa.data.local.dao.LoanDao
import com.example.smartpesa.data.local.dao.MerchantCategoryHistoryDao
import com.example.smartpesa.data.local.dao.PayeeCategoryRuleDao
import com.example.smartpesa.data.local.dao.TransactionCostDao
import com.example.smartpesa.data.local.dao.TransactionDao
import com.example.smartpesa.data.local.entity.Budget
import com.example.smartpesa.data.local.entity.Account
import com.example.smartpesa.data.local.entity.Category
import com.example.smartpesa.data.local.entity.CategoryRule
import com.example.smartpesa.data.local.entity.Fuliza
import com.example.smartpesa.data.local.entity.FulizaAccessCharge
import com.example.smartpesa.data.local.entity.FulizaRepayment
import com.example.smartpesa.data.local.entity.Loan
import com.example.smartpesa.data.local.entity.LoanPayment
import com.example.smartpesa.data.local.entity.MerchantCategoryHistory
import com.example.smartpesa.data.local.entity.PayeeCategoryRule
import com.example.smartpesa.data.local.entity.RecurringConfig
import com.example.smartpesa.data.local.entity.TransactionCost
import com.example.smartpesa.data.local.entity.Transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

/**
 * SmartPesa Room Database
 * Includes TypeConverters for LocalDateTime
 */
@Database(
    entities = [
        Transaction::class,
        Category::class,
        Budget::class,
        TransactionCost::class,
        Fuliza::class,
        Loan::class,
        Account::class,
        CategoryRule::class,
        MerchantCategoryHistory::class,
        PayeeCategoryRule::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SmartPesaDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionCostDao(): TransactionCostDao
    abstract fun fulizaDao(): FulizaDao
    abstract fun loanDao(): LoanDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun merchantCategoryHistoryDao(): MerchantCategoryHistoryDao
    abstract fun payeeCategoryRuleDao(): PayeeCategoryRuleDao

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

    @TypeConverter
    fun fromRecurringConfig(value: RecurringConfig?): String? {
        return value?.let {
            JSONObject().apply {
                put("frequency", it.frequency)
                put("interval", it.interval)
                put("nextRunAt", it.nextRunAt?.format(formatter))
            }.toString()
        }
    }

    @TypeConverter
    fun toRecurringConfig(value: String?): RecurringConfig? {
        if (value.isNullOrBlank()) return null
        val json = JSONObject(value)
        val nextRunAt = if (json.isNull("nextRunAt")) null else json.optString("nextRunAt").takeIf { it.isNotBlank() }?.let {
            LocalDateTime.parse(it, formatter)
        }
        return RecurringConfig(
            frequency = json.optString("frequency", "monthly"),
            interval = json.optInt("interval", 1),
            nextRunAt = nextRunAt
        )
    }

    @TypeConverter
    fun fromLoanPayments(value: List<LoanPayment>?): String? {
        return value?.let {
            JSONArray().apply {
                it.forEach { payment ->
                    put(JSONObject().apply {
                        put("amount", payment.amount)
                        put("timestamp", payment.timestamp.format(formatter))
                        put("transactionId", payment.transactionId)
                        put("note", payment.note)
                    })
                }
            }.toString()
        }
    }

    @TypeConverter
    fun toLoanPayments(value: String?): List<LoanPayment> {
        if (value.isNullOrBlank()) return emptyList()
        val jsonArray = JSONArray(value)
        return buildList {
            for (index in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(index)
                add(
                    LoanPayment(
                        amount = json.getDouble("amount"),
                        timestamp = LocalDateTime.parse(json.getString("timestamp"), formatter),
                        transactionId = if (json.isNull("transactionId")) null else json.optLong("transactionId"),
                        note = json.optString("note", "")
                    )
                )
            }
        }
    }

    @TypeConverter
    fun fromFulizaRepayments(value: List<FulizaRepayment>?): String? {
        return value?.let {
            JSONArray().apply {
                it.forEach { repayment ->
                    put(JSONObject().apply {
                        put("amount", repayment.amount)
                        put("timestamp", repayment.timestamp.format(formatter))
                        put("transactionId", repayment.transactionId)
                    })
                }
            }.toString()
        }
    }

    @TypeConverter
    fun toFulizaRepayments(value: String?): List<FulizaRepayment> {
        if (value.isNullOrBlank()) return emptyList()
        val jsonArray = JSONArray(value)
        return buildList {
            for (index in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(index)
                add(
                    FulizaRepayment(
                        amount = json.getDouble("amount"),
                        timestamp = LocalDateTime.parse(json.getString("timestamp"), formatter),
                        transactionId = if (json.isNull("transactionId")) null else json.optLong("transactionId")
                    )
                )
            }
        }
    }

    @TypeConverter
    fun fromFulizaAccessCharges(value: List<FulizaAccessCharge>?): String? {
        return value?.let {
            JSONArray().apply {
                it.forEach { charge ->
                    put(JSONObject().apply {
                        put("amount", charge.amount)
                        put("timestamp", charge.timestamp.format(formatter))
                        put("transactionId", charge.transactionId)
                    })
                }
            }.toString()
        }
    }

    @TypeConverter
    fun toFulizaAccessCharges(value: String?): List<FulizaAccessCharge> {
        if (value.isNullOrBlank()) return emptyList()
        val jsonArray = JSONArray(value)
        return buildList {
            for (index in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(index)
                add(
                    FulizaAccessCharge(
                        amount = json.getDouble("amount"),
                        timestamp = LocalDateTime.parse(json.getString("timestamp"), formatter),
                        transactionId = if (json.isNull("transactionId")) null else json.optLong("transactionId")
                    )
                )
            }
        }
    }
}
