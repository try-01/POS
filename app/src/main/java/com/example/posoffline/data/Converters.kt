package com.example.posoffline.data

import androidx.room.TypeConverter
import com.example.posoffline.data.entity.TransactionItem
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters. Kept tiny: just the JSON serializer for the
 * transaction items list. Everything else is a primitive that Room
 * handles natively.
 */
class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val listSerializer = ListSerializer(TransactionItem.serializer())

    @TypeConverter
    fun itemsToJson(items: List<TransactionItem>): String =
        json.encodeToString(listSerializer, items)

    @TypeConverter
    fun jsonToItems(raw: String): List<TransactionItem> =
        json.decodeFromString(listSerializer, raw)
}
