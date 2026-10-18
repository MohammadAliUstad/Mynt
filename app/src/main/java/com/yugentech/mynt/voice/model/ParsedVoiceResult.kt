package com.yugentech.mynt.voice.model

import com.yugentech.mynt.database.model.Expense

/**
 * Wraps parsed transactions and distinguishes new entries from existing modifications.
 * [previous] is the saved entry being changed, so the UI can show old vs new.
 */
data class ParsedVoiceResult(
    val expense: Expense,
    val isUpdate: Boolean,
    val previous: Expense? = null
)