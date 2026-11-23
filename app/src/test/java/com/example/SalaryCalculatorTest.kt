package com.example

import com.example.data.AdvanceEntity
import com.example.data.AttendanceStatus
import com.example.data.PayType
import com.example.data.WorkerEntity
import com.example.domain.SalaryCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SalaryCalculatorTest {

    @Test
    fun monthly26DayWorker_withSundayPresentAndAdvances_calculatesExactNetPayable() {
        val dharmendra = WorkerEntity(
            id = 1L,
            name = "Dharmendra",
            payType = PayType.MONTHLY_26,
            monthlySalary = 20000.0,
            useCustomDailyRate = false
        )
        val days = SalaryCalculator.getMonthDays(2026, 11) // 30 days, Nov 1 2026 is Sunday
        assertEquals(30, days.size)
        assertTrue(days.first().isSunday)

        val attendance = mutableMapOf<String, AttendanceStatus>()
        // Mark 24 non-Sunday days + 2 Sundays (Nov 1 & Nov 8) as Present = 26 P days, and 1 Absent
        var weekdayP = 0
        var absentMarked = false
        for (day in days) {
            if (day.isSunday && (day.dayOfMonth == 1 || day.dayOfMonth == 8)) {
                attendance[day.dateKey] = AttendanceStatus.PRESENT
            } else if (!day.isSunday && weekdayP < 24) {
                attendance[day.dateKey] = AttendanceStatus.PRESENT
                weekdayP++
            } else if (!day.isSunday && !absentMarked) {
                attendance[day.dateKey] = AttendanceStatus.ABSENT
                absentMarked = true
            }
        }

        val advances = listOf(
            AdvanceEntity(id = 1, workerId = 1, dateKey = "2026-11-02", amount = 2000.0, note = "Home"),
            AdvanceEntity(id = 2, workerId = 1, dateKey = "2026-11-10", amount = 2000.0, note = "Medical")
        )

        val summary = SalaryCalculator.calculateWorkerMonthlySummary(
            worker = dharmendra,
            year = 2026,
            month = 11,
            days = days,
            attendanceByDate = attendance,
            workerAdvances = advances
        )

        assertEquals(26, summary.presentDays)
        assertEquals(2, summary.sundayPresentDays)
        assertEquals(1, summary.absentDays)
        assertEquals(20000.0, summary.earnedSalary, 0.01)
        assertEquals(4000.0, summary.totalAdvance, 0.01)
        assertEquals(16000.0, summary.netPayable, 0.01)
    }

    @Test
    fun monthlyWorkerWithCustomDailyRateOverride_usesCustomRate() {
        val worker = WorkerEntity(
            id = 2L,
            name = "Ramesh",
            payType = PayType.MONTHLY_26,
            monthlySalary = 20000.0,
            dailyRate = 780.0,
            useCustomDailyRate = true
        )
        val days = SalaryCalculator.getMonthDays(2026, 11)
        val attendance = days.take(22).associate { it.dateKey to AttendanceStatus.PRESENT }

        val summary = SalaryCalculator.calculateWorkerMonthlySummary(
            worker = worker,
            year = 2026,
            month = 11,
            days = days,
            attendanceByDate = attendance,
            workerAdvances = emptyList()
        )

        assertEquals(780.0, summary.effectiveDailyRate, 0.001)
        assertEquals(17160.0, summary.earnedSalary, 0.01)
        assertEquals(17160.0, summary.netPayable, 0.01)
    }

    @Test
    fun dailyWageWorker_calculatesPresentTimesRate() {
        val suresh = WorkerEntity(
            id = 3L,
            name = "Suresh",
            payType = PayType.DAILY_WAGE,
            dailyRate = 450.0
        )
        val days = SalaryCalculator.getMonthDays(2026, 2) // 28 days in non-leap 2026
        assertEquals(28, days.size)

        val attendance = days.take(20).associate { it.dateKey to AttendanceStatus.PRESENT }
        val summary = SalaryCalculator.calculateWorkerMonthlySummary(
            worker = suresh,
            year = 2026,
            month = 2,
            days = days,
            attendanceByDate = attendance,
            workerAdvances = emptyList()
        )

        assertEquals(20, summary.presentDays)
        assertEquals(9000.0, summary.earnedSalary, 0.01)
        assertEquals(9000.0, summary.netPayable, 0.01)
    }

    @Test
    fun indianCurrencyFormatting_handlesStandardAndLargeNumbers() {
        assertEquals("₹20,000", SalaryCalculator.formatInr(20000.0))
        assertEquals("₹1,25,000", SalaryCalculator.formatInr(125000.0))
        assertEquals("₹769.23", SalaryCalculator.formatInr(20000.0 / 26.0))
        assertEquals("-₹500", SalaryCalculator.formatInr(-500.0))
    }
}
