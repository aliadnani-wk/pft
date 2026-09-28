package com.aliadnani.cli

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class PftCliTest {
  private val cli = PftCli()

  @AfterTest fun tearDown() = cli.cleanup()

  @Test
  fun `records income and expenses and summarizes by category`() {
    val addCategory = cli.run("categories", "add", "--name", "Food")
    assertEquals(0, addCategory.exitCode, addCategory.output)
    assertContains(addCategory.output, "Category added", message = addCategory.output)

    val addExpense =
        cli.run(
            "transactions",
            "add",
            "--amount",
            "12.50",
            "--type",
            "EXPENSE",
            "--description",
            "Lunch",
            "--category",
            "Food",
        )
    assertEquals(0, addExpense.exitCode, addExpense.output)

    val addIncome =
        cli.run(
            "transactions",
            "add",
            "--amount",
            "100",
            "--type",
            "INCOME",
            "--description",
            "Salary",
        )
    assertEquals(0, addIncome.exitCode, addIncome.output)

    val summary = cli.run("summary")
    assertEquals(0, summary.exitCode, summary.output)
    assertContains(summary.output, "Total spend: 12.50", message = summary.output)
    assertContains(summary.output, "Total income: 100", message = summary.output)
    assertContains(summary.output, "Food: 12.50", message = summary.output)
  }

  @Test
  fun `categorizing an existing transaction moves it out of uncategorized`() {
    val addCategory = cli.run("categories", "add", "--name", "Rent")
    assertEquals(0, addCategory.exitCode, addCategory.output)

    val addExpense =
        cli.run(
            "transactions",
            "add",
            "--amount",
            "500",
            "--type",
            "EXPENSE",
            "--description",
            "Rent",
        )
    assertEquals(0, addExpense.exitCode, addExpense.output)

    val beforeSummary = cli.run("summary")
    assertEquals(0, beforeSummary.exitCode, beforeSummary.output)
    assertContains(beforeSummary.output, "Uncategorized: 500", message = beforeSummary.output)

    val edit = cli.run("transactions", "edit", "--id", "1", "--category", "Rent")
    assertEquals(0, edit.exitCode, edit.output)

    val afterSummary = cli.run("summary")
    assertEquals(0, afterSummary.exitCode, afterSummary.output)
    assertContains(afterSummary.output, "Rent: 500", message = afterSummary.output)
    assertFalse(afterSummary.output.contains("Uncategorized"), afterSummary.output)
  }

  @Test
  fun `bulk import inserts multiple transactions`() {
    val addCategory = cli.run("categories", "add", "--name", "Food")
    assertEquals(0, addCategory.exitCode, addCategory.output)

    val csv = File(javaClass.classLoader.getResource("transactions.csv")!!.toURI())

    val importResult =
        cli.run("transactions", "import", "--file", csv.absolutePath, "--format", "CSV")
    assertEquals(0, importResult.exitCode, importResult.output)
    assertContains(importResult.output, "Imported 2 transaction(s).", message = importResult.output)

    val list = cli.run("transactions", "list")
    assertEquals(0, list.exitCode, list.output)
    assertContains(list.output, "Breakfast", message = list.output)
    assertContains(list.output, "Refund", message = list.output)

    val summary = cli.run("summary")
    assertEquals(0, summary.exitCode, summary.output)
    assertContains(summary.output, "Total spend: 1.50", message = summary.output)
    assertContains(summary.output, "Total income: 99", message = summary.output)
  }

  @Test
  fun `import with unknown category is atomic`() {
    val addCategory = cli.run("categories", "add", "--name", "Food")
    assertEquals(0, addCategory.exitCode, addCategory.output)

    val csv =
        cli.writeFile(
            "transactions.csv",
            """
            type,amount,description,timestamp,category
            EXPENSE,10,AtomicGood,2024-03-03T12:00:00,Food
            EXPENSE,20,AtomicBad,2024-03-03T12:00:00,Ghost
            """
                .trimIndent(),
        )

    val importResult =
        cli.run("transactions", "import", "--file", csv.absolutePath, "--format", "CSV")
    assertNotEquals(0, importResult.exitCode, importResult.output)
    assertContains(importResult.output, "Ghost", message = importResult.output)

    val list = cli.run("transactions", "list")
    assertEquals(0, list.exitCode, list.output)
    assertFalse(list.output.contains("AtomicGood"), list.output)
  }

  @Test
  fun `weekend rule flags only weekend transactions`() {
    val addSaturday =
        cli.run(
            "transactions",
            "add",
            "--amount",
            "5",
            "--type",
            "EXPENSE",
            "--description",
            "Sat",
            "--timestamp",
            "2024-01-06T12:00:00",
        )
    assertEquals(0, addSaturday.exitCode, addSaturday.output)

    val addMonday =
        cli.run(
            "transactions",
            "add",
            "--amount",
            "5",
            "--type",
            "EXPENSE",
            "--description",
            "Mon",
            "--timestamp",
            "2024-01-08T12:00:00",
        )
    assertEquals(0, addMonday.exitCode, addMonday.output)

    val flagResult = cli.run("flag", "--rule", "weekend-spending")
    assertEquals(0, flagResult.exitCode, flagResult.output)
    assertContains(flagResult.output, "description=Sat", message = flagResult.output)
    assertFalse(flagResult.output.contains("description=Mon"), flagResult.output)
  }

  @Test
  fun `negative amount is rejected`() {
    val result =
        cli.run(
            "transactions",
            "add",
            "--amount",
            "-5",
            "--type",
            "EXPENSE",
            "--description",
            "Bad",
        )
    assertNotEquals(0, result.exitCode, result.output)
    assertContains(result.output, "non-negative", message = result.output)
  }

  @Test
  fun `amounts that aren't in cents are rejected`() {
    val invalidAmount =
        cli.run(
            "transactions",
            "add",
            "--amount",
            "1.001",
            "--type",
            "EXPENSE",
            "--description",
            "Fractional cent",
        )
    assertNotEquals(0, invalidAmount.exitCode, invalidAmount.output)
    assertContains(invalidAmount.output, "two decimal places", message = invalidAmount.output)

    val amountWithTrailingZeros =
        cli.run(
            "transactions",
            "add",
            "--amount",
            "1.2300",
            "--type",
            "EXPENSE",
            "--description",
            "Valid cents",
        )
    assertEquals(0, amountWithTrailingZeros.exitCode, amountWithTrailingZeros.output)

    val summary = cli.run("summary")
    assertContains(summary.output, "Total spend: 1.23", message = summary.output)
  }

  @Test
  fun `help exits successfully`() {
    val result = cli.run("--help")
    assertEquals(0, result.exitCode, result.output)
    assertContains(result.output, "Usage:", message = result.output)
  }
}
