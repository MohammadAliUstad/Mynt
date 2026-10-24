package com.yugentech.mynt.voice.repository

import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.voice.model.ParsedVoiceResult

interface VoiceRepository {
    suspend fun transcribeAudio(audioBytes: ByteArray): Result<String>
    suspend fun parseTransactions(spokenText: String, recentExpenses: List<Expense>, activeSplitPeople: List<String>): Result<List<ParsedVoiceResult>>
}