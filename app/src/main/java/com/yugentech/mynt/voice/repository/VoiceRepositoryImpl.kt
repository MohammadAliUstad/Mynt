package com.yugentech.mynt.voice.repository

import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.voice.model.ParsedVoiceResult
import com.yugentech.mynt.voice.parser.VoiceTransactionParser
import com.yugentech.mynt.voice.service.VoiceService

class VoiceRepositoryImpl(
    private val voiceService: VoiceService
) : VoiceRepository {

    override suspend fun transcribeAudio(audioBytes: ByteArray): Result<String> {
        return voiceService.transcribeAudio(audioBytes)
    }

    // Uses the AI parser first and falls back to the on-device regex parser
    override suspend fun parseTransactions(
        spokenText: String,
        recentExpenses: List<Expense>
    ): List<ParsedVoiceResult> {
        val aiParsed = voiceService.parseTransactions(spokenText, recentExpenses).getOrNull()
        if (!aiParsed.isNullOrEmpty()) return aiParsed

        val fallback = VoiceTransactionParser.parse(spokenText)
        return if (fallback != null) listOf(ParsedVoiceResult(fallback, isUpdate = false)) else emptyList()
    }
}
