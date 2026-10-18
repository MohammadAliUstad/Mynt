package com.yugentech.mynt.ui.split.parent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.mynt.split.logic.SplitCalculator
import com.yugentech.mynt.split.model.SPLIT_SELF
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.split.model.SplitMode
import com.yugentech.mynt.split.model.SplitTransfer
import com.yugentech.mynt.transactions.insights.TransactionDates
import com.yugentech.mynt.ui.shared.components.EmptyState
import com.yugentech.mynt.ui.shared.components.MyntCard
import com.yugentech.mynt.ui.shared.components.SectionHeader
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.shared.util.formatAmount
import kotlinx.coroutines.delay
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.yugentech.mynt.ui.shared.components.orb.OrbEnergy
import androidx.compose.runtime.mutableIntStateOf

enum class SplitTab { SPLIT, BILL }

/** Everything typed into the Split tab, kept here so it survives switching to the Bill tab. */
class SplitFormState {
    var amountText by mutableStateOf("")
    var title by mutableStateOf("")
    var date by mutableStateOf(TransactionDates.today())
    var paidBy by mutableStateOf(SPLIT_SELF)
    var mode by mutableStateOf(SplitMode.EQUAL)
    var error by mutableStateOf<String?>(null)
    var submitRequests by mutableIntStateOf(0)
    val selected = mutableStateListOf(SPLIT_SELF)
    val inputs = mutableStateMapOf<String, String>()

    /** Clears what belongs to one bill; keeps the people, payer and date for the next one. */
    fun reset() {
        amountText = ""
        title = ""
        mode = SplitMode.EQUAL
        error = null
        inputs.clear()
    }
}

/** Everything the Split bill screen knows. It lives in the Home container so the bar can drive it. */
class SplitScreenState {
    var tab by mutableStateOf(SplitTab.SPLIT)
    val people = mutableStateListOf(SPLIT_SELF)
    val bills = mutableStateListOf<SplitBill>()
    val form = SplitFormState()

    /** What the single bar button says. */
    val barLabel: String
        get() = if (tab == SplitTab.SPLIT) "Split" else "Add bill"

    /** Split tab: save the bill. Bill tab: go back to the Split tab to add another. */
    fun onBarButton() {
        if (tab == SplitTab.SPLIT) form.submitRequests++ else tab = SplitTab.SPLIT
    }
}

/**
 * Split a bill: the Split tab is laid out like a new entry (amount, what for, participants,
 * who paid, how to split, when); the Bill tab shows who owes whom as a table or a graph.
 * Everything lives in memory for now; saving and voice come next.
 */
@Composable
fun SplitBillScreen(
    state: SplitScreenState,
    onBackClick: () -> Unit
) {
    var tab by state::tab
    val people = state.people
    val bills = state.bills
    val form = state.form
    val locale = LocalLocale.current.platformLocale

    // If the last bill is deleted there is nothing to show on the Bill tab.
    LaunchedEffect(bills.size) {
        if (bills.isEmpty() && tab == SplitTab.BILL) tab = SplitTab.SPLIT
    }

    fun addPerson(raw: String) {
        val name = raw.trim().replaceFirstChar { it.titlecase(locale) }
        if (name.isEmpty()) return
        val existing = people.firstOrNull { it.equals(name, ignoreCase = true) }
        if (existing == null) {
            people.add(name)
            form.selected.add(name)
        } else if (existing !in form.selected) {
            form.selected.add(existing)
        }
    }

    fun removePerson(name: String) {
        people.remove(name)
        form.selected.remove(name)
        form.inputs.remove(name)
        if (form.paidBy == name) form.paidBy = SPLIT_SELF
    }

    fun canRemove(name: String): Boolean =
        name != SPLIT_SELF && bills.none { it.paidBy == name || name in it.shares }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
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
                text = "Split bill",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        SplitTabSelector(
            selected = tab,
            billEnabled = bills.isNotEmpty(),
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        AnimatedContent(
            targetState = tab,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(300)) { w -> if (forward) w / 4 else -w / 4 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300)) { w -> if (forward) -w / 4 else w / 4 } + fadeOut(tween(200)))
            },
            label = "splitTab"
        ) { current ->
            when (current) {
                SplitTab.SPLIT -> SplitForm(
                    form = form,
                    people = people.toList(),
                    billCount = bills.size,
                    onAddPerson = ::addPerson,
                    onRemovePerson = ::removePerson,
                    canRemove = ::canRemove,
                    onVoiceClick = {},
                    onSubmit = { bill ->
                        bills.add(bill)
                        form.reset()
                        tab = SplitTab.BILL
                    }
                )

                SplitTab.BILL -> BillTab(
                    bills = bills.toList(),
                    people = people.toList(),
                    onDeleteBill = { bill -> bills.removeAll { it.id == bill.id } },
                    onAddAnother = { tab = SplitTab.SPLIT }
                )
            }
        }
        }
    }
}

/** Two-way switch with a sliding highlight. Bill stays greyed out until there is a bill. */
@Composable
private fun SplitTabSelector(
    selected: SplitTab,
    billEnabled: Boolean,
    onSelect: (SplitTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(SplitTab.SPLIT to "Split", SplitTab.BILL to "Bill")
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.padding(4.dp)) {
            val cell = maxWidth / options.size
            val offset by animateDpAsState(
                targetValue = cell * options.indexOfFirst { it.first == selected },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                label = "splitTabIndicator"
            )
            Box(
                modifier = Modifier
                    .offset(x = offset)
                    .width(cell)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(FinanceColors.borrowed)
            )
            Row(modifier = Modifier.fillMaxSize()) {
                options.forEach { (option, label) ->
                    val enabled = option != SplitTab.BILL || billEnabled
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                enabled = enabled,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                isSelected -> Color.White
                                enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                        )
                    }
                }
            }
        }
    }
}

/** The Split tab: amount, what for, participants, who paid, how to split and when. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SplitForm(
    form: SplitFormState,
    people: List<String>,
    billCount: Int,
    onAddPerson: (String) -> Unit,
    onRemovePerson: (String) -> Unit,
    canRemove: (String) -> Boolean,
    onVoiceClick: () -> Unit,
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

    // The bar's Split button asks this form to submit by bumping a counter.
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
                // No logging the future; that's what reminders are for.
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

/** Each person's share for one bill, or null when the entered amounts or percentages don't add up. */
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

/** The Bill tab: every bill, who owes whom as a table or a graph, and the total settle-up. */
@Composable
private fun BillTab(
    bills: List<SplitBill>,
    people: List<String>,
    onDeleteBill: (SplitBill) -> Unit,
    onAddAnother: () -> Unit
) {
    val currency = LocalCurrencySymbol.current
    var showGraph by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    // While switching Table/Graph, the page is held at this scroll position so it never jumps.
    var lockedOffset by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(lockedOffset) {
        val target = lockedOffset ?: return@LaunchedEffect
        val end = System.currentTimeMillis() + 500
        while (System.currentTimeMillis() < end) {
            withFrameNanos { }
            if (scrollState.value != target) scrollState.scrollTo(target)
        }
        lockedOffset = null
    }

    fun switchView(graph: Boolean) {
        if (graph == showGraph) return
        lockedOffset = scrollState.value
        showGraph = graph
    }

    val transfers = remember(people, bills) { SplitCalculator.settle(people, bills) }
    val balances = remember(people, bills) { SplitCalculator.balances(people, bills) }
    val amountLabels = transfers.map { formatAmount(it.amount, currency) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            title = "Who owes whom",
            actionLabel = "Add another bill",
            onAction = onAddAnother
        )
        Spacer(Modifier.height(6.dp))

        if (bills.isEmpty()) {
            MyntCard(modifier = Modifier.fillMaxWidth()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = "No bills yet",
                    message = "Add a bill on the Split tab."
                )
            }
        } else {
            AppearAnimated {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ViewToggle(showGraph = showGraph, onChange = { switchView(it) })

                    AnimatedContent(
                        targetState = showGraph,
                        transitionSpec = {
                            (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.96f)) togetherWith
                                    (fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.98f))
                        },
                        label = "splitView"
                    ) { graph ->
                        if (graph) {
                            MyntCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                                SplitGraph(
                                    people = people,
                                    balances = balances,
                                    transfers = transfers,
                                    amountLabels = amountLabels
                                )
                                Text(
                                    "Each arrow points from the person who pays to the person who gets paid.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    bills.forEach { bill ->
                                        key(bill.id) {
                                            // Stacks up when added; swipe right to left to delete.
                                            StackInItem(onRemoved = { onDeleteBill(bill) }) { remove ->
                                                Box(modifier = Modifier.padding(bottom = 10.dp)) {
                                                    SwipeableBill(bill = bill, onDelete = remove)
                                                }
                                            }
                                        }
                                    }
                                }
                                SettleUpCard(transfers = transfers)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.navigationBarsPadding().height(104.dp))
    }
}

/** Fades in and slides up the first time it appears. */
@Composable
private fun AppearAnimated(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 3 } + expandVertically(tween(320))
    ) {
        content()
    }
}

/**
 * A list item that stacks up when added, and animates away when [content] calls the
 * `remove` it is given. [onRemoved] runs once the exit animation has finished.
 */
@Composable
private fun StackInItem(
    onRemoved: () -> Unit,
    content: @Composable (remove: () -> Unit) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(removing) {
        if (removing) {
            visible = false
            delay(300)
            onRemoved()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 2 } + expandVertically(tween(320)),
        exit = fadeOut(tween(240)) + slideOutVertically(tween(240)) { it / 2 } + shrinkVertically(tween(240))
    ) {
        content { removing = true }
    }
}

/** A bill row that deletes on a swipe from right to left, like the transactions list. */
@Composable
private fun SwipeableBill(bill: SplitBill, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            val active = state.targetValue == SwipeToDismissBoxValue.EndToStart
            val background by animateColorAsState(
                if (active) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                label = "billSwipeBackground"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(background)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete bill",
                    tint = if (active) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) {
        BillAccordion(bill = bill)
    }
}

@Composable
private fun ViewToggle(showGraph: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        ToggleChip("Table", Icons.Rounded.TableChart, selected = !showGraph) { onChange(false) }
        ToggleChip("Graph", Icons.Rounded.Hub, selected = showGraph) { onChange(true) }
    }
}

@Composable
private fun ToggleChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

/** Column labels shared by every table: who owes, how much, and who they pay. */
@Composable
private fun TableLabels() {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text("Who owes", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = muted)
        Text("Amount", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = muted)
        Text("Pays to", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = muted)
    }
}

/** One table line: who owes, the amount, then who they pay. */
@Composable
private fun TransferRow(from: String, to: String, amount: Double) {
    val currency = LocalCurrencySymbol.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            from,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FinanceColors.expense,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            formatAmount(amount, currency),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            maxLines = 1
        )
        Text(
            to,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FinanceColors.income,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** One closed row per bill, titled with the bill's name. Tap it to open its table. */
@Composable
private fun BillAccordion(bill: SplitBill) {
    val currency = LocalCurrencySymbol.current
    val locale = LocalLocale.current.platformLocale
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "billArrow"
    )

    MyntCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                bill.title,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(arrowRotation),
                tint = muted
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Paid by ${bill.paidBy} · ${formatAmount(bill.amount, currency)} · " +
                            bill.date.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale)),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
                Spacer(Modifier.height(10.dp))

                val owing = bill.shares.filter { (name, share) -> name != bill.paidBy && share > 0.0 }
                if (owing.isEmpty()) {
                    Text(
                        "Nobody owes anything for this bill.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                } else {
                    TableLabels()
                    owing.forEach { (name, share) ->
                        TransferRow(from = name, to = bill.paidBy, amount = share)
                    }
                }
            }
        }
    }
}

/** All bills added together, cancelled out, and reduced to the fewest payments. */
@Composable
private fun SettleUpCard(transfers: List<SplitTransfer>) {
    MyntCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 14.dp
    ) {
        Text(
            "Total settle-up",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (transfers.isEmpty()) {
            Text(
                "Everyone is settled up.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TableLabels()
            transfers.forEachIndexed { index, transfer ->
                key(transfer.from, transfer.to) {
                    Column {
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        TransferRow(from = transfer.from, to = transfer.to, amount = transfer.amount)
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitGraph(
    people: List<String>,
    balances: Map<String, Double>,
    transfers: List<SplitTransfer>,
    amountLabels: List<String>
) {
    val measurer = rememberTextMeasurer()
    val owedColor = FinanceColors.income
    val owesColor = FinanceColors.expense
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    val arrowColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val pillColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val nameStyle = MaterialTheme.typography.labelMedium
    val boldStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        val nodeRadius = 26.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val ring = min(size.width, size.height) / 2f - nodeRadius - 24.dp.toPx()
        val count = people.size

        val points = people.mapIndexed { i, name ->
            val angle = -PI / 2 + 2 * PI * i / count
            name to Offset(
                center.x + (ring * cos(angle)).toFloat(),
                center.y + (ring * sin(angle)).toFloat()
            )
        }.toMap()

        // Arrows first, so the circles sit on top of their ends.
        transfers.forEachIndexed { index, transfer ->
            val from = points[transfer.from] ?: return@forEachIndexed
            val to = points[transfer.to] ?: return@forEachIndexed
            val dx = to.x - from.x
            val dy = to.y - from.y
            val dist = hypot(dx, dy)
            if (dist == 0f) return@forEachIndexed

            val gap = nodeRadius + 4.dp.toPx()
            val start = Offset(from.x + dx / dist * gap, from.y + dy / dist * gap)
            val end = Offset(to.x - dx / dist * gap, to.y - dy / dist * gap)

            drawLine(arrowColor, start, end, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)

            val heading = atan2(dy, dx) + PI.toFloat()
            val head = 11.dp.toPx()
            listOf(heading - 0.45f, heading + 0.45f).forEach { wing ->
                drawLine(
                    arrowColor,
                    end,
                    Offset(end.x + head * cos(wing), end.y + head * sin(wing)),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val label = measurer.measure(amountLabels[index], boldStyle)
            val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
            drawRoundRect(
                color = pillColor,
                topLeft = Offset(mid.x - label.size.width / 2f - 6.dp.toPx(), mid.y - label.size.height / 2f - 2.dp.toPx()),
                size = Size(label.size.width + 12.dp.toPx(), label.size.height + 4.dp.toPx()),
                cornerRadius = CornerRadius(10.dp.toPx())
            )
            drawText(
                label,
                color = onSurface,
                topLeft = Offset(mid.x - label.size.width / 2f, mid.y - label.size.height / 2f)
            )
        }

        points.forEach { (name, point) ->
            val balance = balances[name] ?: 0.0
            val color = when {
                balance > 0.005 -> owedColor
                balance < -0.005 -> owesColor
                else -> neutral
            }
            drawCircle(color.copy(alpha = 0.16f), radius = nodeRadius, center = point)
            drawCircle(color, radius = nodeRadius, center = point, style = Stroke(width = 2.dp.toPx()))

            val initial = measurer.measure(name.first().uppercase(), boldStyle)
            drawText(
                initial,
                color = color,
                topLeft = Offset(point.x - initial.size.width / 2f, point.y - initial.size.height / 2f)
            )

            val label = measurer.measure(
                name,
                nameStyle,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = Constraints(maxWidth = 84.dp.roundToPx())
            )
            drawText(
                label,
                color = onSurface,
                topLeft = Offset(point.x - label.size.width / 2f, point.y + nodeRadius + 4.dp.toPx())
            )
        }
    }
}

/** The amount, big and centred, like the New entry screen. */
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
        textAlign = TextAlign.Center,
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
        Box(modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 60.dp)) {
            if (value.isEmpty()) {
                Text("0", style = textStyle.copy(color = color.copy(alpha = 0.35f)))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle,
                singleLine = true,
                cursorBrush = SolidColor(color),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier
                    .widthIn(min = 60.dp)
                    .focusRequester(focusRequester)
            )
        }
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
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onDone: (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = leading,
        trailingIcon = trailing,
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Same look as [PillTextField], for typing an amount or a percentage. */
@Composable
private fun NumberPillField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    TextField(
        value = value,
        onValueChange = { text ->
            if (text.matches(Regex("\\d*\\.?\\d{0,2}"))) onValueChange(text)
        },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}