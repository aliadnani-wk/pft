package com.aliadnani.services

import com.aliadnani.model.NewTransaction
import com.aliadnani.model.Transaction
import com.aliadnani.model.request.BulkAddTransactionRequest
import com.aliadnani.repos.CategoriesRepo
import com.aliadnani.repos.TransactionsRepo
import java.math.BigDecimal
import java.time.LocalDateTime

class TransactionsService(
    private val transactionsRepo: TransactionsRepo,
    private val categoriesRepo: CategoriesRepo,
) {
  sealed interface CreateTransactionResult {
    data class Success(val transaction: Transaction) : CreateTransactionResult

    data object CategoryNotFound : CreateTransactionResult
  }

  fun addTransaction(
      transactionType: Transaction.TransactionType,
      amount: BigDecimal,
      description: String,
      transactionTime: LocalDateTime,
      categoryName: String?,
  ): CreateTransactionResult {
    val category = categoryName?.let { name ->
      categoriesRepo.getCategoryByName(name) ?: return CreateTransactionResult.CategoryNotFound
    }

    val id =
        transactionsRepo.addTransaction(
            transactionType = transactionType,
            amount = amount,
            description = description,
            transactionTime = transactionTime,
            categoryId = category?.id,
        )

    return CreateTransactionResult.Success(
        Transaction(
            id = id,
            transactionType = transactionType,
            amount = amount,
            description = description,
            timestamp = transactionTime,
            category = category,
        )
    )
  }

  sealed interface ImportTransactionsResult {
    data class Success(val transactionCount: Int) : ImportTransactionsResult

    data class CategoryNotFound(val rowNumber: Long, val categoryName: String) :
        ImportTransactionsResult
  }

  fun importTransactions(request: BulkAddTransactionRequest): ImportTransactionsResult {
    val now = LocalDateTime.now()
    val newTransactions = mutableListOf<NewTransaction>()

    for (item in request.transactions) {
      val category =
          item.categoryName?.let { name ->
            categoriesRepo.getCategoryByName(name)
                ?: return ImportTransactionsResult.CategoryNotFound(item.rowNumber, name)
          }

      newTransactions +=
          NewTransaction(
              transactionType = item.transactionType,
              amount = item.amount,
              description = item.description,
              timestamp = item.timestamp ?: now,
              categoryId = category?.id,
          )
    }

    transactionsRepo.addTransactions(newTransactions)

    return ImportTransactionsResult.Success(newTransactions.size)
  }

  sealed interface GetTransactionResult {
    data class Success(val transaction: Transaction) : GetTransactionResult

    data object NotFound : GetTransactionResult
  }

  fun getTransactionById(id: Int): GetTransactionResult =
      transactionsRepo.getTransactionById(id)?.let { GetTransactionResult.Success(it) }
          ?: GetTransactionResult.NotFound

  fun listTransactions(
      before: LocalDateTime?,
      after: LocalDateTime?,
      categoryNames: List<String>?,
  ): List<Transaction> = transactionsRepo.listTransactions(before, after, categoryNames)

  sealed interface EditTransactionResult {
    data class Success(val transaction: Transaction) : EditTransactionResult

    data object NotFound : EditTransactionResult

    data object CategoryNotFound : EditTransactionResult
  }

  fun editTransaction(
      id: Int,
      transactionType: Transaction.TransactionType?,
      amount: BigDecimal?,
      description: String?,
      transactionTime: LocalDateTime?,
      categoryName: String?,
      clearCategory: Boolean,
  ): EditTransactionResult {
    val existing = transactionsRepo.getTransactionById(id) ?: return EditTransactionResult.NotFound

    val newCategory =
        when {
          clearCategory -> null
          categoryName != null ->
              categoriesRepo.getCategoryByName(categoryName)
                  ?: return EditTransactionResult.CategoryNotFound
          else -> existing.category
        }

    val updated =
        existing.copy(
            transactionType = transactionType ?: existing.transactionType,
            amount = amount ?: existing.amount,
            description = description ?: existing.description,
            timestamp = transactionTime ?: existing.timestamp,
            category = newCategory,
        )

    transactionsRepo.editTransaction(
        id = id,
        transactionType = updated.transactionType,
        amount = updated.amount,
        description = updated.description,
        transactionTime = updated.timestamp,
        categoryId = updated.category?.id,
    )

    return EditTransactionResult.Success(updated)
  }

  sealed interface DeleteTransactionResult {
    data object Success : DeleteTransactionResult

    data object NotFound : DeleteTransactionResult
  }

  fun deleteTransaction(id: Int): DeleteTransactionResult =
      if (transactionsRepo.deleteTransaction(id) == 1) {
        DeleteTransactionResult.Success
      } else {
        DeleteTransactionResult.NotFound
      }
}
