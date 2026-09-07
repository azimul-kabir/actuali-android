package com.azimulkabir.actua.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorAmountSheet(
    title: String,
    initialCents: Long,
    conventionalAmountEntry: Boolean = false,
    onDismiss: () -> Unit,
    onApply: (Long) -> Unit,
) {
    val calculator = remember(initialCents, conventionalAmountEntry) {
        CalculatorAmountState(initialCents, conventionalAmountEntry = conventionalAmountEntry)
    }
    var revision by remember { mutableIntStateOf(0) }
    fun press(key: String) {
        when (key) {
            "C" -> calculator.clear()
            "⌫" -> calculator.backspace()
            "." -> calculator.decimalPoint()
            "+" -> calculator.operator(CalculatorAmountState.Operator.ADD)
            "−" -> calculator.operator(CalculatorAmountState.Operator.SUBTRACT)
            "×" -> calculator.operator(CalculatorAmountState.Operator.MULTIPLY)
            "÷" -> calculator.operator(CalculatorAmountState.Operator.DIVIDE)
            "=" -> calculator.finish()
            else -> calculator.digit(key.toInt())
        }
        revision++
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp))
            Text(calculator.display, style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3")).forEach { keys ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            keys.forEach { key -> NumberKey(key, Modifier.weight(1f)) { press(key) } }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val alternateKey = if (conventionalAmountEntry) "." else "C"
                        NumberKey(alternateKey, Modifier.weight(1f)) { press(alternateKey) }
                        NumberKey("0", Modifier.weight(1f)) { press("0") }
                        NumberKey("⌫", Modifier.weight(1f)) { press("⌫") }
                    }
                }
                Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OperatorKey("×", Modifier.weight(1f)) { press("×") }
                        OperatorKey("÷", Modifier.weight(1f)) { press("÷") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OperatorKey("+", Modifier.weight(1f)) { press("+") }
                        OperatorKey("−", Modifier.weight(1f)) { press("−") }
                    }
                    FilledTonalButton(onClick = { press("=") }, modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(22.dp)) {
                        Text("=", style = MaterialTheme.typography.headlineSmall)
                    }
                    Button(onClick = { onApply(calculator.finish()) }, modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(22.dp)) {
                        Text("Done", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
    @Suppress("UNUSED_EXPRESSION") revision
}

@Composable
private fun NumberKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier.height(58.dp), shape = RoundedCornerShape(18.dp)) {
        Text(label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Normal)
    }
}

@Composable
private fun OperatorKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(58.dp), shape = RoundedCornerShape(22.dp)) {
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}
