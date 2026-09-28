package com.aliadnani.parsers

import com.aliadnani.model.Transaction.TransactionType
import java.io.StringReader
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TransactionImportParserTest {
  private val parser = TransactionImportParser()

  @Test
  fun `parses a valid csv row into a transaction request`() {
    val csv =
        """
        type,amount,description,timestamp,category
        EXPENSE,12.50,Lunch,2024-01-06T12:00:00,Food
        """
            .trimIndent()

    val result = parser.parse(StringReader(csv), TransactionImportFormat.CSV)

    assertIs<TransactionImportParseResult.Success>(result)
    val transaction = result.request.transactions.single()
    assertEquals(TransactionType.EXPENSE, transaction.transactionType)
    assertEquals(BigDecimal("12.50"), transaction.amount)
    assertEquals("Lunch", transaction.description)
    assertEquals("Food", transaction.categoryName)
  }

  @Test
  fun `reports the row and field of an invalid row`() {
    val csv =
        """
        type,amount,description,timestamp,category
        EXPENSE,not-a-number,Lunch,2024-01-06T12:00:00,Food
        """
            .trimIndent()

    val result = parser.parse(StringReader(csv), TransactionImportFormat.CSV)

    assertIs<TransactionImportParseResult.Invalid>(result)
    assertEquals(2L, result.error.rowNumber)
    assertEquals("amount", result.error.field)
  }

  @Test
  fun `rejects amounts that cannot be represented in cents`() {
    val csv =
        """
        type,amount,description,timestamp,category
        EXPENSE,1.001,Lunch,2024-01-06T12:00:00,Food
        """
            .trimIndent()

    val result = parser.parse(StringReader(csv), TransactionImportFormat.CSV)

    assertIs<TransactionImportParseResult.Invalid>(result)
    assertEquals(2L, result.error.rowNumber)
    assertEquals("amount", result.error.field)
    assertEquals("Amount must be representable to two decimal places", result.error.message)
  }
}
