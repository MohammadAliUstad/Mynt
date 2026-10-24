package com.yugentech.mynt.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.yugentech.mynt.split.model.SettledRecord
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.split.model.SplitTransfer
import java.time.LocalDate

@Entity(tableName = "split_bills")
data class SplitBillEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val paidBy: String,
    val sharesJson: String,
    val dateIso: String
)

@Entity(tableName = "settled_records")
data class SettledRecordEntity(
    @PrimaryKey val id: String,
    val settledOnIso: String,
    val peopleJson: String,
    val billsJson: String,
    val transfersJson: String
)

// Configures Gson to safely convert Java's LocalDate.
// (The missing <LocalDate> on the Deserializer here is what broke your file!)
private val splitGson: Gson = GsonBuilder()
    .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ -> JsonPrimitive(src.toString()) })
    .registerTypeAdapter(LocalDate::class.java, JsonDeserializer<LocalDate> { json, _, _ -> LocalDate.parse(json.asString) })
    .create()

// Extension functions to easily convert between Room Entities and UI Models
fun SplitBillEntity.toModel(): SplitBill {
    val type = object : TypeToken<Map<String, Double>>() {}.type
    val shares = splitGson.fromJson(sharesJson, type) as? Map<String, Double> ?: emptyMap()
    return SplitBill(id, title, amount, paidBy, shares, LocalDate.parse(dateIso))
}

fun SplitBill.toEntity(): SplitBillEntity {
    return SplitBillEntity(id, title, amount, paidBy, splitGson.toJson(shares), date.toString())
}

fun SettledRecordEntity.toModel(): SettledRecord {
    val peopleType = object : TypeToken<List<String>>() {}.type
    val billsType = object : TypeToken<List<SplitBill>>() {}.type
    val transfersType = object : TypeToken<List<SplitTransfer>>() {}.type

    return SettledRecord(
        id = id,
        settledOn = LocalDate.parse(settledOnIso),
        people = splitGson.fromJson(peopleJson, peopleType) as? List<String> ?: emptyList(),
        bills = splitGson.fromJson(billsJson, billsType) as? List<SplitBill> ?: emptyList(),
        transfers = splitGson.fromJson(transfersJson, transfersType) as? List<SplitTransfer> ?: emptyList()
    )
}

fun SettledRecord.toEntity(): SettledRecordEntity {
    return SettledRecordEntity(
        id = id,
        settledOnIso = settledOn.toString(),
        peopleJson = splitGson.toJson(people),
        billsJson = splitGson.toJson(bills),
        transfersJson = splitGson.toJson(transfers)
    )
}