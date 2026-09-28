package com.aliadnani.model.request

import com.aliadnani.model.Transaction.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

data class BulkAddTransactionRequest(val transactions: List<BulkAddTransactionRequestItem>)

data class BulkAddTransactionRequestItem(
    val rowNumber: Long,
    val transactionType: TransactionType,
    val amount: BigDecimal,
    val description: String,
    val timestamp: LocalDateTime?,
    val categoryName: String?,
)
