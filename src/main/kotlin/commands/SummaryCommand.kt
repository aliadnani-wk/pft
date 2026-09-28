package com.aliadnani.commands

import com.aliadnani.services.SummaryService
import java.time.LocalDateTime
import java.util.concurrent.Callable
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Option
import picocli.CommandLine.Spec

@Command(name = "summary", description = ["Show a summary of transactions"])
class SummaryCommand(private val summaryService: SummaryService) : Callable<Int> {

  @Spec lateinit var spec: CommandSpec

  private val out
    get() = spec.commandLine().out

  @Option(names = ["--before"], paramLabel = "TIME") var before: LocalDateTime? = null

  @Option(names = ["--after"], paramLabel = "TIME") var after: LocalDateTime? = null

  override fun call(): Int {
    val summary = summaryService.summarize(before = before, after = after)

    out.println("Date range: ${formatDateRange(summary.before, summary.after)}")
    out.println("Total spend: ${summary.totalSpend}")
    out.println("Total income: ${summary.totalIncome}")
    out.println("Net: ${summary.net()}")

    if (summary.spendByCategory.isEmpty()) {
      out.println("No spending to summarize.")
    } else {
      out.println("Spend by category:")
      summary.spendByCategory.forEach { (categoryName, totalSpend) ->
        out.println("  ${categoryName ?: "Uncategorized"}: $totalSpend")
      }
    }

    return 0
  }

  private fun formatDateRange(before: LocalDateTime?, after: LocalDateTime?): String =
      when {
        before == null && after == null -> "all time"
        after == null -> "before $before"
        before == null -> "after $after"
        else -> "$after to $before"
      }
}
