package com.yourname.expensetracker

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Dialog for editing or deleting an existing transaction.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: Expense,
    onDismissRequest: () -> Unit,
    onSaveTransaction: (Expense) -> Unit,
    onDeleteTransaction: (Expense) -> Unit = {}
) {
    val context = LocalContext.current

    // Date Formatters
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    // Parse initial date string into Calendar
    val initialCalendar = remember(transaction.date) {
        val cal = Calendar.getInstance()
        try {
            val parsedDate = dateFormatter.parse(transaction.date)
            if (parsedDate != null) {
                cal.time = parsedDate
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cal
    }

    // States for fields
    var titleText by remember { mutableStateOf(transaction.title) }
    var amountText by remember {
        mutableStateOf(
            if (transaction.amount % 1.0 == 0.0) transaction.amount.toLong().toString() else transaction.amount.toString()
        )
    }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var selectedCalendar by remember { mutableStateOf(initialCalendar) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = if (transaction.type == TransactionType.EXPENSE) expenseCategories else incomeCategories

    // Date Picker Dialog setup
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedCalendar = newCalendar
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Edit Transaction",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. TITLE FIELD
                Text(
                    text = "Description / Title",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. AMOUNT FIELD
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            amountText = input
                        }
                    },
                    prefix = {
                        Text(
                            text = "${SettingsManager.currencySymbol} ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. CATEGORY CHIPS
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. DATE SELECTION
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = dateFormatter.format(selectedCalendar.time),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    if (titleText.isBlank() || parsedAmount == null || parsedAmount <= 0.0) {
                        errorMessage = "Please enter a valid title and positive amount"
                    } else {
                        errorMessage = null
                        val updated = transaction.copy(
                            title = titleText.trim(),
                            amount = parsedAmount,
                            category = selectedCategory,
                            date = dateFormatter.format(selectedCalendar.time),
                            monthYear = monthYearFormatter.format(selectedCalendar.time)
                        )
                        onSaveTransaction(updated)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onDeleteTransaction(transaction) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
