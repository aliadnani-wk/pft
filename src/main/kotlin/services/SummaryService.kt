package com.aliadnani.services

import com.aliadnani.model.CategorySpend
import com.aliadnani.model.MAX_AMOUNT_DECIMAL_PLACES
import com.aliadnani.model.Summary
import com.aliadnani.model.Transaction
import com.aliadnani.model.Transaction.TransactionType
import com.aliadnani.repos.TransactionsRepo
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

class SummaryService(private val transactionsRepo: TransactionsRepo) {

  fun summarize(before: LocalDateTime?, after: LocalDateTime?): Summary {
    val transactions =
        transactionsRepo.listTransactions(before = before, after = after, categoryNames = null)

    val spend = transactions.filter { it.transactionType == TransactionType.EXPENSE }
    val income = transactions.filter { it.transactionType == TransactionType.INCOME }

    val spendByCategory =
        spend
            .groupBy { it.category?.name }
            .map { (categoryName, categoryTransactions) ->
              CategorySpend(categoryName, totalOf(categoryTransactions))
            }
            .sortedByDescending { it.totalSpend }

    return Summary(
        before = before,
        after = after,
        totalSpend = totalOf(spend),
        totalIncome = totalOf(income),
        spendByCategory = spendByCategory,
    )
  }

  private fun totalOf(transactions: List<Transaction>): BigDecimal =
      transactions
          .fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount }
          .setScale(MAX_AMOUNT_DECIMAL_PLACES, RoundingMode.HALF_UP)
}
