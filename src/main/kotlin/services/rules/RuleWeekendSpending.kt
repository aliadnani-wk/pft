package com.aliadnani.services.rules

import com.aliadnani.model.Transaction
import com.aliadnani.model.Transaction.TransactionType
import com.aliadnani.repos.TransactionsRepo

class RuleWeekendSpending(private val transactionsRepo: TransactionsRepo) : Rule {
  override val ruleName: String = "weekend-spending"

  override fun execute(): List<Transaction> {
    // A repo method per rule is not ideal - but let's keep things simple for now.
    return transactionsRepo.findTransactionsMadeOnWeekends().filter {
      it.transactionType == TransactionType.EXPENSE
    }
  }
}
