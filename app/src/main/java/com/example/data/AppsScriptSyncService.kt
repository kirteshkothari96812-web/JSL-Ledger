package com.example.data

import android.content.Context
import com.example.domain.DayMeta
import com.example.domain.SalaryCalculator
import com.example.domain.WorkerMonthlySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class SheetSyncConfig(
    val webAppUrl: String = "",
    val autoPush: Boolean = false,
    val lastSyncMessage: String = ""
)

class AppsScriptSyncService(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "jsl_sheet_sync_prefs",
        Context.MODE_PRIVATE
    )

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .build()

    fun loadConfig(): SheetSyncConfig {
        return SheetSyncConfig(
            webAppUrl = prefs.getString(KEY_WEB_APP_URL, "") ?: "",
            autoPush = prefs.getBoolean(KEY_AUTO_PUSH, false),
            lastSyncMessage = prefs.getString(KEY_LAST_SYNC_MSG, "") ?: ""
        )
    }

    fun saveWebAppUrl(url: String) {
        prefs.edit().putString(KEY_WEB_APP_URL, url.trim()).apply()
    }

    fun saveAutoPush(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PUSH, enabled).apply()
    }

    fun saveLastSyncMessage(message: String) {
        prefs.edit().putString(KEY_LAST_SYNC_MSG, message).apply()
    }

    suspend fun pushToSheet(
        webAppUrl: String,
        year: Int,
        month: Int,
        days: List<DayMeta>,
        summaries: List<WorkerMonthlySummary>,
        attendanceLookup: Map<Long, Map<String, AttendanceStatus>>,
        advancesForMonth: List<AdvanceEntity>,
        rawBackupJson: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUrl = validateAppsScriptUrl(webAppUrl)
            val monthTitle = "${SalaryCalculator.monthName(month)} $year"
            val monthSlug = String.format(Locale.US, "%04d_%02d", year, month)
            val workerById = summaries.associate { it.worker.id to it.worker.name }

            val salaryRows = JSONArray().apply {
                put(
                    JSONArray(
                        listOf(
                            "Worker Name",
                            "Role",
                            "Pay Type",
                            "Monthly Salary (₹)",
                            "Rate / Day (₹)",
                            "Present (P)",
                            "Sunday P",
                            "Absent (A)",
                            "Earned Salary (₹)",
                            "Total Advance (₹)",
                            "Net Payable (₹)"
                        )
                    )
                )
                summaries.forEach { s ->
                    put(
                        JSONArray(
                            listOf(
                                s.worker.name,
                                s.worker.roleOrNote,
                                s.worker.payType.shortLabel,
                                roundTwo(s.worker.monthlySalary),
                                roundTwo(s.effectiveDailyRate),
                                s.presentDays,
                                s.sundayPresentDays,
                                s.absentDays,
                                roundTwo(s.earnedSalary),
                                roundTwo(s.totalAdvance),
                                roundTwo(s.netPayable)
                            )
                        )
                    )
                }
                // Totals row
                put(
                    JSONArray(
                        listOf(
                            "TOTAL (${summaries.size})",
                            "",
                            "",
                            "",
                            "",
                            summaries.sumOf { it.presentDays },
                            summaries.sumOf { it.sundayPresentDays },
                            summaries.sumOf { it.absentDays },
                            roundTwo(summaries.sumOf { it.earnedSalary }),
                            roundTwo(summaries.sumOf { it.totalAdvance }),
                            roundTwo(summaries.sumOf { it.netPayable })
                        )
                    )
                )
            }

            val attendanceRows = JSONArray().apply {
                val header = mutableListOf("Worker Name", "Present (P)", "Absent (A)")
                days.forEach { d ->
                    header.add(if (d.isSunday) "${d.dayOfMonth} (Sun)" else "${d.dayOfMonth}")
                }
                put(JSONArray(header))

                summaries.forEach { s ->
                    val attMap = attendanceLookup[s.worker.id].orEmpty()
                    val row = mutableListOf<Any>(s.worker.name, s.presentDays, s.absentDays)
                    days.forEach { d ->
                        row.add((attMap[d.dateKey] ?: AttendanceStatus.UNMARKED).code)
                    }
                    put(JSONArray(row))
                }
            }

            val advanceRows = JSONArray().apply {
                put(JSONArray(listOf("Date", "Worker Name", "Amount (₹)", "Note")))
                advancesForMonth.forEach { adv ->
                    put(
                        JSONArray(
                            listOf(
                                adv.dateKey,
                                workerById[adv.workerId] ?: "Worker",
                                roundTwo(adv.amount),
                                adv.note
                            )
                        )
                    )
                }
            }

            val payload = JSONObject().apply {
                put("action", "sync_all")
                put("company", "JSL Industries")
                put("monthTitle", monthTitle)
                put("monthSlug", monthSlug)
                put("salaryRows", salaryRows)
                put("attendanceRows", attendanceRows)
                put("advanceRows", advanceRows)
                put("rawBackupJson", rawBackupJson)
            }

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(cleanUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val respText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}: ${respText.take(180)}")
                }
                val parsed = runCatching { JSONObject(respText) }.getOrNull()
                if (parsed != null && !parsed.optBoolean("ok", true)) {
                    error(parsed.optString("error", "Apps Script returned an error"))
                }
                parsed?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: "Updated Google Sheet ($monthTitle)"
            }
        }
    }

    suspend fun pullFromSheet(webAppUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUrl = validateAppsScriptUrl(webAppUrl)
            val separator = if (cleanUrl.contains("?")) "&" else "?"
            val request = Request.Builder()
                .url("${cleanUrl}${separator}action=pull_backup")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val respText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}: ${respText.take(180)}")
                }
                val json = runCatching { JSONObject(respText) }.getOrNull()
                    ?: error("Unexpected response from Apps Script. Make sure deployment access is set to 'Anyone'.")
                if (!json.optBoolean("ok", false)) {
                    error(json.optString("error", "No sync data found in Google Sheet yet."))
                }
                val backupJson = json.optString("backupJson", "")
                if (backupJson.isBlank()) {
                    error("Google Sheet has no backup data yet. Push from one phone first.")
                }
                backupJson
            }
        }
    }

    private fun validateAppsScriptUrl(url: String): String {
        val trimmed = url.trim()
        require(trimmed.isNotEmpty()) { "Please enter your Google Apps Script Web App URL first." }
        require(trimmed.startsWith("https://")) { "URL must start with https://" }
        if (trimmed.contains("docs.google.com/spreadsheets")) {
            error("Please paste the deployed Apps Script Web App URL (script.google.com/macros/s/.../exec), not the spreadsheet link.")
        }
        return trimmed
    }

    private fun roundTwo(value: Double): Double = Math.round(value * 100.0) / 100.0

    companion object {
        private const val KEY_WEB_APP_URL = "web_app_url"
        private const val KEY_AUTO_PUSH = "auto_push"
        private const val KEY_LAST_SYNC_MSG = "last_sync_msg"

        val APPS_SCRIPT_CODE: String = """
function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var slug = data.monthSlug || "Current";

    writeFormattedSheet_(ss, "Salary_" + slug, data.salaryRows || [], "#173559");
    writeFormattedSheet_(ss, "Attendance_" + slug, data.attendanceRows || [], "#173559");
    writeFormattedSheet_(ss, "Advances_" + slug, data.advanceRows || [], "#92400E");

    if (data.rawBackupJson) {
      writeRawBackup_(ss, data.rawBackupJson);
    }

    return jsonResponse_({
      ok: true,
      message: "Synced " + (data.monthTitle || slug) + " to Google Sheet!",
      updatedAt: new Date().toISOString()
    });
  } catch (err) {
    return jsonResponse_({ ok: false, error: err.toString() });
  }
}

function doGet(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("_JSL_Sync_Data");
    if (!sheet) {
      return jsonResponse_({
        ok: false,
        error: "No sync data found yet. Push from one phone first."
      });
    }
    var lastRow = sheet.getLastRow();
    if (lastRow < 2) {
      return jsonResponse_({ ok: false, error: "Sync tab is empty." });
    }
    var updatedAt = sheet.getRange(1, 2).getValue();
    var values = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    var combined = "";
    for (var i = 0; i < values.length; i++) {
      combined += String(values[i][0] || "");
    }
    return jsonResponse_({
      ok: true,
      backupJson: combined,
      updatedAt: String(updatedAt || "")
    });
  } catch (err) {
    return jsonResponse_({ ok: false, error: err.toString() });
  }
}

function writeFormattedSheet_(ss, sheetName, rows, headerHex) {
  if (!rows || rows.length === 0) return;
  var sheet = ss.getSheetByName(sheetName) || ss.insertSheet(sheetName);
  sheet.clearContents();
  var numRows = rows.length;
  var numCols = rows[0].length;
  sheet.getRange(1, 1, numRows, numCols).setValues(rows);

  var headerRange = sheet.getRange(1, 1, 1, numCols);
  headerRange.setBackground(headerHex).setFontColor("#FFFFFF").setFontWeight("bold");
  sheet.setFrozenRows(1);

  for (var c = 1; c <= numCols; c++) {
    var headerVal = String(rows[0][c - 1] || "");
    if (headerVal.indexOf("(Sun)") !== -1) {
      sheet.getRange(1, c, numRows, 1).setBackground("#FEF3C7").setFontColor("#92400E");
      sheet.getRange(1, c).setBackground("#D97706").setFontColor("#FFFFFF");
    }
  }
  sheet.autoResizeColumns(1, numCols);
}

function writeRawBackup_(ss, jsonStr) {
  var sheet = ss.getSheetByName("_JSL_Sync_Data") || ss.insertSheet("_JSL_Sync_Data");
  sheet.clearContents();
  sheet.getRange(1, 1, 1, 2).setValues([["UpdatedAt", new Date().toISOString()]]);
  var chunkSize = 40000;
  var chunks = [];
  for (var i = 0; i < jsonStr.length; i += chunkSize) {
    chunks.push([jsonStr.substring(i, i + chunkSize)]);
  }
  if (chunks.length > 0) {
    sheet.getRange(2, 1, chunks.length, 1).setValues(chunks);
  }
}

function jsonResponse_(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
        """.trimIndent()
    }
}
