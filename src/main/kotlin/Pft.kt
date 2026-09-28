package com.aliadnani

import com.aliadnani.commands.CategoriesCommand
import com.aliadnani.commands.FlagCommand
import com.aliadnani.commands.SummaryCommand
import com.aliadnani.commands.TransactionsCommand
import com.aliadnani.parsers.TransactionImportParser
import com.aliadnani.repos.CategoriesRepoSqlite
import com.aliadnani.repos.TransactionsRepoSqlite
import com.aliadnani.services.CategoriesService
import com.aliadnani.services.RulesService
import com.aliadnani.services.SummaryService
import com.aliadnani.services.TransactionsService
import com.aliadnani.services.rules.RuleWeekendSpending
import com.aliadnani.storage.Sqlite
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.IFactory
import picocli.CommandLine.ScopeType

@Command(
    name = "pft",
    description = ["A simple personal finance tracker (PFT) CLI application."],
    version = ["pft 0.1.0"],
    mixinStandardHelpOptions = true,
    subcommands =
        [
            TransactionsCommand::class,
            CategoriesCommand::class,
            SummaryCommand::class,
            FlagCommand::class,
        ],
    scope = ScopeType.INHERIT,
)
class Pft {
  class Factory(private val dbPath: String = "pft.db") : IFactory {
    private val fallback = CommandLine.defaultFactory()

    private val sqlite by lazy { Sqlite(dbPath) }
    private val categoriesRepo by lazy { CategoriesRepoSqlite(sqlite) }
    private val transactionsRepo by lazy { TransactionsRepoSqlite(sqlite) }

    private val rulesService by lazy { RulesService(listOf(RuleWeekendSpending(transactionsRepo))) }
    private val summaryService by lazy { SummaryService(transactionsRepo) }

    // Main wiring done here. See for how picocli apps are configured:
    // - https://picocli.info/#_custom_factory
    // - https://picocli.info/apidocs/picocli/CommandLine.IFactory.html
    @Suppress("UNCHECKED_CAST")
    override fun <K : Any> create(cls: Class<K>): K =
        when (cls.kotlin) {
          CategoriesCommand::class -> CategoriesCommand(CategoriesService(categoriesRepo))
          TransactionsCommand::class ->
              TransactionsCommand(
                  TransactionsService(transactionsRepo, categoriesRepo),
                  TransactionImportParser(),
              )
          FlagCommand::class -> FlagCommand(rulesService)
          SummaryCommand::class -> SummaryCommand(summaryService)
          else -> fallback.create(cls)
        }
            as K
  }
}
