package com.example.data

import androidx.room.TypeConverter

class JslConverters {
    @TypeConverter
    fun fromPayType(value: PayType): String = value.name

    @TypeConverter
    fun toPayType(value: String): PayType =
        runCatching { PayType.valueOf(value) }.getOrDefault(PayType.DAILY_WAGE)

    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus): String = value.name

    @TypeConverter
    fun toAttendanceStatus(value: String): AttendanceStatus =
        runCatching { AttendanceStatus.valueOf(value) }.getOrDefault(AttendanceStatus.UNMARKED)
}
