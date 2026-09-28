package com.aliadnani.model

import java.math.BigDecimal

internal const val MAX_AMOUNT_DECIMAL_PLACES = 2

internal fun isRepresentableInCents(amount: BigDecimal): Boolean =
    amount.stripTrailingZeros().scale() <= MAX_AMOUNT_DECIMAL_PLACES
