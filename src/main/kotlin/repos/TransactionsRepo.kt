package com.aliadnani.repos

import com.aliadnani.model.NewTransaction
import com.aliadnani.model.Transaction
import java.math.BigDecimal
import java.time.LocalDateTime

interface TransactionsRepo {
  fun getTransactionById(id: Int): Transaction?

  fun listTransactions(
      before: LocalDateTime?,
      after: LocalDateTime?,
      categoryNames: List<String>?,
  ): List<Transaction>

  fun findTransactionsMadeOnWeekends(): List<Transaction>

  fun addTransaction(
      transactionType: Transaction.TransactionType,
      amount: BigDecimal,
      description: String,
      transactionTime: LocalDateTime,
      categoryId: Int?,
  ): Int

  fun addTransactions(transactions: List<NewTransaction>)

  fun editTransaction(
      id: Int,
      transactionType: Transaction.TransactionType,
      amount: BigDecimal,
      description: String,
      transactionTime: LocalDateTime,
      categoryId: Int?,
  )

  fun deleteTransaction(id: Int): Int
}
