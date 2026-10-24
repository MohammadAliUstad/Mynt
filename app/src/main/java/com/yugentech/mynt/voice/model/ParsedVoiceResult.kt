package com.yugentech.mynt.voice.model

import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.split.model.SplitBill

/**
 * Wraps parsed transactions and split bills.
 */
data class ParsedVoiceResult(
    val expense: Expense? = null,
    val splitBill: SplitBill? = null,
    val isUpdate: Boolean = false,
    val previous: Expense? = null,
    /** Everyone who was there for a split bill, including people who didn't share the cost. */
    val participants: List<String> = emptyList()
) {
    val isSplit: Boolean get() = splitBill != null
}