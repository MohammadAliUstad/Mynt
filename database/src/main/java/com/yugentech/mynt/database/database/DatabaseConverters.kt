package com.yugentech.mynt.database.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yugentech.mynt.split.model.SplitBill
import com.yugentech.mynt.split.model.SplitTransfer

class DatabaseConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringMap(value: Map<String, Double>): String = gson.toJson(value)

    @TypeConverter
    fun toStringMap(value: String): Map<String, Double> {
        val type = object : TypeToken<Map<String, Double>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromBillList(value: List<SplitBill>): String = gson.toJson(value)

    @TypeConverter
    fun toBillList(value: String): List<SplitBill> {
        val type = object : TypeToken<List<SplitBill>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromTransferList(value: List<SplitTransfer>): String = gson.toJson(value)

    @TypeConverter
    fun toTransferList(value: String): List<SplitTransfer> {
        val type = object : TypeToken<List<SplitTransfer>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}