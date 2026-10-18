package com.yugentech.mynt.split.logic

import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.split.model.SplitTransfer
import kotlin.math.abs
import kotlin.math.roundToLong

/** All split maths, done in paise so the totals are always exact. */
object SplitCalculator {

    private fun toMinor(amount: Double): Long = (amount * 100).roundToLong()
    private fun toMajor(minor: Long): Double = minor / 100.0

    /** Splits [total] evenly; odd paise go to the first people so the sum stays exact. */
    fun equalShares(total: Double, people: List<String>): Map<String, Double> {
        if (people.isEmpty()) return emptyMap()
        val totalMinor = toMinor(total)
        val base = totalMinor / people.size
        val extra = (totalMinor % people.size).toInt()
        return people.mapIndexed { index, name ->
            name to toMajor(base + if (index < extra) 1 else 0)
        }.toMap()
    }

    /** Percent split. Any rounding leftover goes to the largest share so the sum stays exact. */
    fun percentShares(total: Double, percents: Map<String, Double>): Map<String, Double> {
        if (percents.isEmpty()) return emptyMap()
        val totalMinor = toMinor(total)
        val minor = percents
            .mapValues { (_, pct) -> (totalMinor * pct / 100.0).roundToLong() }
            .toMutableMap()
        val diff = totalMinor - minor.values.sum()
        if (diff != 0L) {
            val largest = minor.maxByOrNull { it.value }!!.key
            minor[largest] = minor.getValue(largest) + diff
        }
        return minor.mapValues { toMajor(it.value) }
    }

    fun amountsMatch(total: Double, amounts: Collection<Double>): Boolean =
        amounts.sumOf { toMinor(it) } == toMinor(total)

    fun percentsMatch(percents: Collection<Double>): Boolean =
        abs(percents.sum() - 100.0) < 0.005

    /** Net position in paise: positive means they are owed, negative means they owe. */
    private fun netMinor(people: List<String>, bills: List<SplitBill>): Map<String, Long> {
        val net = people.associateWith { 0L }.toMutableMap()
        bills.forEach { bill ->
            net[bill.paidBy] = (net[bill.paidBy] ?: 0L) + toMinor(bill.amount)
            bill.shares.forEach { (name, share) ->
                net[name] = (net[name] ?: 0L) - toMinor(share)
            }
        }
        return net
    }

    fun balances(people: List<String>, bills: List<SplitBill>): Map<String, Double> =
        netMinor(people, bills).mapValues { toMajor(it.value) }

    /** Matches the biggest debtor with the biggest creditor until everyone is square. */
    fun settle(people: List<String>, bills: List<SplitBill>): List<SplitTransfer> {
        val net = netMinor(people, bills)
        val creditors = net.filter { it.value > 0 }.map { it.key to it.value }
            .sortedByDescending { it.second }.toMutableList()
        val debtors = net.filter { it.value < 0 }.map { it.key to -it.value }
            .sortedByDescending { it.second }.toMutableList()

        val transfers = mutableListOf<SplitTransfer>()
        var c = 0
        var d = 0
        while (c < creditors.size && d < debtors.size) {
            val pay = minOf(creditors[c].second, debtors[d].second)
            transfers += SplitTransfer(debtors[d].first, creditors[c].first, toMajor(pay))
            creditors[c] = creditors[c].first to creditors[c].second - pay
            debtors[d] = debtors[d].first to debtors[d].second - pay
            if (creditors[c].second == 0L) c++
            if (debtors[d].second == 0L) d++
        }
        return transfers
    }
}