package com.yugentech.mynt.split.service

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.mynt.split.model.SettledRecord
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class SettleMessageService(
    private val functions: FirebaseFunctions
) {

    suspend fun generate(record: SettledRecord, currencySymbol: String): Result<String> {
        return try {
            val data = hashMapOf<String, Any>(
                "settledOn" to record.settledOn.toString(),
                "currencySymbol" to currencySymbol,
                "people" to record.people,
                "bills" to record.bills.map { bill ->
                    hashMapOf<String, Any>(
                        "title" to bill.title,
                        "amount" to bill.amount,
                        "paidBy" to bill.paidBy,
                        "date" to bill.date.toString()
                    )
                },
                "transfers" to record.transfers.map { transfer ->
                    hashMapOf<String, Any>(
                        "from" to transfer.from,
                        "to" to transfer.to,
                        "amount" to transfer.amount
                    )
                }
            )

            val result = functions.getHttpsCallable("generateSettleMessage").call(data).await()
            val message = (result.data as? Map<*, *>)?.get("message") as? String

            if (message.isNullOrBlank()) Result.failure(Exception("Empty message"))
            else Result.success(message.trim())
        } catch (e: Exception) {
            Timber.e(e, "generateSettleMessage failed")
            Result.failure(e)
        }
    }
}