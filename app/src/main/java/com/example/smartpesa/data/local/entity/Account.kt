package com.example.smartpesa.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class AccountType {
    MPESA,
    BANK,
    CASH,
    OTHER
}

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["name"], unique = true)]
)
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val currencyCode: String? = null,
    val openingBalance: Double = 0.0,
    val phoneNumber: String? = null,
    val isDefault: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
