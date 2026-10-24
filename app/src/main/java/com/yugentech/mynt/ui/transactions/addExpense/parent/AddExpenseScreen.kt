package com.yugentech.mynt.ui.transactions.addExpense.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.yugentech.mynt.ui.shared.components.MyntCard
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.shared.util.categoryStyle
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val typeOrder = listOf(
    TransactionType.EXPENSE,
    TransactionType.INCOME,
    TransactionType.LENT,
    TransactionType.BORROWED
)

private fun TransactionType.label() = when (this) {
    TransactionType.EXPENSE -> "Expense"
    TransactionType.INCOME -> "Income"
    TransactionType.LENT -> "Lent"
    TransactionType.BORROWED -> "Borrowed"
}

/**
 * Adding and editing share one screen. A new entry opens with the amount focused so you can
 * type straight away; an existing one opens filled in, with delete in the corner.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    existing: Expense?,
    initialType: TransactionType,
    knownPeople: List<String>,
    onBackClick: () -> Unit,
    onSave: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    val isEditing = existing != null

    var type by rememberSaveable { mutableStateOf(existing?.type ?: initialType) }
    var amountText by rememberSaveable {
        mutableStateOf(existing?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    var title by rememberSaveable {
        mutableStateOf(
            existing?.let { if (it.title.equals(it.personName, ignoreCase = true)) "" else it.title } ?: ""
        )
    }
    var person by rememberSaveable { mutableStateOf(existing?.personName ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: defaultCategory(initialType)) }
    var date by rememberSaveable { mutableStateOf(existing?.localDate ?: TransactionDates.today()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val accent by animateColorAsState(FinanceColors.forType(type), label = "accent")
    val isPersonType = type == TransactionType.LENT || type == TransactionType.BORROWED
    val categories = when (type) {
        TransactionType.EXPENSE -> expenseCategories
        TransactionType.INCOME -> incomeCategories
        else -> emptyList()
    }

    val amountFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (!isEditing) amountFocus.requestFocus() }

    fun submit() {
        val amount = amountText.toDoubleOrNull()
        val cleanPerson = person.trim()
        when {
            amount == null || amount <= 0 -> error = "Enter an amount above zero"
            isPersonType && cleanPerson.isEmpty() -> error = if (type == TransactionType.LENT) "Who did you lend to?" else "Who did you borrow from?"
            else -> {
                val finalCategory = when {
                    isPersonType && category == SETTLEMENT_CATEGORY -> SETTLEMENT_CATEGORY
                    type == TransactionType.LENT -> LENDING_CATEGORY
                    type == TransactionType.BORROWED -> BORROWING_CATEGORY
                    category in categories -> category
                    else -> categories.first()
                }
                val finalTitle = title.trim().ifEmpty { if (isPersonType) cleanPerson else finalCategory }
                val base = existing ?: Expense(
                    id = UUID.randomUUID().toString(),
                    title = finalTitle,
                    amount = amount,
                    category = finalCategory,
                    date = "",
                    monthYear = ""
                )
                onSave(
                    base.copy(
                        title = finalTitle,
                        amount = amount,
                        category = finalCategory,
                        type = type,
                        personName = if (isPersonType) cleanPerson else "",
                        note = note.trim()
                    ).onDate(date)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isEditing) "Edit entry" else "New entry",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            if (existing != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            TypeSelector(
                selected = type,
                onSelect = { newType ->
                    if (newType != type) {
                        type = newType
                        category = defaultCategory(newType)
                        error = null
                    }
                }
            )

            Spacer(Modifier.height(28.dp))

            // The amount, big and centred
            AmountInput(
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
                color = accent,
                focusRequester = amountFocus
            )

            Spacer(Modifier.height(28.dp))

            AnimatedVisibility(
                visible = isPersonType,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    FieldLabel(if (type == TransactionType.LENT) "Lent to" else "Borrowed from")
                    PillTextField(
                        value = person,
                        onValueChange = {
                            person = it
                            error = null
                        },
                        placeholder = "Name",
                        leading = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        capitalization = KeyboardCapitalization.Words
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
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = category == SETTLEMENT_CATEGORY,
                            onClick = {
                                category = if (category == SETTLEMENT_CATEGORY) defaultCategory(type) else SETTLEMENT_CATEGORY
                            },
                            label = {
                                Text(if (type == TransactionType.LENT) "This pays back what I owe" else "This is them paying me back")
                            },
                            shape = CircleShape
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            FieldLabel(if (isPersonType) "Note (optional)" else "What for?")
            PillTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = when (type) {
                    TransactionType.EXPENSE -> "Chair, Coffee, Lunch…"
                    TransactionType.INCOME -> "October salary"
                    else -> "Dinner split, rent share…"
                },
                capitalization = KeyboardCapitalization.Sentences
            )

            if (categories.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                FieldLabel("Category")
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
            FieldLabel("When")
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

            // AI Context Sentence in the void space below "When"
            if (note.isNotBlank()) {
                Spacer(Modifier.height(28.dp))
                MyntCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = 18.dp
                ) {
                    Text(
                        text = "“${note.trim()}”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
        }

        Button(
            onClick = ::submit,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(56.dp)
        ) {
            Text(
                text = if (isEditing) "Save changes" else "Add ${type.label().lowercase()}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDate = date,
            selectableDates = object : SelectableDates {
                // No logging the future; that's what reminders are for.
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

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete this entry?") },
            text = { Text("You can undo this right after.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(existing)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } }
        )
    }
}

private fun defaultCategory(type: TransactionType) = when (type) {
    TransactionType.EXPENSE -> expenseCategories.first()
    TransactionType.INCOME -> incomeCategories.first()
    TransactionType.LENT -> LENDING_CATEGORY
    TransactionType.BORROWED -> BORROWING_CATEGORY
}

/** Four-way switch with a sliding highlight that takes on the type's colour. */
@Composable
private fun TypeSelector(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.padding(4.dp)) {
            val cell = maxWidth / typeOrder.size
            val offset by animateDpAsState(
                targetValue = cell * typeOrder.indexOf(selected),
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                label = "typeIndicator"
            )
            val indicatorColor by animateColorAsState(FinanceColors.forType(selected), label = "typeColor")
            Box(
                modifier = Modifier
                    .offset(x = offset)
                    .width(cell)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Row(modifier = Modifier.fillMaxSize()) {
                typeOrder.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label(),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    focusRequester: FocusRequester
) {
    val textStyle = MaterialTheme.typography.displayMedium.copy(
        fontWeight = FontWeight.Bold,
        color = color,
        fontSize = if (value.length > 8) 40.sp else 52.sp
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = LocalCurrencySymbol.current,
            style = textStyle.copy(fontSize = 32.sp, color = color.copy(alpha = 0.7f))
        )
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(color),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .widthIn(min = 40.dp)
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text("0", style = textStyle.copy(color = color.copy(alpha = 0.35f)))
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization,
    leading: (@Composable () -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = leading,
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Done),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}