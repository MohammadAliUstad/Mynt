package com.yugentech.mynt.database.mapper

import com.yugentech.mynt.database.entity.TransactionEntity
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType

fun TransactionEntity.toExpense(): Expense {
    val transactionType = try {
        TransactionType.valueOf(type)
    } catch (_: IllegalArgumentException) {
        TransactionType.EXPENSE
    }

    return Expense(
        id = id,
        title = title,
        amount = amount,
        category = category,
        date = date,
        monthYear = monthYear,
        type = transactionType,
        personName = personName,
        timestamp = timestamp
    )
}

fun Expense.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        title = title,
        amount = amount,
        category = category,
        date = date,
        monthYear = monthYear,
        type = type.name,
        personName = personName,
        timestamp = timestamp
    )
}
