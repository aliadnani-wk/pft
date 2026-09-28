package com.aliadnani.services.rules

import com.aliadnani.model.Transaction

sealed interface Rule {
  val ruleName: String

  fun execute(): List<Transaction>
}
