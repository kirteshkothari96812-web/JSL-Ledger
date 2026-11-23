package com.example.data

import com.example.domain.DayMeta
import com.example.domain.SalaryCalculator
import com.example.domain.WorkerMonthlySummary
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class JslRepository(private val dao: JslDao) {

    val workers: Flow<List<WorkerEntity>> = dao.observeWorkers()

    fun observeAttendanceForMonth(year: Int, month: Int): Flow<List<AttendanceEntity>> {
        val prefix = SalaryCalculator.monthPrefix(year, month)
        return dao.observeAttendanceForMonth(prefix)
    }

    fun observeAdvancesForMonth(year: Int, month: Int): Flow<List<AdvanceEntity>> {
        val prefix = SalaryCalculator.monthPrefix(year, month)
        return dao.observeAdvancesForMonth(prefix)
    }

    suspend fun saveWorker(worker: WorkerEntity): Long {
        return if (worker.id == 0L) {
            dao.insertWorker(worker)
        } else {
            dao.updateWorker(worker)
            worker.id
        }
    }

    suspend fun deleteWorker(worker: WorkerEntity) {
        dao.deleteWorker(worker)
    }

    suspend fun setAttendance(workerId: Long, dateKey: String, status: AttendanceStatus) {
        if (status == AttendanceStatus.UNMARKED) {
            dao.deleteAttendance(workerId, dateKey)
        } else {
            dao.upsertAttendance(AttendanceEntity(workerId = workerId, dateKey = dateKey, status = status))
        }
    }

    suspend fun markAllPresentForDate(workerIds: List<Long>, dateKey: String) {
        val items = workerIds.map { id ->
            AttendanceEntity(workerId = id, dateKey = dateKey, status = AttendanceStatus.PRESENT)
        }
        if (items.isNotEmpty()) {
            dao.upsertAttendances(items)
        }
    }

    suspend fun addAdvance(workerId: Long, dateKey: String, amount: Double, note: String) {
        dao.insertAdvance(
            AdvanceEntity(
                workerId = workerId,
                dateKey = dateKey,
                amount = amount,
                note = note.trim()
            )
        )
    }

    suspend fun deleteAdvance(advanceId: Long) {
        dao.deleteAdvanceById(advanceId)
    }

    suspend fun loadSampleFactoryData(year: Int, month: Int) {
        val dharmendra = WorkerEntity(
            name = "Dharmendra",
            roleOrNote = "Senior Machine Operator",
            payType = PayType.MONTHLY_26,
            monthlySalary = 20000.0,
            dailyRate = 0.0,
            useCustomDailyRate = false
        )
        val ramesh = WorkerEntity(
            name = "Ramesh Kumar",
            roleOrNote = "Welder (Custom Rate)",
            payType = PayType.MONTHLY_26,
            monthlySalary = 20280.0,
            dailyRate = 780.0,
            useCustomDailyRate = true
        )
        val suresh = WorkerEntity(
            name = "Suresh Yadav",
            roleOrNote = "Helper / Loader",
            payType = PayType.DAILY_WAGE,
            monthlySalary = 0.0,
            dailyRate = 450.0,
            useCustomDailyRate = false
        )
        val mukesh = WorkerEntity(
            name = "Mukesh Verma",
            roleOrNote = "Packaging Helper",
            payType = PayType.DAILY_WAGE,
            monthlySalary = 0.0,
            dailyRate = 500.0,
            useCustomDailyRate = false
        )

        val dId = dao.insertWorker(dharmendra)
        val rId = dao.insertWorker(ramesh)
        val sId = dao.insertWorker(suresh)
        val mId = dao.insertWorker(mukesh)

        val days = SalaryCalculator.getMonthDays(year, month)
        val attendanceList = mutableListOf<AttendanceEntity>()

        for (day in days) {
            val d = day.dayOfMonth
            if (d <= 27) {
                // Dharmendra: 26 P (worked 1 Sunday, off other Sundays, 1 A)
                if (!day.isSunday) {
                    val status = if (d == 14) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
                    attendanceList.add(AttendanceEntity(dId, day.dateKey, status))
                } else if (d in 1..7) {
                    attendanceList.add(AttendanceEntity(dId, day.dateKey, AttendanceStatus.PRESENT))
                }

                // Ramesh: 22 P, 2 A
                if (!day.isSunday) {
                    val status = if (d == 6 || d == 19) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
                    attendanceList.add(AttendanceEntity(rId, day.dateKey, status))
                }

                // Suresh: 20 P, 3 A
                if (!day.isSunday) {
                    val status = if (d == 5 || d == 12 || d == 20) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
                    attendanceList.add(AttendanceEntity(sId, day.dateKey, status))
                }

                // Mukesh: 18 P + 1 Sunday P
                if (!day.isSunday && d <= 22) {
                    val status = if (d == 9) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
                    attendanceList.add(AttendanceEntity(mId, day.dateKey, status))
                } else if (day.isSunday && d in 8..14) {
                    attendanceList.add(AttendanceEntity(mId, day.dateKey, AttendanceStatus.PRESENT))
                }
            }
        }
        dao.upsertAttendances(attendanceList)

        dao.insertAdvance(
            AdvanceEntity(
                workerId = dId,
                dateKey = SalaryCalculator.dateKey(year, month, 2),
                amount = 2000.0,
                note = "Kharch / Home"
            )
        )
        dao.insertAdvance(
            AdvanceEntity(
                workerId = dId,
                dateKey = SalaryCalculator.dateKey(year, month, 10),
                amount = 2000.0,
                note = "Medical / Cash"
            )
        )
        dao.insertAdvance(
            AdvanceEntity(
                workerId = sId,
                dateKey = SalaryCalculator.dateKey(year, month, 8),
                amount = 1500.0,
                note = "Weekly Advance"
            )
        )
        dao.insertAdvance(
            AdvanceEntity(
                workerId = rId,
                dateKey = SalaryCalculator.dateKey(year, month, 15),
                amount = 3000.0,
                note = "Family Expense"
            )
        )
    }

    suspend fun exportToJson(): String {
        val workers = dao.getAllWorkersOnce()
        val attendance = dao.getAllAttendanceOnce()
        val advances = dao.getAllAdvancesOnce()

        val root = JSONObject().apply {
            put("app", "JSL Industries Ledger")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())

            val workersArr = JSONArray()
            workers.forEach { w ->
                workersArr.put(JSONObject().apply {
                    put("id", w.id)
                    put("name", w.name)
                    put("roleOrNote", w.roleOrNote)
                    put("payType", w.payType.name)
                    put("monthlySalary", w.monthlySalary)
                    put("dailyRate", w.dailyRate)
                    put("useCustomDailyRate", w.useCustomDailyRate)
                    put("createdAt", w.createdAt)
                })
            }
            put("workers", workersArr)

            val attArr = JSONArray()
            attendance.forEach { a ->
                attArr.put(JSONObject().apply {
                    put("workerId", a.workerId)
                    put("dateKey", a.dateKey)
                    put("status", a.status.name)
                })
            }
            put("attendance", attArr)

            val advArr = JSONArray()
            advances.forEach { adv ->
                advArr.put(JSONObject().apply {
                    put("id", adv.id)
                    put("workerId", adv.workerId)
                    put("dateKey", adv.dateKey)
                    put("amount", adv.amount)
                    put("note", adv.note)
                    put("createdAt", adv.createdAt)
                })
            }
            put("advances", advArr)
        }
        return root.toString(2)
    }

    suspend fun importFromJson(jsonText: String): Result<Int> {
        return runCatching {
            val root = JSONObject(jsonText)
            val workersArr = root.optJSONArray("workers")
                ?: throw IllegalArgumentException("Invalid backup file: missing 'workers'")
            val attArr = root.optJSONArray("attendance") ?: JSONArray()
            val advArr = root.optJSONArray("advances") ?: JSONArray()

            val workers = buildList {
                for (i in 0 until workersArr.length()) {
                    val obj = workersArr.getJSONObject(i)
                    add(
                        WorkerEntity(
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            roleOrNote = obj.optString("roleOrNote", ""),
                            payType = runCatching {
                                PayType.valueOf(obj.getString("payType"))
                            }.getOrDefault(PayType.DAILY_WAGE),
                            monthlySalary = obj.optDouble("monthlySalary", 0.0),
                            dailyRate = obj.optDouble("dailyRate", 0.0),
                            useCustomDailyRate = obj.optBoolean("useCustomDailyRate", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            dao.replaceAllData(
                workers = workers,
                attendanceBuilder = { idMap ->
                    buildList {
                        for (i in 0 until attArr.length()) {
                            val obj = attArr.getJSONObject(i)
                            val newWorkerId = idMap[obj.getLong("workerId")] ?: continue
                            val status = runCatching {
                                AttendanceStatus.valueOf(obj.getString("status"))
                            }.getOrDefault(AttendanceStatus.UNMARKED)
                            if (status != AttendanceStatus.UNMARKED) {
                                add(
                                    AttendanceEntity(
                                        workerId = newWorkerId,
                                        dateKey = obj.getString("dateKey"),
                                        status = status
                                    )
                                )
                            }
                        }
                    }
                },
                advancesBuilder = { idMap ->
                    buildList {
                        for (i in 0 until advArr.length()) {
                            val obj = advArr.getJSONObject(i)
                            val newWorkerId = idMap[obj.getLong("workerId")] ?: continue
                            add(
                                AdvanceEntity(
                                    workerId = newWorkerId,
                                    dateKey = obj.getString("dateKey"),
                                    amount = obj.optDouble("amount", 0.0),
                                    note = obj.optString("note", ""),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                }
            )
            workers.size
        }
    }

    fun buildMonthlyCsv(
        year: Int,
        month: Int,
        days: List<DayMeta>,
        summaries: List<WorkerMonthlySummary>,
        attendanceLookup: Map<Long, Map<String, AttendanceStatus>>
    ): String {
        val sb = StringBuilder()
        val monthTitle = "${SalaryCalculator.monthName(month)} $year"
        sb.appendLine("JSL Industries - Monthly Attendance & Salary Sheet ($monthTitle)")

        val headers = mutableListOf(
            "Worker Name",
            "Role",
            "Pay Type",
            "Monthly Salary",
            "Effective Per-Day Rate",
            "Present Days",
            "Sunday P Days",
            "Absent Days",
            "Earned Salary",
            "Total Advance",
            "Net Payable"
        )
        days.forEach { day ->
            headers.add(if (day.isSunday) "${day.dayOfMonth}(Sun)" else "${day.dayOfMonth}")
        }
        sb.appendLine(headers.joinToString(",") { escapeCsv(it) })

        summaries.forEach { s ->
            val workerAtt = attendanceLookup[s.worker.id].orEmpty()
            val row = mutableListOf(
                s.worker.name,
                s.worker.roleOrNote,
                s.worker.payType.label,
                String.format(Locale.US, "%.2f", s.worker.monthlySalary),
                String.format(Locale.US, "%.2f", s.effectiveDailyRate),
                s.presentDays.toString(),
                s.sundayPresentDays.toString(),
                s.absentDays.toString(),
                String.format(Locale.US, "%.2f", s.earnedSalary),
                String.format(Locale.US, "%.2f", s.totalAdvance),
                String.format(Locale.US, "%.2f", s.netPayable)
            )
            days.forEach { day ->
                row.add((workerAtt[day.dateKey] ?: AttendanceStatus.UNMARKED).code)
            }
            sb.appendLine(row.joinToString(",") { escapeCsv(it) })
        }
        return sb.toString()
    }

    fun buildPrintableHtml(
        year: Int,
        month: Int,
        summaries: List<WorkerMonthlySummary>
    ): String {
        val monthTitle = "${SalaryCalculator.monthName(month)} $year"
        val totalGross = summaries.sumOf { it.earnedSalary }
        val totalAdvance = summaries.sumOf { it.totalAdvance }
        val totalNet = summaries.sumOf { it.netPayable }

        val rowsHtml = summaries.joinToString("\n") { s ->
            val advNotes = if (s.advances.isEmpty()) {
                "—"
            } else {
                s.advances.joinToString("<br/>") { adv ->
                    val noteSuffix = if (adv.note.isNotBlank()) " (${escapeHtml(adv.note)})" else ""
                    "${SalaryCalculator.formatReadableDate(adv.dateKey)}: ${SalaryCalculator.formatInr(adv.amount)}$noteSuffix"
                }
            }
            val sunBadge = if (s.sundayPresentDays > 0) " (${s.sundayPresentDays} Sun)" else ""
            """
            <tr>
              <td><strong>${escapeHtml(s.worker.name)}</strong><br/><small>${escapeHtml(s.worker.payType.shortLabel)}</small></td>
              <td class="num">${s.presentDays}$sunBadge</td>
              <td class="num">${s.absentDays}</td>
              <td class="num">${SalaryCalculator.formatInr(s.effectiveDailyRate)}</td>
              <td class="num">${SalaryCalculator.formatInr(s.earnedSalary)}</td>
              <td class="num">${SalaryCalculator.formatInr(s.totalAdvance)}<br/><small>$advNotes</small></td>
              <td class="num net"><strong>${SalaryCalculator.formatInr(s.netPayable)}</strong></td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8"/>
          <style>
            body { font-family: sans-serif; color: #181C20; padding: 20px; }
            h1 { margin: 0; color: #173559; font-size: 22px; }
            .sub { color: #5A6169; margin: 4px 0 16px 0; font-size: 13px; }
            .totals { display: flex; gap: 16px; margin-bottom: 16px; }
            .box { border: 1px solid #D6D0C4; border-radius: 6px; padding: 10px 14px; background: #F6F4EF; }
            .box-label { font-size: 11px; color: #5A6169; text-transform: uppercase; }
            .box-val { font-size: 16px; font-weight: bold; color: #173559; margin-top: 2px; }
            table { width: 100%; border-collapse: collapse; font-size: 12px; }
            th { background: #173559; color: #FFFFFF; text-align: left; padding: 8px; }
            td { border-bottom: 1px solid #E2DDD3; padding: 8px; vertical-align: top; }
            th.num, td.num { text-align: right; }
            td.net { color: #15803D; font-size: 13px; }
            small { color: #5A6169; font-size: 10px; }
          </style>
        </head>
        <body>
          <h1>JSL Industries — Salary &amp; Advance Ledger</h1>
          <div class="sub">Month: $monthTitle • Total Workers: ${summaries.size}</div>
          <div class="totals">
            <div class="box"><div class="box-label">Total Earned</div><div class="box-val">${SalaryCalculator.formatInr(totalGross)}</div></div>
            <div class="box"><div class="box-label">Total Advance</div><div class="box-val">${SalaryCalculator.formatInr(totalAdvance)}</div></div>
            <div class="box"><div class="box-label">Net Payable</div><div class="box-val">${SalaryCalculator.formatInr(totalNet)}</div></div>
          </div>
          <table>
            <thead>
              <tr>
                <th>Worker</th>
                <th class="num">Present (P)</th>
                <th class="num">Absent (A)</th>
                <th class="num">Rate/Day</th>
                <th class="num">Earned</th>
                <th class="num">Advances</th>
                <th class="num">Net Payable</th>
              </tr>
            </thead>
            <tbody>
              $rowsHtml
            </tbody>
          </table>
        </body>
        </html>
        """.trimIndent()
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
