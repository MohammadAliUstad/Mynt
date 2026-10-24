package com.yugentech.mynt.voice.repository

import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.voice.model.ParsedVoiceResult
import com.yugentech.mynt.voice.parser.VoiceTransactionParser
import com.yugentech.mynt.voice.service.ClarificationException
import com.yugentech.mynt.voice.service.VoiceService

class VoiceRepositoryImpl(
    private val voiceService: VoiceService
) : VoiceRepository {

    override suspend fun transcribeAudio(audioBytes: ByteArray): Result<String> {
        return voiceService.transcribeAudio(audioBytes)
    }

    override suspend fun parseTransactions(
        spokenText: String,
        recentExpenses: List<Expense>,
        activeSplitPeople: List<String>
    ): Result<List<ParsedVoiceResult>> {
        val aiResult = voiceService.parseTransactions(spokenText, recentExpenses, activeSplitPeople)

        if (aiResult.isFailure && aiResult.exceptionOrNull() is ClarificationException) {
            return aiResult
        }

        val aiParsed = aiResult.getOrNull()
        if (!aiParsed.isNullOrEmpty()) return Result.success(aiParsed)

        // SMART LOCAL FALLBACK
        val textLower = spokenText.lowercase()
        val hasAmount = Regex("\\d+").containsMatchIn(spokenText)

        val isSplit = textLower.contains("split") ||
                textLower.contains("between") ||
                textLower.contains("friends") ||
                textLower.contains("we had") ||
                activeSplitPeople.size > 1 // If there are active friends, it's a split context!

        if (isSplit) {
            val hasPayer = textLower.contains("paid") || textLower.contains("i paid") || textLower.contains("me")

            if (!hasAmount) {
                return Result.failure(ClarificationException("I got the people, but what was the total amount?"))
            }
            if (!hasPayer) {
                return Result.failure(ClarificationException("I heard the amount, but who paid the bill?"))
            }
        } else {
            if (!hasAmount && spokenText.isNotBlank()) {
                return Result.failure(ClarificationException("I couldn't catch the amount. How much was it?"))
            }
        }

        val fallback = VoiceTransactionParser.parse(spokenText)
        val fallbackList = if (fallback != null) listOf(ParsedVoiceResult(fallback, isUpdate = false)) else emptyList()
        return Result.success(fallbackList)
    }
}