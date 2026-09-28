package com.aliadnani.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Transaction(
    val id: Int,
    val transactionType: TransactionType,
    val amount: BigDecimal,
    val description: String,
    val timestamp: LocalDateTime,
    val category: Category?,
) {
  enum class TransactionType {
    INCOME,
    EXPENSE,
  }
}

data class NewTransaction(
    val transactionType: Transaction.TransactionType,
    val amount: BigDecimal,
    val description: String,
    val timestamp: LocalDateTime,
    val categoryId: Int?,
)
