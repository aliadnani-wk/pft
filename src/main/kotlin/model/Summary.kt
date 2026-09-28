package com.aliadnani.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Summary(
    val before: LocalDateTime?,
    val after: LocalDateTime?,
    val totalSpend: BigDecimal,
    val totalIncome: BigDecimal,
    val spendByCategory: List<CategorySpend>,
) {
  fun net(): BigDecimal = totalIncome - totalSpend
}

data class CategorySpend(val categoryName: String?, val totalSpend: BigDecimal)
