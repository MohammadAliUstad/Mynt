package com.yugentech.mynt.voice.model

import com.yugentech.mynt.database.model.Expense

/**
 * Wraps parsed transactions and distinguishes new entries from existing modifications.
 */
data class ParsedVoiceResult(
    val expense: Expense,
    val isUpdate: Boolean
)
