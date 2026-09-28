package com.aliadnani.services

import com.aliadnani.model.Transaction
import com.aliadnani.services.rules.Rule

class RulesService(private val rules: List<Rule>) {

  fun listRules(): List<String> = rules.map(Rule::ruleName)

  sealed interface FlagResult {
    data class Success(val transactions: List<Transaction>) : FlagResult

    data object RuleNotFound : FlagResult
  }

  fun flag(ruleName: String): FlagResult {
    val rule = rules.firstOrNull { it.ruleName == ruleName } ?: return FlagResult.RuleNotFound

    return FlagResult.Success(rule.execute())
  }
}
