package com.yugentech.mynt.ui.split.parent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.yugentech.mynt.split.logic.SplitCalculator
import com.yugentech.mynt.split.model.SPLIT_SELF
import com.yugentech.mynt.split.model.SettledRecord
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.split.model.SplitMode
import com.yugentech.mynt.split.repository.SplitRepository
import com.yugentech.mynt.split.service.SettleMessageService
import com.yugentech.mynt.transactions.insights.TransactionDates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

enum class SplitTab { SPLIT, BILL, SETTLED }

/** Where the AI message for a settle-up is at. */
sealed interface MessageState {
    data object Idle : MessageState
    data object Loading : MessageState
    data class Ready(val text: String) : MessageState
    data class Failed(val message: String) : MessageState
}

/** Everything typed into the Split tab. */
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

    fun reset() {
        amountText = ""
        title = ""
        mode = SplitMode.EQUAL
        error = null
        inputs.clear()
    }

    fun resetAll() {
        reset()
        date = TransactionDates.today()
        paidBy = SPLIT_SELF
        selected.clear()
        selected.add(SPLIT_SELF)
    }
}

/** The reactive state connected to the Repository */
class SplitScreenState(
    private val repository: SplitRepository,
    private val messageService: SettleMessageService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var generateJob: Job? = null

    var tab by mutableStateOf(SplitTab.SPLIT)
    val people = mutableStateListOf(SPLIT_SELF)
    val bills = mutableStateListOf<SplitBill>()
    val form = SplitFormState()
    val settled = mutableStateListOf<SettledRecord>()

    // Settled tab: which card is selected, delete confirmation, and the AI message panel.
    var selectedSettledId by mutableStateOf<String?>(null)
    var confirmDelete by mutableStateOf(false)
    var panelRecord by mutableStateOf<SettledRecord?>(null)
    var messageState by mutableStateOf<MessageState>(MessageState.Idle)

    /** Set from the UI so the message uses the user's currency. */
    var currencySymbol: String = "₹"

    /** Where each settled card sits on screen, so the panel can grow out of it. */
    val cardBounds = mutableMapOf<String, Rect>()

    fun saveBill(bill: SplitBill) {
        CoroutineScope(Dispatchers.IO).launch { repository.saveBill(bill) }
    }

    fun updateBill(updated: SplitBill) {
        CoroutineScope(Dispatchers.IO).launch { repository.saveBill(updated) }
    }

    fun deleteBill(bill: SplitBill) {
        CoroutineScope(Dispatchers.IO).launch { repository.deleteBill(bill.id) }
    }

    fun settleAll() {
        if (bills.isEmpty()) return
        val record = SettledRecord(
            id = UUID.randomUUID().toString(),
            settledOn = TransactionDates.today(),
            people = people.toList(),
            bills = bills.toList(),
            transfers = SplitCalculator.settle(people.toList(), bills.toList())
        )
        bills.clear()
        CoroutineScope(Dispatchers.IO).launch {
            repository.saveSettledRecord(record)
            repository.clearActiveBills()
        }
        tab = SplitTab.SETTLED
        form.resetAll()
    }

    // ---- Settled tab selection ----

    fun select(id: String) {
        selectedSettledId = id
    }

    /** A plain tap only does something while a card is already selected. */
    fun toggleIfSelecting(id: String) {
        if (selectedSettledId == null) return
        selectedSettledId = if (selectedSettledId == id) null else id
    }

    fun clearSelection() {
        selectedSettledId = null
    }

    // ---- Delete ----

    fun requestDelete() {
        if (selectedSettledId != null) confirmDelete = true
    }

    fun cancelDelete() {
        confirmDelete = false
    }

    fun confirmDeleteNow() {
        val id = selectedSettledId
        confirmDelete = false
        selectedSettledId = null
        if (id == null) return
        cardBounds.remove(id)
        scope.launch { repository.deleteSettledRecord(id) }
    }

    // ---- AI message ----

    fun generateMessage() {
        val record = settled.firstOrNull { it.id == selectedSettledId } ?: return
        selectedSettledId = null
        panelRecord = record
        runGeneration(record)
    }

    fun retryMessage() {
        panelRecord?.let { runGeneration(it) }
    }

    private fun runGeneration(record: SettledRecord) {
        generateJob?.cancel()
        messageState = MessageState.Loading
        generateJob = scope.launch {
            val result = messageService.generate(record, currencySymbol)
            messageState = result.fold(
                onSuccess = { MessageState.Ready(it) },
                onFailure = {
                    MessageState.Failed("Couldn't write the message. Check your connection and try again.")
                }
            )
        }
    }

    fun closePanel() {
        generateJob?.cancel()
        panelRecord = null
    }

    // ---- Bottom bar ----

    val barLabel: String
        get() = when {
            panelRecord != null -> "Done"
            tab == SplitTab.SETTLED && selectedSettledId != null -> "Generate message"
            tab == SplitTab.SPLIT -> "Split"
            else -> "Add bill"
        }

    fun onBarButton() {
        when {
            panelRecord != null -> closePanel()
            tab == SplitTab.SETTLED && selectedSettledId != null -> generateMessage()
            tab == SplitTab.SPLIT -> form.submitRequests++
            else -> tab = SplitTab.SPLIT
        }
    }
}