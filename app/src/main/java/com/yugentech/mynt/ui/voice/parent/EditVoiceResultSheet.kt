package com.yugentech.mynt.ui.voice.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.database.model.BORROWING_CATEGORY
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.LENDING_CATEGORY
import com.yugentech.mynt.database.model.SETTLEMENT_CATEGORY
import com.yugentech.mynt.database.model.TransactionType
import com.yugentech.mynt.database.model.expenseCategories
import com.yugentech.mynt.database.model.incomeCategories
import com.yugentech.mynt.transactions.insights.TransactionDates
import com.yugentech.mynt.transactions.insights.localDate
import com.yugentech.mynt.transactions.insights.onDate
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.shared.util.categoryStyle
import kotlinx.coroutines.launch
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val sheetTypeOrder = listOf(
    TransactionType.EXPENSE,
    TransactionType.INCOME,
    TransactionType.LENT,
    TransactionType.BORROWED
)

private fun TransactionType.sheetLabel() = when (this) {
    TransactionType.EXPENSE -> "Expense"
    TransactionType.INCOME -> "Income"
    TransactionType.LENT -> "Lent"
    TransactionType.BORROWED -> "Borrowed"
}

private fun sheetDefaultCategory(type: TransactionType) = when (type) {
    TransactionType.EXPENSE -> expenseCategories.first()
    TransactionType.INCOME -> incomeCategories.first()
    TransactionType.LENT -> LENDING_CATEGORY
    TransactionType.BORROWED -> BORROWING_CATEGORY
}

/**
 * Bottom sheet for correcting a transaction that was parsed from voice.
 *
 * It only edits the entry held in the voice screen's list. Nothing is written to the database
 * here; the normal "Save entry" button still does that with the corrected values.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditVoiceResultSheet(
    expense: Expense,
    knownPeople: List<String>,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var type by remember { mutableStateOf(expense.type) }
    var amountText by remember {
        mutableStateOf(if (expense.amount % 1.0 == 0.0) expense.amount.toLong().toString() else expense.amount.toString())
    }
    var title by remember {
        mutableStateOf(if (expense.title.equals(expense.personName, ignoreCase = true)) "" else expense.title)
    }
    var person by remember { mutableStateOf(expense.personName) }
    var category by remember { mutableStateOf(expense.category) }
    var date by remember { mutableStateOf(expense.localDate) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isPersonType = type == TransactionType.LENT || type == TransactionType.BORROWED
    val categories = when (type) {
        TransactionType.EXPENSE -> expenseCategories
        TransactionType.INCOME -> incomeCategories
        else -> emptyList()
    }

    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    fun submit() {
        val amount = amountText.toDoubleOrNull()
        val cleanPerson = person.trim()
        when {
            amount == null || amount <= 0 -> error = "Enter an amount above zero"
            isPersonType && cleanPerson.isEmpty() ->
                error = if (type == TransactionType.LENT) "Who did you lend to?" else "Who did you borrow from?"

            else -> {
                val finalCategory = when {
                    isPersonType && category == SETTLEMENT_CATEGORY -> SETTLEMENT_CATEGORY
                    type == TransactionType.LENT -> LENDING_CATEGORY
                    type == TransactionType.BORROWED -> BORROWING_CATEGORY
                    category in categories -> category
                    else -> categories.first()
                }
                val finalTitle = title.trim().ifEmpty { if (isPersonType) cleanPerson else finalCategory }
                val updated = expense.copy(
                    title = finalTitle,
                    amount = amount,
                    category = finalCategory,
                    type = type,
                    personName = if (isPersonType) cleanPerson else ""
                ).onDate(date)
                closeThen { onSave(updated) }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Edit entry",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(16.dp))
            SheetLabel("Type")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sheetTypeOrder.forEach { option ->
                    FilterChip(
                        selected = option == type,
                        onClick = {
                            if (option != type) {
                                type = option
                                category = sheetDefaultCategory(option)
                                error = null
                            }
                        },
                        label = { Text(option.sheetLabel()) },
                        shape = CircleShape
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SheetLabel("Amount")
            SheetTextField(
                value = amountText,
                onValueChange = { input ->
                    val cleaned = input.replace(',', '.')
                    if (cleaned.count { it == '.' } <= 1 && cleaned.all { it.isDigit() || it == '.' } &&
                        cleaned.substringAfter('.', "").length <= 2 && cleaned.length <= 12
                    ) {
                        amountText = cleaned
                        error = null
                    }
                },
                placeholder = "0",
                keyboardType = KeyboardType.Decimal,
                capitalization = KeyboardCapitalization.None,
                prefix = LocalCurrencySymbol.current
            )

            AnimatedVisibility(
                visible = isPersonType,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    SheetLabel(if (type == TransactionType.LENT) "Lent to" else "Borrowed from")
                    SheetTextField(
                        value = person,
                        onValueChange = {
                            person = it
                            error = null
                        },
                        placeholder = "Name",
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                        leading = { Icon(Icons.Rounded.Person, contentDescription = null) }
                    )
                    val suggestions = knownPeople
                        .filter { it.contains(person.trim(), ignoreCase = true) && !it.equals(person.trim(), ignoreCase = true) }
                        .take(6)
                    if (suggestions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggestions.forEach { name ->
                                FilterChip(
                                    selected = false,
                                    onClick = { person = name },
                                    label = { Text(name) },
                                    shape = CircleShape
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    FilterChip(
                        selected = category == SETTLEMENT_CATEGORY,
                        onClick = {
                            category = if (category == SETTLEMENT_CATEGORY) sheetDefaultCategory(type) else SETTLEMENT_CATEGORY
                        },
                        label = {
                            Text(if (type == TransactionType.LENT) "This pays back what I owe" else "This is them paying me back")
                        },
                        shape = CircleShape
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SheetLabel(if (isPersonType) "Note (optional)" else "What for?")
            SheetTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = when (type) {
                    TransactionType.EXPENSE -> "Lunch with the team"
                    TransactionType.INCOME -> "October salary"
                    else -> "Dinner split, rent share…"
                },
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences
            )

            if (categories.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SheetLabel("Category")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { option ->
                        val style = categoryStyle(option, type)
                        val selected = option == category
                        FilterChip(
                            selected = selected,
                            onClick = { category = option },
                            label = { Text(option) },
                            leadingIcon = {
                                Icon(
                                    style.icon,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else style.color,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SheetLabel("When")
            val today = TransactionDates.today()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = date == today,
                    onClick = { date = today },
                    label = { Text("Today") },
                    shape = CircleShape
                )
                FilterChip(
                    selected = date == today.minusDays(1),
                    onClick = { date = today.minusDays(1) },
                    label = { Text("Yesterday") },
                    shape = CircleShape
                )
                val isOther = date != today && date != today.minusDays(1)
                FilterChip(
                    selected = isOther,
                    onClick = { showDatePicker = true },
                    leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = {
                        Text(
                            if (isOther) date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
                            else "Pick a date"
                        )
                    },
                    shape = CircleShape
                )
            }

            AnimatedVisibility(visible = error != null) {
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = ::submit,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Apply changes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDate = date,
            selectableDates = object : SelectableDates {
                // Same rule as the Add Expense screen: no logging the future.
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= TransactionDates.today().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.getSelectedDate()?.let { date = it }
                    showDatePicker = false
                }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    capitalization: KeyboardCapitalization,
    prefix: String? = null,
    leading: (@Composable () -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = leading,
        prefix = prefix?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = ImeAction.Done
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}