package com.yugentech.mynt.split.model

import java.time.LocalDate

/** The name used for the person holding the phone. */
const val SPLIT_SELF = "You"

enum class SplitMode { EQUAL, AMOUNT, PERCENT }

/** One shared bill. [shares] is what each person owes for it and always adds up to [amount]. */
data class SplitBill(
    val id: String,
    val title: String,
    val amount: Double,
    val paidBy: String,
    val shares: Map<String, Double>,
    val date: LocalDate = LocalDate.now()
)

/** "[from] pays [to] [amount]": one payment that helps settle the group. */
data class SplitTransfer(
    val from: String,
    val to: String,
    val amount: Double
)

/** One settle-up that was marked as settled: the people, the bills that went into it and the payments that closed it. */
data class SettledRecord(
    val id: String,
    val settledOn: LocalDate = LocalDate.now(),
    val people: List<String>,
    val bills: List<SplitBill>,
    val transfers: List<SplitTransfer>
)