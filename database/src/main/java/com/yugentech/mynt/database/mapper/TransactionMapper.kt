package com.yugentech.mynt.database.mapper

import com.yugentech.mynt.database.entity.TransactionEntity
import com.yugentech.mynt.database.model.Expense
import com.yugentech.mynt.database.model.TransactionType

fun TransactionEntity.toExpense(): Expense {
    val parsedType = runCatching { TransactionType.valueOf(type) }
        .getOrDefault(TransactionType.EXPENSE)

    return Expense(
        id = id,
        title = title,
        amount = amount,
        category = category,
        date = date,
        monthYear = monthYear,
        type = parsedType,
        personName = personName,
        note = note,
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
        note = note,
        timestamp = timestamp
    )
}