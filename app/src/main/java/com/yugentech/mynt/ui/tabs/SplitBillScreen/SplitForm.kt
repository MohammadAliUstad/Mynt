package com.yugentech.mynt.ui.split.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.split.logic.SplitCalculator
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.split.model.SplitMode
import com.yugentech.mynt.transactions.insights.TransactionDates
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.shared.util.formatAmount
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SplitForm(
    form: SplitFormState,
    people: List<String>,
    billCount: Int,
    onAddPerson: (String) -> Unit,
    onRemovePerson: (String) -> Unit,
    canRemove: (String) -> Boolean,
    onSubmit: (SplitBill) -> Unit
) {
    val currency = LocalCurrencySymbol.current
    val locale = LocalLocale.current.platformLocale
    val accent = FinanceColors.borrowed
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    var showAddName by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountFocus = remember { FocusRequester() }

    val total = form.amountText.toDoubleOrNull() ?: 0.0
    val included = people.filter { it in form.selected }
    val liveShares = buildShares(total, included, form.mode, form.inputs)
    val entered = included.sumOf { form.inputs[it]?.toDoubleOrNull() ?: 0.0 }

    fun submit() {
        val amount = form.amountText.toDoubleOrNull() ?: 0.0
        val chosen = people.filter { it in form.selected }
        val shares = buildShares(amount, chosen, form.mode, form.inputs)
        when {
            amount <= 0.0 -> form.error = "Enter an amount above zero"
            chosen.none { it != form.paidBy } -> form.error = "Pick at least one other person to split with"
            shares == null -> form.error =
                if (form.mode == SplitMode.AMOUNT) "The amounts must add up to the total"
                else "The percentages must add up to 100"
            else -> onSubmit(
                SplitBill(
                    id = UUID.randomUUID().toString(),
                    title = form.title.trim().ifBlank { "Bill ${billCount + 1}" },
                    amount = amount,
                    paidBy = form.paidBy,
                    shares = shares,
                    date = form.date
                )
            )
        }
    }

    var lastHandled by remember { mutableIntStateOf(form.submitRequests) }
    LaunchedEffect(form.submitRequests) {
        if (form.submitRequests != lastHandled) {
            lastHandled = form.submitRequests
            submit()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            AmountInput(
                value = form.amountText,
                onValueChange = { input ->
                    val cleaned = input.replace(',', '.')
                    if (cleaned.count { it == '.' } <= 1 && cleaned.all { it.isDigit() || it == '.' } &&
                        cleaned.substringAfter('.', "").length <= 2 && cleaned.length <= 12
                    ) {
                        form.amountText = cleaned
                        form.error = null
                    }
                },
                color = accent,
                focusRequester = amountFocus
            )

            Spacer(Modifier.height(28.dp))

            FieldLabel("What for?")
            PillTextField(
                value = form.title,
                onValueChange = { form.title = it },
                placeholder = "Dinner at the café",
                capitalization = KeyboardCapitalization.Sentences
            )

            Spacer(Modifier.height(20.dp))
            FieldLabel("Participants")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showAddName,
                    onClick = { showAddName = !showAddName },
                    label = { Text("Add name") },
                    trailingIcon = {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (showAddName) 90f else 0f)
                        )
                    },
                    shape = CircleShape
                )
                people.forEach { person ->
                    key(person) {
                        val isSelected = person in form.selected
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) form.selected.remove(person) else form.selected.add(person)
                            },
                            label = { Text(person) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = if (isSelected) onPrimary else primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = if (canRemove(person)) {
                                {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Remove $person",
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .clickable { onRemovePerson(person) }
                                    )
                                }
                            } else null,
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primary,
                                selectedLabelColor = onPrimary,
                                selectedLeadingIconColor = onPrimary,
                                selectedTrailingIconColor = onPrimary
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showAddName,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    PillTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = "Type a name, then tap +",
                        capitalization = KeyboardCapitalization.Words,
                        leading = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        trailing = {
                            IconButton(onClick = {
                                onAddPerson(newName)
                                newName = ""
                            }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add participant")
                            }
                        },
                        onDone = {
                            onAddPerson(newName)
                            newName = ""
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            FieldLabel("Paid by")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                people.forEach { person ->
                    key(person) {
                        FilterChip(
                            selected = form.paidBy == person,
                            onClick = { form.paidBy = person },
                            label = { Text(person) },
                            shape = CircleShape
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            FieldLabel("How to split")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    SplitMode.EQUAL to "Equally",
                    SplitMode.AMOUNT to "Amounts",
                    SplitMode.PERCENT to "Percent"
                ).forEach { (option, label) ->
                    FilterChip(
                        selected = form.mode == option,
                        onClick = {
                            form.mode = option
                            form.inputs.clear()
                            form.error = null
                        },
                        label = { Text(label) },
                        shape = CircleShape
                    )
                }
            }

            AnimatedVisibility(
                visible = form.mode != SplitMode.EQUAL && included.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.height(4.dp))
                    included.forEach { person ->
                        NumberPillField(
                            value = form.inputs[person].orEmpty(),
                            onValueChange = {
                                form.inputs[person] = it
                                form.error = null
                            },
                            placeholder = if (form.mode == SplitMode.AMOUNT) "$person's amount" else "$person's %"
                        )
                    }
                    Text(
                        text = if (form.mode == SplitMode.AMOUNT) {
                            "Entered ${formatAmount(entered, currency)} of ${formatAmount(total, currency)}"
                        } else {
                            "Entered ${"%.1f".format(entered)}% of 100%"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (liveShares != null) FinanceColors.income
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            FieldLabel("When")
            val today = TransactionDates.today()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = form.date == today,
                    onClick = { form.date = today },
                    label = { Text("Today") },
                    shape = CircleShape
                )
                FilterChip(
                    selected = form.date == today.minusDays(1),
                    onClick = { form.date = today.minusDays(1) },
                    label = { Text("Yesterday") },
                    shape = CircleShape
                )
                val isOther = form.date != today && form.date != today.minusDays(1)
                FilterChip(
                    selected = isOther,
                    onClick = { showDatePicker = true },
                    leadingIcon = {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    label = {
                        Text(
                            if (isOther) form.date.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
                            else "Pick a date"
                        )
                    },
                    shape = CircleShape
                )
            }

            AnimatedVisibility(visible = form.error != null) {
                Text(
                    text = form.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(Modifier.navigationBarsPadding().height(104.dp))
        }

    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDate = form.date,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= TransactionDates.today().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.getSelectedDate()?.let { form.date = it }
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

private fun buildShares(
    total: Double,
    included: List<String>,
    mode: SplitMode,
    inputs: Map<String, String>
): Map<String, Double>? {
    if (total <= 0.0 || included.isEmpty()) return null
    return when (mode) {
        SplitMode.EQUAL -> SplitCalculator.equalShares(total, included)
        SplitMode.AMOUNT -> {
            val values = included.associateWith { inputs[it]?.toDoubleOrNull() }
            if (values.values.any { it == null }) null
            else {
                val amounts = values.mapValues { it.value!! }
                if (SplitCalculator.amountsMatch(total, amounts.values)) amounts else null
            }
        }
        SplitMode.PERCENT -> {
            val values = included.associateWith { inputs[it]?.toDoubleOrNull() }
            if (values.values.any { it == null }) null
            else {
                val percents = values.mapValues { it.value!! }
                if (SplitCalculator.percentsMatch(percents.values)) {
                    SplitCalculator.percentShares(total, percents)
                } else null
            }
        }
    }
}