package com.aliadnani.commands

import com.aliadnani.CommandFailedException
import com.aliadnani.model.Transaction.TransactionType
import com.aliadnani.model.isRepresentableInCents
import com.aliadnani.parsers.TransactionImportFormat
import com.aliadnani.parsers.TransactionImportParseResult
import com.aliadnani.parsers.TransactionImportParser
import com.aliadnani.services.TransactionsService
import java.io.IOException
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import picocli.CommandLine.ArgGroup
import picocli.CommandLine.Command
import picocli.CommandLine.ITypeConverter
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import picocli.CommandLine.Spec
import picocli.CommandLine.TypeConversionException

@Command(name = "transactions", description = ["Manage transactions"])
class TransactionsCommand(
    private val transactionsService: TransactionsService,
    private val parser: TransactionImportParser,
) {

  @Spec lateinit var spec: CommandSpec

  private val out
    get() = spec.commandLine().out

  class AmountConverter : ITypeConverter<BigDecimal> {
    override fun convert(value: String): BigDecimal {
      val amount = value.toBigDecimalOrNull()
      return if (amount == null || amount < BigDecimal.ZERO || !isRepresentableInCents(amount)) {
        throw TypeConversionException(
            "'$value' must be a non-negative amount representable to two decimal places"
        )
      } else {
        amount
      }
    }
  }

  @Command(name = "add", description = ["Add a new transaction"])
  fun addTransaction(
      @Option(
          names = ["--amount"],
          required = true,
          paramLabel = "AMOUNT",
          converter = [AmountConverter::class],
      )
      amount: BigDecimal,
      @Option(names = ["--type"], required = true, paramLabel = "TYPE") type: TransactionType,
      @Option(names = ["--description"], required = true, paramLabel = "TEXT") description: String,
      @Option(names = ["--category"], paramLabel = "NAME") categoryName: String?,
      @Option(names = ["--timestamp"], paramLabel = "TIME") timestamp: LocalDateTime?,
  ) {
    transactionsService
        .addTransaction(
            transactionType = type,
            amount = amount,
            description = description,
            transactionTime = timestamp ?: LocalDateTime.now(),
            categoryName = categoryName,
        )
        .let { result ->
          when (result) {
            is TransactionsService.CreateTransactionResult.Success ->
                out.println("Transaction added: ${result.transaction}")

            TransactionsService.CreateTransactionResult.CategoryNotFound ->
                throw CommandFailedException("Category not found: $categoryName")
          }
        }
  }

  @Command(name = "import", description = ["Import transactions from a CSV or TSV file"])
  fun importTransactions(
      @Option(names = ["--file"], required = true, paramLabel = "PATH") file: Path,
      @Option(names = ["--format"], required = true, paramLabel = "FORMAT")
      format: TransactionImportFormat,
  ) {
    val parseResult =
        try {
          Files.newBufferedReader(file).use { reader -> parser.parse(reader, format) }
        } catch (exception: IOException) {
          throw ParameterException(spec.commandLine(), "Unable to read file: $file")
        }

    when (parseResult) {
      is TransactionImportParseResult.Invalid -> {
        val error = parseResult.error
        val location = error.rowNumber?.let { "Row $it" } ?: "File"
        val field = error.field?.let { " ($it)" } ?: ""
        throw CommandFailedException("Import failed: $location$field: ${error.message}")
      }

      is TransactionImportParseResult.Success ->
          when (val result = transactionsService.importTransactions(parseResult.request)) {
            is TransactionsService.ImportTransactionsResult.Success ->
                out.println("Imported ${result.transactionCount} transaction(s).")

            is TransactionsService.ImportTransactionsResult.CategoryNotFound ->
                throw CommandFailedException(
                    "Import failed: Row ${result.rowNumber}: category not found: ${result.categoryName}"
                )
          }
    }
  }

  @Command(name = "get", description = ["Get a transaction by id"])
  fun getTransactionById(
      @Option(names = ["--id"], required = true, paramLabel = "ID") id: Int,
  ) {
    when (val result = transactionsService.getTransactionById(id)) {
      is TransactionsService.GetTransactionResult.Success ->
          out.println("Transaction: ${result.transaction}")

      TransactionsService.GetTransactionResult.NotFound ->
          throw CommandFailedException("Transaction not found: $id")
    }
  }

  class CategoryOption {
    @Option(names = ["--category"], paramLabel = "NAME") var categoryName: String? = null

    @Option(
        names = ["--clear-category"],
        description = ["Remove the transaction's category"],
    )
    var clearCategory: Boolean = false
  }

  @Command(name = "edit", description = ["Edit an existing transaction"])
  fun editTransaction(
      @Option(names = ["--id"], required = true, paramLabel = "ID") id: Int,
      @Option(names = ["--type"], paramLabel = "TYPE") transactionType: TransactionType?,
      @Option(
          names = ["--amount"],
          paramLabel = "AMOUNT",
          converter = [AmountConverter::class],
      )
      amount: BigDecimal?,
      @Option(names = ["--description"], paramLabel = "TEXT") description: String?,
      @Option(names = ["--timestamp"], paramLabel = "TIME") timestamp: LocalDateTime?,
      @ArgGroup(exclusive = true) categoryOption: CategoryOption?,
  ) {
    val categoryName = categoryOption?.categoryName
    val clearCategory = categoryOption?.clearCategory ?: false

    val result =
        transactionsService.editTransaction(
            id = id,
            transactionType = transactionType,
            amount = amount,
            description = description,
            transactionTime = timestamp,
            categoryName = categoryName,
            clearCategory = clearCategory,
        )

    when (result) {
      is TransactionsService.EditTransactionResult.Success ->
          out.println("Transaction updated: ${result.transaction}")

      TransactionsService.EditTransactionResult.NotFound ->
          throw CommandFailedException("Transaction not found: $id")

      TransactionsService.EditTransactionResult.CategoryNotFound ->
          throw CommandFailedException("Category not found: $categoryName")
    }
  }

  @Command(name = "delete", description = ["Delete a transaction by id"])
  fun deleteTransaction(
      @Option(names = ["--id"], required = true, paramLabel = "ID") id: Int,
  ) {
    when (transactionsService.deleteTransaction(id)) {
      TransactionsService.DeleteTransactionResult.Success -> out.println("Transaction deleted: $id")

      TransactionsService.DeleteTransactionResult.NotFound ->
          throw CommandFailedException("Transaction not found: $id")
    }
  }

  @Command(name = "list", description = ["List all transactions"])
  fun listTransactions(
      @Option(names = ["--before"], paramLabel = "TIME") beforeTimestampFilter: LocalDateTime?,
      @Option(names = ["--after"], paramLabel = "TIME") afterTimestampFilter: LocalDateTime?,
      @Option(names = ["--category"], paramLabel = "NAME") categoryNameFilter: List<String>?,
  ) {
    val transactions =
        transactionsService.listTransactions(
            before = beforeTimestampFilter,
            after = afterTimestampFilter,
            categoryNames = categoryNameFilter,
        )

    if (transactions.isEmpty()) {
      out.println("No transactions found.")
    } else {
      transactions.forEach { out.println(it) }
    }
  }
}
