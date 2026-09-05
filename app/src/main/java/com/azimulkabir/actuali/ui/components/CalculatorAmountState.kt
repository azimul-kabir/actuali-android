package com.azimulkabir.actuali.ui.components

import java.math.BigDecimal
import java.math.RoundingMode

/** Port of iOS AmountInputField's calculator-style amount entry. */
class CalculatorAmountState(initialCents: Long = 0, private val allowsNegative: Boolean = false) {
    enum class Operator(val symbol: String) { ADD("+"), SUBTRACT("−"), MULTIPLY("×"), DIVIDE("÷") }

    private var operandCents = initialCents
    private var accumulatorCents: Long? = null
    private var pending: Operator? = null
    private var hasOperand = initialCents != 0L

    val cents: Long
        get() = accumulatorCents?.let { left ->
            if (hasOperand) apply(left, operandCents, requireNotNull(pending)) else left
        } ?: operandCents

    val display: String
        get() {
            val operand = format(operandCents)
            val left = accumulatorCents ?: return operand
            val op = pending ?: return operand
            return if (hasOperand) "${format(left)} ${op.symbol} $operand" else "${format(left)} ${op.symbol}"
        }

    fun digit(value: Int) {
        require(value in 0..9)
        val sign = if (operandCents < 0) -1 else 1
        val magnitude = kotlin.math.abs(operandCents)
        if (magnitude <= (Long.MAX_VALUE - value) / 10) operandCents = sign * (magnitude * 10 + value)
        hasOperand = true
    }

    fun backspace() {
        if (hasOperand) {
            operandCents /= 10
            hasOperand = operandCents != 0L
        } else if (pending != null) {
            operandCents = accumulatorCents ?: 0
            accumulatorCents = null
            pending = null
            hasOperand = operandCents != 0L
        }
    }

    fun toggleSign() {
        if (allowsNegative) operandCents = -operandCents
    }

    fun operator(operator: Operator) {
        if (accumulatorCents == null && !hasOperand) return
        if (hasOperand) {
            accumulatorCents = accumulatorCents?.let { apply(it, operandCents, requireNotNull(pending)) }
                ?: operandCents
            operandCents = 0
            hasOperand = false
        }
        pending = operator
    }

    fun finish(): Long {
        val result = cents
        accumulatorCents = null
        pending = null
        operandCents = result
        hasOperand = result != 0L
        return result
    }

    private fun apply(left: Long, right: Long, operator: Operator): Long = when (operator) {
        Operator.ADD -> left + right
        Operator.SUBTRACT -> left - right
        Operator.MULTIPLY -> BigDecimal(left).multiply(BigDecimal(right))
            .divide(BigDecimal(100), 0, RoundingMode.HALF_UP).longValueExact()
        Operator.DIVIDE -> if (right == 0L) left else BigDecimal(left).multiply(BigDecimal(100))
            .divide(BigDecimal(right), 0, RoundingMode.HALF_UP).longValueExact()
    }.let { if (allowsNegative) it else kotlin.math.abs(it) }

    private fun format(value: Long): String {
        val magnitude = kotlin.math.abs(value)
        return buildString {
            if (value < 0) append('−')
            append(magnitude / 100)
            append('.')
            append((magnitude % 100).toString().padStart(2, '0'))
        }
    }
}
