package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PayType(val label: String, val shortLabel: String) {
    MONTHLY_26("Monthly (26-Day Basis)", "26-Day Monthly"),
    DAILY_WAGE("Daily Wage", "Daily Wage")
}

enum class AttendanceStatus(val code: String, val label: String) {
    UNMARKED("", "Unmarked"),
    PRESENT("P", "Present"),
    ABSENT("A", "Absent")
}

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val roleOrNote: String = "",
    val payType: PayType,
    val monthlySalary: Double = 0.0,
    val dailyRate: Double = 0.0,
    val useCustomDailyRate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance",
    primaryKeys = ["workerId", "dateKey"],
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workerId"]),
        Index(value = ["dateKey"])
    ]
)
data class AttendanceEntity(
    val workerId: Long,
    val dateKey: String, // YYYY-MM-DD
    val status: AttendanceStatus
)

@Entity(
    tableName = "advances",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workerId"]),
        Index(value = ["dateKey"])
    ]
)
data class AdvanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    val dateKey: String, // YYYY-MM-DD
    val amount: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
