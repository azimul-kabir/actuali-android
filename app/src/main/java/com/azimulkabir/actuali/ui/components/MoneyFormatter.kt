package com.azimulkabir.actuali.ui.components

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

object BalanceVisibility {
    @Volatile var hidden: Boolean = false
}

fun formatMoneyCents(
    cents: Long,
    hideDecimalPlaces: Boolean,
    showPositiveSign: Boolean = false,
): String {
    if (BalanceVisibility.hidden) return "••••"
    val sign = when {
        cents < 0 -> "−"
        cents > 0 && showPositiveSign -> "+"
        else -> ""
    }
    val magnitude = cents.absoluteValue
    val whole = NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-BD")).format(magnitude / 100)
    val decimals = if (hideDecimalPlaces) "" else ".${(magnitude % 100).toString().padStart(2, '0')}"
    return "$sign৳$whole$decimals"
}

fun centsToInput(cents: Long): String {
    val magnitude = cents.absoluteValue
    val decimal = (magnitude % 100).toString().padStart(2, '0')
    return "${magnitude / 100}.$decimal"
}

fun parseInputCents(value: String): Long? = runCatching {
    value.trim().toBigDecimal().movePointRight(2).longValueExact()
}.getOrNull()
