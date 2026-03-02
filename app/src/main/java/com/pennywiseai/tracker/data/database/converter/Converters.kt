


package com.pennywiseai.tracker.data.database.converter

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date

class Converters {

    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        private val dateOnlyFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    // Date converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // LocalDateTime converters
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, dateFormatter) }
    }

    // LocalDate converters
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(dateOnlyFormatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateOnlyFormatter) }
    }

    // BigDecimal converters
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }

    // String converters (safe implementation)
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }

    // Boolean converters
    @TypeConverter
    fun fromBoolean(value: Boolean?): Int? {
        return value?.let { if (it) 1 else 0 }
    }

    @TypeConverter
    fun toBoolean(value: Int?): Boolean? {
        return value?.let { it == 1 }
    }

    // Integer converters for nullable integers
    @TypeConverter
    fun fromInt(value: Int?): Long? {
        return value?.toLong()
    }

    @TypeConverter
    fun toInt(value: Long?): Int? {
        return value?.toInt()
    }
}
