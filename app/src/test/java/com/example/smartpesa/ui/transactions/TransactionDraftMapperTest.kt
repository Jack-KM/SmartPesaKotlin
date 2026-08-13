package com.example.smartpesa.ui.transactions

import com.example.smartpesa.data.local.entity.Transaction
import com.example.smartpesa.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TransactionDraftMapperTest {

    @Test
    fun `transaction to draft maps edit fields`() {
        val timestamp = LocalDateTime.of(2026, 8, 3, 14, 25)
        val transaction = Transaction(
            id = 7,
            amount = 1234.5,
            feeAmount = 12.0,
            description = "Lunch\nWith team",
            type = TransactionType.EXPENSE,
            timestamp = timestamp,
            categoryId = 3,
            category = "Food",
            counterparty = "M-Pesa",
            mpesaMessage = "SMS body",
            source = "Manual"
        )

        val draft = transaction.toDraft()

        assertEquals(AddTransactionTab.Expense, draft.tab)
        assertEquals("Food", draft.category)
        assertEquals("1234.5", draft.amount)
        assertEquals("12.0", draft.cost)
        assertEquals("Lunch", draft.title)
        assertEquals("With team", draft.description)
        assertEquals("SMS body", draft.mpesaMessage)
        assertEquals("M-Pesa", draft.expenseAccount)
        assertEquals("M-Pesa", draft.incomeAccount)
        assertEquals(timestamp.toLocalDate(), draft.selectedDate)
        assertEquals(timestamp.toLocalTime().withSecond(0).withNano(0), draft.selectedTime)
    }

    @Test
    fun `transaction to draft maps transfer accounts`() {
        val transaction = Transaction(
            id = 8,
            amount = 200.0,
            feeAmount = 0.0,
            description = "Transfer",
            type = TransactionType.EXPENSE,
            timestamp = LocalDateTime.of(2026, 8, 3, 9, 0),
            categoryId = null,
            category = "Transfer",
            counterparty = "M-Pesa → Cash",
            source = "Manual"
        )

        val draft = transaction.toDraft()

        assertEquals(AddTransactionTab.Transfer, draft.tab)
        assertEquals("M-Pesa", draft.transferFrom)
        assertEquals("Cash", draft.transferTo)
    }
}
