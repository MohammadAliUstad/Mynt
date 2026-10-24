package com.yugentech.mynt.voice.service

import android.util.Base64
import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType
import com.yugentech.mynt.database.model.expenseCategories
import com.yugentech.mynt.database.model.incomeCategories
import com.yugentech.mynt.split.logic.SplitCalculator
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.split.model.SPLIT_SELF
import com.yugentech.mynt.voice.model.ParsedVoiceResult
import timber.log.Timber
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.UUID

class ClarificationException(val prompt: String) : Exception(prompt)

class VoiceService(
    private val functions: FirebaseFunctions
) {

    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/m4a"
    ): Result<String> {
        return try {
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val data = hashMapOf("audioBase64" to audioBase64, "mimeType" to mimeType)
            val result = functions.getHttpsCallable("transcribeVoice").call(data).await()
            val resultMap = result.data as? Map<*, *>
            val transcribedText = resultMap?.get("text") as? String ?: ""

            if (transcribedText.isNotBlank()) Result.success(transcribedText)
            else Result.failure(Exception("No speech detected by Soniox."))
        } catch (e: Exception) {
            Timber.e(e, "Voice cloud function failed")
            Result.failure(e)
        }
    }

    suspend fun parseTransactions(
        spokenText: String,
        recentExpenses: List<Expense> = emptyList(),
        activeSplitPeople: List<String> = emptyList()
    ): Result<List<ParsedVoiceResult>> {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val displayFormatStandard = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            val candidates = recentExpenses.sortedByDescending { it.timestamp }.take(60)
            val candidateList = candidates.map { exp ->
                val isoDate = try {
                    val parsed = displayFormatStandard.parse(exp.date) ?: SimpleDateFormat("d MMM yyyy", Locale.getDefault()).parse(exp.date)
                    if (parsed != null) isoFormat.format(parsed) else exp.date
                } catch (e: Exception) {
                    exp.date
                }

                hashMapOf(
                    "id" to exp.id,
                    "title" to exp.title,
                    "amount" to exp.amount,
                    "category" to exp.category,
                    "date" to isoDate,
                    "type" to exp.type.name,
                    "personName" to exp.personName,
                    "note" to exp.note
                )
            }

            val data = hashMapOf(
                "text" to spokenText,
                "today" to isoFormat.format(Date()),
                "recentTransactions" to candidateList,
                "activeParticipants" to activeSplitPeople
            )

            val result = functions.getHttpsCallable("parseTransactions").call(data).await()
            val resultMap = result.data as? Map<*, *>

            val rootAction = (resultMap?.get("action") as? String)?.uppercase()
            if (rootAction == "CLARIFY" || resultMap?.containsKey("missingInfo") == true) {
                val prompt = (resultMap["missingInfo"] as? String) ?: "I missed some details. Could you repeat?"
                throw ClarificationException(prompt)
            }

            val rawEntries = resultMap?.get("entries") as? List<*> ?: emptyList<Any>()
            val baseTimestamp = System.currentTimeMillis()

            val parsedResults = rawEntries.mapIndexedNotNull { index, item ->
                val m = item as? Map<*, *> ?: return@mapIndexedNotNull null

                val action = (m["action"] as? String)?.uppercase() ?: ""

                if (action == "CLARIFY" || m.containsKey("missingInfo")) {
                    val prompt = (m["missingInfo"] as? String) ?: "I missed some details. Could you repeat?"
                    throw ClarificationException(prompt)
                }

                val amount = (m["amount"] as? Number)?.toDouble() ?: return@mapIndexedNotNull null
                if (amount <= 0.0) return@mapIndexedNotNull null

                val parsedDate = try { isoFormat.parse(m["date"] as? String ?: "") ?: Date() } catch (e: Exception) { Date() }
                val localDate = parsedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

                val lowerText = spokenText.lowercase(Locale.getDefault())
                val titleStr = (m["title"] as? String)?.trim().takeUnless { it.isNullOrBlank() }

                val typeStr = (m["type"] as? String)?.uppercase()
                val type = when (typeStr) {
                    "LENT" -> TransactionType.LENT
                    "BORROWED" -> TransactionType.BORROWED
                    "INCOME" -> TransactionType.INCOME
                    else -> TransactionType.EXPENSE
                }

                val personName = (m["personName"] as? String)?.trim() ?: ""
                val category = when (type) {
                    TransactionType.LENT -> "Lending"
                    TransactionType.BORROWED -> "Borrowing"
                    TransactionType.INCOME -> incomeCategories.firstOrNull { it.equals(m["category"] as? String, true) } ?: "Other"
                    TransactionType.EXPENSE -> expenseCategories.firstOrNull { it.equals(m["category"] as? String, true) } ?: "Other"
                }

                val finalTitle = titleStr ?: if (personName.isNotBlank()) personName else category
                var finalNote = (m["note"] as? String)?.trim().orEmpty()

                // ==========================================
                // LOCAL SAFETY FALLBACKS
                // ==========================================
                if (type == TransactionType.LENT || type == TransactionType.BORROWED) {
                    val answeredNoName = Regex("\\b(no|save without name|without name|skip|none)\\b").containsMatchIn(lowerText)
                    if (personName.isBlank() && !answeredNoName) {
                        val actionWord = if (type == TransactionType.LENT) "lent the money to" else "borrowed the money from"
                        throw ClarificationException("Do you want to give the name of the person that you $actionWord?")
                    }
                }

                if (action != "SPLIT_BILL" && type == TransactionType.EXPENSE) {
                    val impliesGroup = Regex("\\b(colleague|colleagues|friend|friends|roommate|roommates|us|we)\\b").containsMatchIn(lowerText)
                    val answeredRegular = Regex("\\b(regular )?expense\\b").containsMatchIn(lowerText)
                    val answeredSplit = Regex("\\b(split( bill)?)\\b").containsMatchIn(lowerText)

                    if (impliesGroup && !answeredRegular && !answeredSplit) {
                        throw ClarificationException("Is this a split bill, or just a regular expense?")
                    }

                    if (answeredSplit) {
                        val everyone = if (activeSplitPeople.size > 1) activeSplitPeople else listOf(SPLIT_SELF, "Friend")
                        val splitBill = SplitBill(
                            id = UUID.randomUUID().toString(),
                            title = finalTitle.replace(Regex("(?i)\\.\\s*split( bill)?"), "").trim().ifBlank { "Split bill" },
                            amount = amount,
                            paidBy = SPLIT_SELF,
                            shares = SplitCalculator.equalShares(amount, everyone),
                            date = localDate
                        )
                        return@mapIndexedNotNull ParsedVoiceResult(splitBill = splitBill, participants = everyone)
                    }

                    // If Gemini forgot to generate the note on a group expense, create a clean fallback note
                    if ((answeredRegular || impliesGroup) && finalNote.isBlank()) {
                        val cleanSpoken = spokenText
                            .replace(Regex("(?i)\\.\\s*(regular )?expense"), "")
                            .trim()
                        finalNote = "Spent on ${finalTitle.lowercase(Locale.getDefault())} (${cleanSpoken})"
                    }
                }

                // ==========================================
                // STANDARD SPLIT BILL HANDLING
                // ==========================================
                if (action == "SPLIT_BILL" || typeStr == "SPLIT_BILL") {
                    val paidByRaw = (m["paidBy"] as? String)?.trim().takeUnless { it.isNullOrBlank() } ?: SPLIT_SELF
                    val paidBy = if (paidByRaw.equals("me", true) || paidByRaw.equals("i", true)) SPLIT_SELF else paidByRaw

                    val participantsRaw = (m["participants"] as? List<*>)?.map { it.toString().trim() } ?: listOf(SPLIT_SELF)
                    val parsedParticipants = participantsRaw.map { if (it.equals("me", true) || it.equals("i", true)) SPLIT_SELF else it }.distinct()
                    val mergedParticipants = (parsedParticipants + activeSplitPeople).distinct()

                    val excludedRaw = (m["excluded"] as? List<*>)?.map { it.toString().trim() } ?: emptyList()
                    val excluded = excludedRaw.map { if (it.equals("me", true) || it.equals("i", true)) SPLIT_SELF else it }

                    val everyone = mutableListOf<String>().also { list ->
                        (mergedParticipants + paidBy).forEach { name ->
                            if (list.none { it.equals(name, ignoreCase = true) }) list.add(name)
                        }
                    }

                    val included = everyone.filter { name -> excluded.none { it.equals(name, ignoreCase = true) } }
                    val shares = SplitCalculator.equalShares(amount, included.ifEmpty { everyone })

                    val splitBill = SplitBill(
                        id = UUID.randomUUID().toString(),
                        title = finalTitle,
                        amount = amount,
                        paidBy = paidBy,
                        shares = shares,
                        date = localDate
                    )

                    return@mapIndexedNotNull ParsedVoiceResult(
                        splitBill = splitBill,
                        participants = everyone
                    )
                }

                val matchedId = (m["id"] as? String)?.trim().orEmpty()
                val original: Expense? = if (action == "UPDATE" && matchedId.isNotBlank()) {
                    candidates.firstOrNull { it.id == matchedId }
                } else null
                val isUpdate = original != null

                val entryTimestamp = baseTimestamp + (index * 10L)

                val expense = original?.copy(
                    title = finalTitle,
                    amount = amount,
                    category = category,
                    date = displayFormatStandard.format(parsedDate),
                    monthYear = monthYearFormat.format(parsedDate),
                    type = type,
                    personName = personName,
                    note = if (finalNote.isNotBlank()) finalNote else original.note
                ) ?: Expense(
                    id = UUID.randomUUID().toString(),
                    title = finalTitle,
                    amount = amount,
                    category = category,
                    date = displayFormatStandard.format(parsedDate),
                    monthYear = monthYearFormat.format(parsedDate),
                    type = type,
                    personName = personName,
                    note = finalNote,
                    timestamp = entryTimestamp
                )

                ParsedVoiceResult(expense = expense, isUpdate = isUpdate, previous = original)
            }

            Result.success(parsedResults)
        } catch (e: ClarificationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Voice cloud function failed")
            Result.failure(e)
        }
    }
}