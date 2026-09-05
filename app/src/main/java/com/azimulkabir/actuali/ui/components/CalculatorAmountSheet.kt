package com.azimulkabir.actuali.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
    onDismiss: () -> Unit,
    onApply: (Long) -> Unit,
) {
    val calculator = remember(initialCents) { CalculatorAmountState(initialCents) }
    var revision by remember { mutableIntStateOf(0) }
    fun press(key: String) {
        when (key) {
            "C" -> calculator.clear()
            "⌫" -> calculator.backspace()
            "+" -> calculator.operator(CalculatorAmountState.Operator.ADD)
            "−" -> calculator.operator(CalculatorAmountState.Operator.SUBTRACT)
            "×" -> calculator.operator(CalculatorAmountState.Operator.MULTIPLY)
            "÷" -> calculator.operator(CalculatorAmountState.Operator.DIVIDE)
            else -> calculator.digit(key.toInt())
        }
        revision++
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(calculator.display, style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
            listOf(
                listOf("7", "8", "9", "÷"), listOf("4", "5", "6", "×"),
                listOf("1", "2", "3", "−"), listOf("C", "0", "⌫", "+"),
            ).forEach { keys ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    keys.forEach { key -> Button(onClick = { press(key) }, modifier = Modifier.weight(1f)) { Text(key) } }
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onApply(calculator.finish()) }, modifier = Modifier.weight(1f)) { Text("Apply") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    @Suppress("UNUSED_EXPRESSION") revision
}
