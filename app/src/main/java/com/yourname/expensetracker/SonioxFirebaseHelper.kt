package com.yourname.expensetracker

import android.util.Base64
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Helper class to invoke Firebase Cloud Functions for Soniox transcription and Gemini parsing.
 */
object SonioxFirebaseHelper {

    private val functions: FirebaseFunctions
        get() = FirebaseFunctions.getInstance()

    /**
     * Sends Base64 encoded audio bytes to Firebase Cloud Function 'transcribeVoice',
     * which calls Soniox REST API securely.
     */
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
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Sends transcribed text to Firebase Cloud Function 'parseTransactions' (Gemini)
     * and returns a list of structured transactions (can be more than one).
     */
    suspend fun parseTransactions(spokenText: String): Result<List<Expense>> {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val data = hashMapOf(
                "text" to spokenText,
                "today" to isoFormat.format(Date())
            )

            val result = functions
                .getHttpsCallable("parseTransactions")
                .call(data)
                .await()

            val resultMap = result.data as? Map<*, *>
            val rawEntries = resultMap?.get("entries") as? List<*> ?: emptyList<Any>()

            val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            val expenses = rawEntries.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                val amount = (m["amount"] as? Number)?.toDouble() ?: return@mapNotNull null
                if (amount <= 0.0) return@mapNotNull null

                val type = if ((m["type"] as? String)?.uppercase() == "INCOME") {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }

                val allowedCategories = if (type == TransactionType.INCOME) incomeCategories else expenseCategories
                val rawCategory = m["category"] as? String ?: "Other"
                val category = allowedCategories.firstOrNull { it.equals(rawCategory, ignoreCase = true) } ?: "Other"

                val title = (m["title"] as? String)?.trim().takeUnless { it.isNullOrBlank() } ?: category

                val parsedDate = try {
                    isoFormat.parse(m["date"] as? String ?: "") ?: Date()
                } catch (e: Exception) {
                    Date()
                }

                Expense(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    amount = amount,
                    category = category,
                    date = displayFormat.format(parsedDate),
                    monthYear = monthYearFormat.format(parsedDate),
                    type = type
                )
            }

            Result.success(expenses)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}