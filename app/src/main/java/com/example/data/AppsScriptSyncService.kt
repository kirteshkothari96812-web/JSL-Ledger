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
    val lastSyncMessage: String = "",
    val lastSyncSuccess: Boolean? = null
)

class AppsScriptSyncService(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "jsl_sheet_sync_prefs",
        Context.MODE_PRIVATE
    )

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun loadConfig(): SheetSyncConfig {
        val hasSuccess = prefs.contains(KEY_LAST_SYNC_OK)
        return SheetSyncConfig(
            webAppUrl = prefs.getString(KEY_WEB_APP_URL, "") ?: "",
            autoPush = prefs.getBoolean(KEY_AUTO_PUSH, false),
            lastSyncMessage = prefs.getString(KEY_LAST_SYNC_MSG, "") ?: "",
            lastSyncSuccess = if (hasSuccess) prefs.getBoolean(KEY_LAST_SYNC_OK, false) else null
        )
    }

    fun saveWebAppUrl(url: String) {
        val normalized = normalizeInputUrl(url)
        prefs.edit().putString(KEY_WEB_APP_URL, normalized).apply()
    }

    fun saveAutoPush(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PUSH, enabled).apply()
    }

    fun saveLastSyncStatus(message: String, success: Boolean) {
        prefs.edit()
            .putString(KEY_LAST_SYNC_MSG, message)
            .putBoolean(KEY_LAST_SYNC_OK, success)
            .apply()
    }

    suspend fun testConnection(webAppUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUrl = validateAppsScriptUrl(webAppUrl)
            val separator = if (cleanUrl.contains("?")) "&" else "?"
            val request = Request.Builder()
                .url("${cleanUrl}${separator}action=ping")
                .header("Accept", "application/json")
                .header("User-Agent", "JSL-Ledger-Android/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val respText = response.body?.string().orEmpty()
                validateHttpResponse(response.code, finalUrl, respText)

                val json = parseJsonOrThrow(respText, finalUrl)
                val ok = json.optBoolean("ok", false)
                val err = json.optString("error", "")
                if (ok) {
                    json.optString("message").takeIf { it.isNotBlank() }
                        ?: "Connected! Google Sheet sync ready hai."
                } else if (err.contains("No sync data found", ignoreCase = true) ||
                    err.contains("Sync tab is empty", ignoreCase = true)
                ) {
                    "Link sahi hai! Sheet connected hai — pehle 1 worker add karke 'Push Sheet' dabaye."
                } else {
                    error(err.ifBlank { "Apps Script returned an error." })
                }
            }
        }
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
                .header("Accept", "application/json")
                .header("User-Agent", "JSL-Ledger-Android/1.0")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val respText = response.body?.string().orEmpty()
                validateHttpResponse(response.code, finalUrl, respText)

                val parsed = parseJsonOrThrow(respText, finalUrl)
                if (!parsed.optBoolean("ok", true)) {
                    error(parsed.optString("error", "Apps Script returned an error"))
                }
                parsed.optString("message").takeIf { it.isNotBlank() }
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
                .header("Accept", "application/json")
                .header("User-Agent", "JSL-Ledger-Android/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val respText = response.body?.string().orEmpty()
                validateHttpResponse(response.code, finalUrl, respText)

                val json = parseJsonOrThrow(respText, finalUrl)
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

    private fun validateHttpResponse(code: Int, finalUrl: String, respText: String) {
        if (finalUrl.contains("accounts.google.com", ignoreCase = true) ||
            respText.contains("ServiceLogin", ignoreCase = true) ||
            respText.contains("accounts.google.com/v3/signin", ignoreCase = true)
        ) {
            error("Access denied! Apps Script me Deploy → Manage deployments → Edit (pencil) → 'Who has access' ko 'Anyone' set karke Update karein.")
        }
        if (code == 401 || code == 403) {
            error("HTTP $code: Access denied. Apps Script deployment me 'Who has access: Anyone' select karein.")
        }
        if (code == 404) {
            error("HTTP 404: Deployment URL galat ya delete ho chuka hai. Naya Web app deploy karke /exec URL paste karein.")
        }
        if (code !in 200..299) {
            error("HTTP $code: ${stripHtml(respText).take(180)}")
        }
    }

    private fun parseJsonOrThrow(respText: String, finalUrl: String): JSONObject {
        val trimmed = respText.trim()
        val parsed = runCatching { JSONObject(trimmed) }.getOrNull()
        if (parsed != null) return parsed

        val plain = stripHtml(trimmed)
        if (plain.contains("Script function not found", ignoreCase = true)) {
            error("Apps Script me doGet/doPost code save nahi hua! App se code Copy karke Apps Script me Save karein aur New deployment banayein.")
        }
        if (plain.contains("Sign in", ignoreCase = true) || finalUrl.contains("accounts.google.com", ignoreCase = true)) {
            error("Access 'Anyone' nahi hai! Apps Script → Deploy → Manage deployments → 'Who has access: Anyone' set karein.")
        }
        if (plain.contains("Sorry, unable to open the file", ignoreCase = true) ||
            plain.contains("Page Not Found", ignoreCase = true)
        ) {
            error("URL invalid ya expired hai. Apps Script se /exec wala Web App URL dubara copy karein.")
        }
        error("Apps Script se sahi jawab nahi mila (${plain.take(120)}). Check karein ki code Save karke 'Anyone' access ke sath /exec deploy hua ho.")
    }

    private fun stripHtml(text: String): String {
        return text
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun roundTwo(value: Double): Double = Math.round(value * 100.0) / 100.0

    companion object {
        private const val KEY_WEB_APP_URL = "web_app_url"
        private const val KEY_AUTO_PUSH = "auto_push"
        private const val KEY_LAST_SYNC_MSG = "last_sync_msg"
        private const val KEY_LAST_SYNC_OK = "last_sync_ok"

        private val SCRIPT_URL_REGEX = Regex("https?://script\\.google\\.com/[^\\s\"'<>]+", RegexOption.IGNORE_CASE)
        private val GENERIC_URL_REGEX = Regex("https?://[^\\s\"'<>]+", RegexOption.IGNORE_CASE)

        fun normalizeInputUrl(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""
            val extracted = SCRIPT_URL_REGEX.find(trimmed)?.value
                ?: GENERIC_URL_REGEX.find(trimmed)?.value
                ?: trimmed
            val cleaned = extracted.trim().trimEnd('.', ',', ';', ')', ']')
            return if (cleaned.startsWith("http://", ignoreCase = true)) {
                "https://" + cleaned.substring(7)
            } else if (!cleaned.startsWith("https://", ignoreCase = true) &&
                cleaned.startsWith("script.google.com/", ignoreCase = true)
            ) {
                "https://$cleaned"
            } else {
                cleaned
            }
        }

        fun validateAppsScriptUrl(url: String): String {
            val cleaned = normalizeInputUrl(url)
            require(cleaned.isNotEmpty()) { "Pehle Google Apps Script Web App URL paste karein." }
            if (cleaned.contains("docs.google.com/spreadsheets", ignoreCase = true)) {
                error("Aapne Google Sheet ka link paste kiya hai. Apps Script → Deploy → Web app URL (script.google.com/.../exec) paste karein.")
            }
            if (cleaned.endsWith("/dev", ignoreCase = true) || cleaned.contains("/dev?", ignoreCase = true)) {
                error("'/dev' test URL phone par kaam nahi karta. Deploy → New deployment karke '/exec' wala URL paste karein.")
            }
            if (cleaned.contains("script.google.com/home/projects", ignoreCase = true) ||
                cleaned.endsWith("/edit", ignoreCase = true)
            ) {
                error("Yeh Apps Script editor ka link hai. Apps Script me Deploy → Web app URL (.../exec) copy karke paste karein.")
            }
            require(cleaned.startsWith("https://", ignoreCase = true)) {
                "URL 'https://' se shuru hona chahiye (e.g. https://script.google.com/macros/s/.../exec)."
            }
            require(cleaned.contains("script.google.com", ignoreCase = true)) {
                "Sahi Apps Script Web App URL paste karein (https://script.google.com/macros/s/.../exec)."
            }
            return cleaned
        }

        val APPS_SCRIPT_CODE: String = """
function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return jsonResponse_({ ok: false, error: "Empty POST body." });
    }
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
    var action = (e && e.parameter && e.parameter.action) ? String(e.parameter.action) : "";
    var sheet = ss.getSheetByName("_JSL_Sync_Data");

    if (action === "ping" || action === "") {
      return jsonResponse_({
        ok: true,
        status: "ready",
        sheetName: ss.getName(),
        hasBackup: Boolean(sheet && sheet.getLastRow() >= 2),
        message: "Connected to Sheet: " + ss.getName()
      });
    }

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
  if (numCols === 0) return;
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
