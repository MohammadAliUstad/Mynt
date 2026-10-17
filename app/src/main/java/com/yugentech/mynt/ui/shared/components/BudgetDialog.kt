package com.yugentech.mynt.ui.shared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol

@Composable
fun BudgetDialog(
    currentBudget: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember {
        mutableStateOf(if (currentBudget > 0) currentBudget.toLong().toString() else "")
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val parsed = text.toDoubleOrNull()
    val isValid = parsed != null && parsed > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Monthly budget", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Mynt tracks your spending against this each month and tells you how much you can safely spend per day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> if (input.length <= 12 && input.all { it.isDigit() || it == '.' }) text = input },
                    prefix = { Text("${LocalCurrencySymbol.current} ", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("30000") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (isValid) onSave(parsed!!) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            Button(onClick = { parsed?.let(onSave) }, enabled = isValid) { Text("Save") }
        },
        dismissButton = {
            BudgetDismissActions(
                showRemove = currentBudget > 0,
                onRemove = { onSave(0.0) },
                onCancel = onDismiss
            )
        }
    )
}

@Composable
private fun BudgetDismissActions(showRemove: Boolean, onRemove: () -> Unit, onCancel: () -> Unit) {
    Row {
        if (showRemove) {
            TextButton(onClick = onRemove) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}
