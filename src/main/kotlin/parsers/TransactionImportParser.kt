package com.aliadnani.parsers

import com.aliadnani.model.Transaction.TransactionType
import com.aliadnani.model.isRepresentableInCents
import com.aliadnani.model.request.BulkAddTransactionRequest
import com.aliadnani.model.request.BulkAddTransactionRequestItem
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.io.IOException
import java.io.Reader
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.Locale

enum class TransactionImportFormat(val delimiter: Char) {
  CSV(','),
  TSV('\t'),
}

data class TransactionImportParseError(
    val rowNumber: Long?,
    val field: String?,
    val message: String,
)

sealed interface TransactionImportParseResult {
  data class Success(val request: BulkAddTransactionRequest) : TransactionImportParseResult

  data class Invalid(val error: TransactionImportParseError) : TransactionImportParseResult
}

class TransactionImportParser {
  private val mapper = CsvMapper()

  fun parse(reader: Reader, format: TransactionImportFormat): TransactionImportParseResult =
      try {
        val schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(format.delimiter)
        mapper
            .readerFor(RawTransactionRow::class.java)
            .with(schema)
            .readValues<RawTransactionRow>(reader)
            .use(::parseRows)
      } catch (exception: IOException) {
        invalidFile(exception.message ?: "Unable to read delimited file")
      }

  private fun parseRows(rows: Iterator<RawTransactionRow>): TransactionImportParseResult {
    val transactions = mutableListOf<BulkAddTransactionRequestItem>()
    var rowNumber = 1L

    for (row in rows) {
      rowNumber++
      when (val result = parseRow(row, rowNumber)) {
        is RowResult.Success -> transactions += result.transaction
        is RowResult.Invalid -> return TransactionImportParseResult.Invalid(result.error)
      }
    }

    if (transactions.isEmpty()) {
      return invalidFile("File contains no transaction rows")
    }
    return TransactionImportParseResult.Success(BulkAddTransactionRequest(transactions))
  }

  private fun parseRow(row: RawTransactionRow, rowNumber: Long): RowResult {
    val typeValue =
        row.type?.trim()?.takeIf(String::isNotBlank)
            ?: return rowError(rowNumber, "type", "Transaction type is required")
    val type =
        runCatching { TransactionType.valueOf(typeValue.uppercase(Locale.ROOT)) }.getOrNull()
            ?: return rowError(
                rowNumber,
                "type",
                "Expected INCOME or EXPENSE but found '$typeValue'",
            )

    val amountValue =
        row.amount?.trim()?.takeIf(String::isNotBlank)
            ?: return rowError(rowNumber, "amount", "Amount is required")
    val amount =
        runCatching { BigDecimal(amountValue) }.getOrNull()
            ?: return rowError(rowNumber, "amount", "Invalid amount '$amountValue'")
    if (amount < BigDecimal.ZERO) {
      return rowError(rowNumber, "amount", "Amount must be non-negative")
    }
    if (!isRepresentableInCents(amount)) {
      return rowError(rowNumber, "amount", "Amount must be representable to two decimal places")
    }

    val description =
        row.description?.trim()?.takeIf(String::isNotBlank)
            ?: return rowError(rowNumber, "description", "Description is required")
    val timestampValue = row.timestamp?.trim()?.takeIf(String::isNotBlank)
    val timestamp =
        try {
          timestampValue?.let(LocalDateTime::parse)
        } catch (exception: DateTimeParseException) {
          return rowError(
              rowNumber,
              "timestamp",
              "Invalid timestamp '$timestampValue'; expected ISO-8601 local date-time",
          )
        }

    return RowResult.Success(
        BulkAddTransactionRequestItem(
            rowNumber = rowNumber,
            transactionType = type,
            amount = amount,
            description = description,
            timestamp = timestamp,
            categoryName = row.category?.trim()?.takeIf(String::isNotBlank),
        )
    )
  }

  private fun rowError(rowNumber: Long, field: String, message: String) =
      RowResult.Invalid(TransactionImportParseError(rowNumber, field, message))

  private fun invalidFile(message: String) =
      TransactionImportParseResult.Invalid(
          TransactionImportParseError(rowNumber = null, field = null, message = message)
      )

  private sealed interface RowResult {
    data class Success(val transaction: BulkAddTransactionRequestItem) : RowResult

    data class Invalid(val error: TransactionImportParseError) : RowResult
  }
}

private data class RawTransactionRow(
    var type: String? = null,
    var amount: String? = null,
    var description: String? = null,
    var timestamp: String? = null,
    var category: String? = null,
)
