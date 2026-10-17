package com.yugentech.mynt.voice.parser

import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

object VoiceTransactionParser {

    private val monthMap = mapOf(
        "january" to 0, "jan" to 0, "february" to 1, "feb" to 1,
        "march" to 2, "mar" to 2, "april" to 3, "apr" to 3,
        "may" to 4, "june" to 5, "jun" to 5, "july" to 6, "jul" to 6,
        "august" to 7, "aug" to 7, "september" to 8, "sep" to 8, "sept" to 8,
        "october" to 9, "oct" to 9, "november" to 10, "nov" to 10,
        "december" to 11, "dec" to 11
    )

    private val wordDayMap = mapOf(
        "first" to 1, "1st" to 1, "second" to 2, "2nd" to 2,
        "third" to 3, "3rd" to 3, "fourth" to 4, "4th" to 4,
        "fifth" to 5, "5th" to 5, "sixth" to 6, "6th" to 6,
        "seventh" to 7, "7th" to 7, "eighth" to 8, "8th" to 8,
        "ninth" to 9, "9th" to 9, "tenth" to 10, "10th" to 10,
        "eleventh" to 11, "11th" to 11, "twelfth" to 12, "12th" to 12,
        "thirteenth" to 13, "13th" to 13, "fourteenth" to 14, "14th" to 14,
        "fifteenth" to 15, "15th" to 15, "sixteenth" to 16, "16th" to 16,
        "seventeenth" to 17, "17th" to 17, "eighteenth" to 18, "18th" to 18,
        "nineteenth" to 19, "19th" to 19, "twentieth" to 20, "20th" to 20,
        "twenty first" to 21, "21st" to 21, "twenty second" to 22, "22nd" to 22,
        "twenty third" to 23, "23rd" to 23, "twenty fourth" to 24, "24th" to 24,
        "twenty fifth" to 25, "25th" to 25, "twenty sixth" to 26, "26th" to 26,
        "twenty seventh" to 27, "27th" to 27, "twenty eighth" to 28, "28th" to 28,
        "twenty ninth" to 29, "29th" to 29, "thirtieth" to 30, "30th" to 30,
        "thirty first" to 31, "31st" to 31
    )

    fun parse(spokenText: String): Expense? {
        val raw = spokenText.trim()
        if (raw.isBlank()) return null

        val currentCalendar = Calendar.getInstance()
        var textWorking = raw
        val lowerFull = raw.lowercase(Locale.getDefault())

        var targetDay: Int? = null
        var targetMonth: Int? = null
        var targetYear: Int? = null

        if (lowerFull.contains("yesterday")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            targetDay = cal.get(Calendar.DAY_OF_MONTH)
            targetMonth = cal.get(Calendar.MONTH)
            targetYear = cal.get(Calendar.YEAR)
            textWorking = textWorking.replace(Regex("(?i)\\byesterday\\b"), " ")
        }

        val finalCalendar = Calendar.getInstance().apply {
            if (targetDay != null && targetMonth != null) {
                set(Calendar.YEAR, targetYear ?: currentCalendar.get(Calendar.YEAR))
                set(Calendar.MONTH, targetMonth)
                set(Calendar.DAY_OF_MONTH, targetDay)
            }
        }

        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val formattedDate = dateFormatter.format(finalCalendar.time)
        val formattedMonthYear = monthYearFormatter.format(finalCalendar.time)

        val normalizedText = textWorking.replace(Regex("(\\d+),\\s*(\\d+)"), "$1$2")
        val regexAmount = Regex("\\b(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\b")
        val amountMatches = regexAmount.findAll(normalizedText).toList()

        var amountDouble: Double? = null
        var matchedAmountText = ""

        for (m in amountMatches) {
            val valStr = m.groupValues[1].replace(",", "")
            val parsedVal = valStr.toDoubleOrNull()
            if (parsedVal != null && parsedVal > 0.0) {
                amountDouble = parsedVal
                matchedAmountText = m.value
                break
            }
        }

        if (amountDouble == null) return null

        // Determine Type & Person Name
        val type: TransactionType
        var personName = ""

        when {
            lowerFull.contains("lent") -> {
                type = TransactionType.LENT
                personName = extractPersonName(raw, "lent", "to")
            }
            lowerFull.contains("borrowed") -> {
                type = TransactionType.BORROWED
                personName = extractPersonName(raw, "borrowed", "from")
            }
            lowerFull.contains("earned") || lowerFull.contains("receive") || lowerFull.contains("received") || lowerFull.contains("got") -> {
                type = TransactionType.INCOME
            }
            else -> {
                type = TransactionType.EXPENSE
            }
        }

        val category = if (type == TransactionType.LENT || type == TransactionType.BORROWED) "Lending" else determineCategory(lowerFull, type)
        val title = if (personName.isNotBlank()) "$personName" else determineTitle(raw, matchedAmountText, category)

        return Expense(
            id = UUID.randomUUID().toString(),
            title = title,
            amount = amountDouble,
            category = category,
            date = formattedDate,
            monthYear = formattedMonthYear,
            type = type,
            personName = personName
        )
    }

    private fun extractPersonName(raw: String, keyword: String, preposition: String): String {
        val regex = Regex("(?i)\\b$keyword\\b.*?\\b$preposition\\b\\s+([a-zA-Z]+)")
        val match = regex.find(raw)
        return match?.groupValues?.get(1)?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: ""
    }

    private fun determineCategory(lower: String, type: TransactionType): String {
        return if (type == TransactionType.EXPENSE) {
            when {
                lower.contains("emi") || lower.contains("loan") -> "EMI"
                lower.contains("food") || lower.contains("lunch") || lower.contains("coffee") -> "Food"
                lower.contains("travel") || lower.contains("cab") || lower.contains("uber") -> "Travel"
                else -> "Other"
            }
        } else "Salary"
    }

    private fun determineTitle(raw: String, amountStr: String, category: String): String {
        var cleaned = raw
            .replace(amountStr, "", ignoreCase = true)
            .replace(Regex("(?i)\\b(spent|paid|earned|received|got|for|on|in|rs|rupees|inr)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.length < 2) cleaned = category
        return cleaned
    }
}