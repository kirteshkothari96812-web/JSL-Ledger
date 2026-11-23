package com.example.domain

import com.example.data.AdvanceEntity
import com.example.data.AttendanceStatus
import com.example.data.PayType
import com.example.data.WorkerEntity
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class DayMeta(
    val dayOfMonth: Int,
    val dateKey: String,
    val isSunday: Boolean,
    val dayShortName: String
)

data class WorkerMonthlySummary(
    val worker: WorkerEntity,
    val year: Int,
    val month: Int,
    val presentDays: Int,
    val sundayPresentDays: Int,
    val absentDays: Int,
    val unmarkedDays: Int,
    val effectiveDailyRate: Double,
    val earnedSalary: Double,
    val full26DayReference: Double?,
    val totalAdvance: Double,
    val netPayable: Double,
    val formulaBreakdown: String,
    val advances: List<AdvanceEntity>
)

object SalaryCalculator {

    private val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val shortMonthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    private val weekdayShort = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    fun monthName(month: Int): String = monthNames.getOrElse(month - 1) { "Month" }

    fun shortMonthName(month: Int): String = shortMonthNames.getOrElse(month - 1) { "Mon" }

    fun monthPrefix(year: Int, month: Int): String =
        String.format(Locale.US, "%04d-%02d-", year, month)

    fun dateKey(year: Int, month: Int, day: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getMonthDays(year: Int, month: Int): List<DayMeta> {
        val total = daysInMonth(year, month)
        val cal = Calendar.getInstance()
        return (1..total).map { day ->
            cal.clear()
            cal.set(year, month - 1, day)
            val dow = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sun .. 7 = Sat
            DayMeta(
                dayOfMonth = day,
                dateKey = dateKey(year, month, day),
                isSunday = dow == Calendar.SUNDAY,
                dayShortName = weekdayShort.getOrElse(dow - 1) { "" }
            )
        }
    }

    fun isDateKeySunday(dateKey: String): Boolean {
        val parts = dateKey.split("-")
        if (parts.size != 3) return false
        val y = parts[0].toIntOrNull() ?: return false
        val m = parts[1].toIntOrNull() ?: return false
        val d = parts[2].toIntOrNull() ?: return false
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(y, m - 1, d)
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    }

    fun formatReadableDate(dateKey: String): String {
        val parts = dateKey.split("-")
        if (parts.size != 3) return dateKey
        val y = parts[0].toIntOrNull() ?: return dateKey
        val m = parts[1].toIntOrNull() ?: return dateKey
        val d = parts[2].toIntOrNull() ?: return dateKey
        return String.format(Locale.US, "%02d %s %04d", d, shortMonthName(m), y)
    }

    fun effectiveDailyRate(worker: WorkerEntity): Double {
        return when (worker.payType) {
            PayType.MONTHLY_26 -> {
                if (worker.useCustomDailyRate && worker.dailyRate > 0.0) {
                    worker.dailyRate
                } else {
                    worker.monthlySalary / 26.0
                }
            }
            PayType.DAILY_WAGE -> worker.dailyRate
        }
    }

    fun calculateWorkerMonthlySummary(
        worker: WorkerEntity,
        year: Int,
        month: Int,
        days: List<DayMeta>,
        attendanceByDate: Map<String, AttendanceStatus>,
        workerAdvances: List<AdvanceEntity>
    ): WorkerMonthlySummary {
        var presentDays = 0
        var sundayPresentDays = 0
        var absentDays = 0
        var unmarkedDays = 0

        for (day in days) {
            when (attendanceByDate[day.dateKey] ?: AttendanceStatus.UNMARKED) {
                AttendanceStatus.PRESENT -> {
                    presentDays++
                    if (day.isSunday) sundayPresentDays++
                }
                AttendanceStatus.ABSENT -> absentDays++
                AttendanceStatus.UNMARKED -> unmarkedDays++
            }
        }

        val rate = effectiveDailyRate(worker)
        val earnedSalary = presentDays * rate
        val full26DayReference = if (worker.payType == PayType.MONTHLY_26) {
            if (worker.useCustomDailyRate && worker.dailyRate > 0.0) {
                rate * 26.0
            } else {
                worker.monthlySalary
            }
        } else {
            null
        }
        val totalAdvance = workerAdvances.sumOf { it.amount }
        val netPayable = earnedSalary - totalAdvance

        val rateFormatted = formatInr(rate)
        val breakdown = when (worker.payType) {
            PayType.MONTHLY_26 -> {
                if (worker.useCustomDailyRate && worker.dailyRate > 0.0) {
                    "$presentDays P × $rateFormatted/day (Custom override)"
                } else {
                    "$presentDays P × $rateFormatted/day (${formatInr(worker.monthlySalary)} ÷ 26)"
                }
            }
            PayType.DAILY_WAGE -> "$presentDays P × $rateFormatted/day"
        }

        return WorkerMonthlySummary(
            worker = worker,
            year = year,
            month = month,
            presentDays = presentDays,
            sundayPresentDays = sundayPresentDays,
            absentDays = absentDays,
            unmarkedDays = unmarkedDays,
            effectiveDailyRate = rate,
            earnedSalary = earnedSalary,
            full26DayReference = full26DayReference,
            totalAdvance = totalAdvance,
            netPayable = netPayable,
            formulaBreakdown = breakdown,
            advances = workerAdvances
        )
    }

    fun formatInr(amount: Double, forceDecimals: Boolean = false): String {
        val isNegative = amount < -0.005
        val absVal = abs(amount)
        val roundedTwoDecimals = (absVal * 100.0).roundToLong() / 100.0
        val whole = roundedTwoDecimals.toLong()
        val frac = ((roundedTwoDecimals - whole) * 100.0).roundToLong().toInt()

        val groupedWhole = formatIndianInteger(whole)
        val numberPart = if (forceDecimals || frac > 0) {
            String.format(Locale.US, "%s.%02d", groupedWhole, frac)
        } else {
            groupedWhole
        }
        return if (isNegative) "-₹$numberPart" else "₹$numberPart"
    }

    private fun formatIndianInteger(value: Long): String {
        val str = value.toString()
        if (str.length <= 3) return str
        val lastThree = str.substring(str.length - 3)
        var rest = str.substring(0, str.length - 3)
        val sb = StringBuilder()
        while (rest.length > 2) {
            sb.insert(0, "," + rest.substring(rest.length - 2))
            rest = rest.substring(0, rest.length - 2)
        }
        if (rest.isNotEmpty()) {
            sb.insert(0, rest)
        }
        return sb.append(",").append(lastThree).toString()
    }
}
