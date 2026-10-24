package com.yugentech.mynt.ui.split.parent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.mynt.split.model.SPLIT_SELF
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.split.repository.SplitRepository
import com.yugentech.mynt.split.service.SettleMessageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The one Split bill session for the whole app. It acts as the bridge between
 * the UI and the Cloud/Local Sync Repository.
 */
object SplitSession : KoinComponent {
    private val repository: SplitRepository by inject()
    private val functions: FirebaseFunctions by inject()

    val state = SplitScreenState(repository, SettleMessageService(functions))

    /** Set by the voice screen after "Add bill"; Home reads it and opens the Split screen. */
    var openOnHome by mutableStateOf(false)

    /** Connects the local UI state variables to the live Repository Flow */
    fun initializeSync(scope: CoroutineScope) {
        scope.launch {
            repository.activeBills.collect { bills ->
                state.bills.clear()
                state.bills.addAll(bills)

                // Intelligently rebuild the active people list from the database
                val activePeople = bills.flatMap { it.shares.keys + it.paidBy }.distinct()
                val currentSelected = state.form.selected.toList()

                state.people.clear()
                state.people.add(SPLIT_SELF)
                activePeople.filter { it != SPLIT_SELF }.forEach { state.people.add(it) }

                // Ensure form selections don't break when data updates
                state.form.selected.clear()
                currentSelected.filter { it in state.people }.forEach { state.form.selected.add(it) }
                if (state.form.selected.isEmpty()) state.form.selected.add(SPLIT_SELF)
            }
        }

        scope.launch {
            repository.settledRecords.collect { records ->
                state.settled.clear()
                state.settled.addAll(records)
            }
        }
    }

    /** Adds everyone who was there and saves the bill directly to the repository. */
    fun addVoiceBill(participants: List<String>, bill: SplitBill) {
        val everyone = (participants + bill.paidBy + bill.shares.keys)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        everyone.forEach { name ->
            if (state.people.none { it.equals(name, ignoreCase = true) }) {
                state.people.add(name)
                state.form.selected.add(name)
            }
        }

        fun canonical(name: String): String =
            state.people.firstOrNull { it.equals(name.trim(), ignoreCase = true) } ?: name.trim()

        val normalizedBill = bill.copy(
            paidBy = canonical(bill.paidBy),
            shares = bill.shares.entries.associate { (name, share) -> canonical(name) to share }
        )

        state.saveBill(normalizedBill)
        state.tab = SplitTab.BILL
        openOnHome = true
    }
}