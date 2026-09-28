package com.aliadnani.commands

import com.aliadnani.services.RulesService
import java.util.concurrent.Callable
import picocli.CommandLine.ArgGroup
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import picocli.CommandLine.Spec

@Command(name = "flag", description = ["Flag (i.e. find) transactions based on a rule"])
class FlagCommand(private val rulesService: RulesService) : Callable<Int> {

  @Spec lateinit var spec: CommandSpec

  private val out
    get() = spec.commandLine().out

  class RuleSelection {
    @Option(names = ["--rule"], paramLabel = "RULE", description = ["Rule to apply"])
    var rule: String? = null

    @Option(names = ["--list-rules"], description = ["List available rules"])
    var listRules: Boolean = false
  }

  @ArgGroup(exclusive = true, multiplicity = "1") lateinit var selection: RuleSelection

  override fun call(): Int {
    if (selection.listRules) {
      rulesService.listRules().forEach { out.println(it) }
      return 0
    }

    val ruleName = selection.rule!!
    return when (val result = rulesService.flag(ruleName)) {
      is RulesService.FlagResult.Success -> {
        if (result.transactions.isEmpty()) {
          out.println("No transactions matched rule: $ruleName")
        } else {
          result.transactions.forEach { out.println(it) }
        }
        0
      }

      RulesService.FlagResult.RuleNotFound ->
          throw ParameterException(spec.commandLine(), "Unknown rule: $ruleName")
    }
  }
}
