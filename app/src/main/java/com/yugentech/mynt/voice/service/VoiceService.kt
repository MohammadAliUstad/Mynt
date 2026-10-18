package com.yugentech.mynt.voice.service

import android.util.Base64
import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType
import com.yugentech.mynt.database.model.expenseCategories
import com.yugentech.mynt.database.model.incomeCategories
import com.yugentech.mynt.voice.model.ParsedVoiceResult
import timber.log.Timber
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Calls the transcribeVoice (Soniox) and parseTransactions (Gemini) Cloud Functions
class VoiceService(
    private val functions: FirebaseFunctions
) {

    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/m4a"
    ): Result<String> {
        return try {
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            val data = hashMapOf(
                "audioBase64" to audioBase64,
                "mimeType" to mimeType
            )

            val result = functions
                .getHttpsCallable("transcribeVoice")
                .call(data)
                .await()

            val resultMap = result.data as? Map<*, *>
            val transcribedText = resultMap?.get("text") as? String ?: ""

            if (transcribedText.isNotBlank()) {
                Result.success(transcribedText)
            } else {
                Result.failure(Exception("No speech detected by Soniox."))
            }
        } catch (e: Exception) {
            Timber.e(e, "Voice cloud function failed")
            Result.failure(e)
        }
    }

    suspend fun parseTransactions(
        spokenText: String,
        recentExpenses: List<Expense> = emptyList()
    ): Result<List<ParsedVoiceResult>> {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val displayFormatStandard = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            // Candidates sent to Gemini. Updates are only accepted if they match one of these.
            val candidates = recentExpenses.sortedByDescending { it.timestamp }.take(60)

            val candidateList = candidates.map { exp ->
                val isoDate = try {
                    val parsed = displayFormatStandard.parse(exp.date)
                        ?: SimpleDateFormat("d MMM yyyy", Locale.getDefault()).parse(exp.date)
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
                    "personName" to exp.personName
                )
            }

            val data = hashMapOf(
                "text" to spokenText,
                "today" to isoFormat.format(Date()),
                "recentTransactions" to candidateList
            )

            val result = functions
                .getHttpsCallable("parseTransactions")
                .call(data)
                .await()

            val resultMap = result.data as? Map<*, *>
            val rawEntries = resultMap?.get("entries") as? List<*> ?: emptyList<Any>()

            val parsedResults = rawEntries.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                val amount = (m["amount"] as? Number)?.toDouble() ?: return@mapNotNull null
                if (amount <= 0.0) return@mapNotNull null

                val action = (m["action"] as? String)?.uppercase() ?: "CREATE"
                val matchedId = (m["id"] as? String)?.trim().orEmpty()

                // Only a real update if Gemini's id matches a saved entry we sent it.
                // Otherwise it is treated as a new entry, so a bad id never writes anywhere odd.
                val original: Expense? = if (action == "UPDATE" && matchedId.isNotBlank()) {
                    candidates.firstOrNull { it.id == matchedId }
                } else null
                val isUpdate = original != null

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
                    TransactionType.INCOME -> {
                        val rawCategory = m["category"] as? String ?: "Other"
                        incomeCategories.firstOrNull { it.equals(rawCategory, ignoreCase = true) } ?: "Other"
                    }
                    TransactionType.EXPENSE -> {
                        val rawCategory = m["category"] as? String ?: "Other"
                        expenseCategories.firstOrNull { it.equals(rawCategory, ignoreCase = true) } ?: "Other"
                    }
                }

                val title = (m["title"] as? String)?.trim().takeUnless { it.isNullOrBlank() }
                    ?: if (personName.isNotBlank()) personName else category

                val parsedDate = try {
                    isoFormat.parse(m["date"] as? String ?: "") ?: Date()
                } catch (e: Exception) {
                    Date()
                }

                val expense = if (original != null) {
                    // Start from the saved entry so hidden fields (timestamp etc.) are preserved,
                    // then apply the new values. Same id means the save overwrites the original.
                    original.copy(
                        title = title,
                        amount = amount,
                        category = category,
                        date = displayFormatStandard.format(parsedDate),
                        monthYear = monthYearFormat.format(parsedDate),
                        type = type,
                        personName = personName
                    )
                } else {
                    Expense(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        amount = amount,
                        category = category,
                        date = displayFormatStandard.format(parsedDate),
                        monthYear = monthYearFormat.format(parsedDate),
                        type = type,
                        personName = personName
                    )
                }

                ParsedVoiceResult(expense = expense, isUpdate = isUpdate, previous = original)
            }

            Result.success(parsedResults)
        } catch (e: Exception) {
            Timber.e(e, "Voice cloud function failed")
            Result.failure(e)
        }
    }
}