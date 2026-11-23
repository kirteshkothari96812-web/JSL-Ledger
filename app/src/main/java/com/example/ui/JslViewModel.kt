package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdvanceEntity
import com.example.data.AppsScriptSyncService
import com.example.data.AttendanceStatus
import com.example.data.JslDatabase
import com.example.data.JslRepository
import com.example.data.PayType
import com.example.data.SheetSyncConfig
import com.example.data.WorkerEntity
import com.example.domain.DayMeta
import com.example.domain.SalaryCalculator
import com.example.domain.WorkerMonthlySummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MonthYear(val year: Int, val month: Int) {
    fun previous(): MonthYear = if (month == 1) MonthYear(year - 1, 12) else MonthYear(year, month - 1)
    fun next(): MonthYear = if (month == 12) MonthYear(year + 1, 1) else MonthYear(year, month + 1)
}

data class JslUiState(
    val selectedMonthYear: MonthYear,
    val searchQuery: String = "",
    val workers: List<WorkerEntity> = emptyList(),
    val daysInMonth: List<DayMeta> = emptyList(),
    val attendanceLookup: Map<Long, Map<String, AttendanceStatus>> = emptyMap(),
    val advancesForMonth: List<AdvanceEntity> = emptyList(),
    val summaries: List<WorkerMonthlySummary> = emptyList(),
    val filteredSummaries: List<WorkerMonthlySummary> = emptyList(),
    val totalGrossEarned: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val totalNetPayable: Double = 0.0,
    val totalPresentMarks: Int = 0,
    val sheetSyncConfig: SheetSyncConfig = SheetSyncConfig(),
    val isSyncingSheet: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class JslViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: JslRepository by lazy {
        val db = JslDatabase.getInstance(application)
        JslRepository(db.jslDao())
    }

    private val sheetSyncService: AppsScriptSyncService by lazy {
        AppsScriptSyncService(application)
    }

    private val initialMonthYear: MonthYear = run {
        val cal = Calendar.getInstance()
        MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private val _selectedMonthYear = MutableStateFlow(initialMonthYear)
    private val _searchQuery = MutableStateFlow("")
    private val _sheetSyncConfig = MutableStateFlow(sheetSyncService.loadConfig())
    private val _isSyncingSheet = MutableStateFlow(false)
    private var autoPushJob: Job? = null

    private val monthAttendanceFlow = _selectedMonthYear.flatMapLatest { my ->
        repository.observeAttendanceForMonth(my.year, my.month)
    }

    private val monthAdvancesFlow = _selectedMonthYear.flatMapLatest { my ->
        repository.observeAdvancesForMonth(my.year, my.month)
    }

    private val coreDataFlow = combine(
        _selectedMonthYear,
        _searchQuery,
        repository.workers,
        monthAttendanceFlow,
        monthAdvancesFlow
    ) { monthYear, query, workers, attendanceList, advancesList ->
        val days = SalaryCalculator.getMonthDays(monthYear.year, monthYear.month)

        val attendanceLookup = HashMap<Long, MutableMap<String, AttendanceStatus>>()
        for (att in attendanceList) {
            attendanceLookup.getOrPut(att.workerId) { HashMap() }[att.dateKey] = att.status
        }

        val advancesByWorker = advancesList.groupBy { it.workerId }

        val summaries = workers.map { worker ->
            SalaryCalculator.calculateWorkerMonthlySummary(
                worker = worker,
                year = monthYear.year,
                month = monthYear.month,
                days = days,
                attendanceByDate = attendanceLookup[worker.id].orEmpty(),
                workerAdvances = advancesByWorker[worker.id].orEmpty()
            )
        }

        val trimmed = query.trim().lowercase()
        val filtered = if (trimmed.isEmpty()) {
            summaries
        } else {
            summaries.filter {
                it.worker.name.lowercase().contains(trimmed) ||
                    it.worker.roleOrNote.lowercase().contains(trimmed)
            }
        }

        JslUiState(
            selectedMonthYear = monthYear,
            searchQuery = query,
            workers = workers,
            daysInMonth = days,
            attendanceLookup = attendanceLookup,
            advancesForMonth = advancesList,
            summaries = summaries,
            filteredSummaries = filtered,
            totalGrossEarned = summaries.sumOf { it.earnedSalary },
            totalAdvance = summaries.sumOf { it.totalAdvance },
            totalNetPayable = summaries.sumOf { it.netPayable },
            totalPresentMarks = summaries.sumOf { it.presentDays },
            isLoading = false
        )
    }

    val uiState: StateFlow<JslUiState> = combine(
        coreDataFlow,
        _sheetSyncConfig,
        _isSyncingSheet
    ) { core, syncConfig, isSyncing ->
        core.copy(
            sheetSyncConfig = syncConfig,
            isSyncingSheet = isSyncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JslUiState(
            selectedMonthYear = initialMonthYear,
            sheetSyncConfig = sheetSyncService.loadConfig()
        )
    )

    fun previousMonth() {
        _selectedMonthYear.value = _selectedMonthYear.value.previous()
    }

    fun nextMonth() {
        _selectedMonthYear.value = _selectedMonthYear.value.next()
    }

    fun selectMonthYear(year: Int, month: Int) {
        _selectedMonthYear.value = MonthYear(year, month.coerceIn(1, 12))
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveWorker(
        existingId: Long = 0L,
        name: String,
        roleOrNote: String,
        payType: PayType,
        monthlySalary: Double,
        dailyRate: Double,
        useCustomDailyRate: Boolean,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()
            if (cleanName.isEmpty()) return@launch
            val entity = WorkerEntity(
                id = existingId,
                name = cleanName,
                roleOrNote = roleOrNote.trim(),
                payType = payType,
                monthlySalary = if (payType == PayType.MONTHLY_26) monthlySalary else 0.0,
                dailyRate = dailyRate,
                useCustomDailyRate = if (payType == PayType.MONTHLY_26) useCustomDailyRate else false
            )
            repository.saveWorker(entity)
            onSaved()
            triggerAutoPushIfEnabled()
        }
    }

    fun deleteWorker(worker: WorkerEntity) {
        viewModelScope.launch {
            repository.deleteWorker(worker)
            triggerAutoPushIfEnabled()
        }
    }

    fun setAttendance(workerId: Long, dateKey: String, status: AttendanceStatus) {
        viewModelScope.launch {
            repository.setAttendance(workerId, dateKey, status)
            triggerAutoPushIfEnabled()
        }
    }

    fun markAllPresentForDay(dayOfMonth: Int) {
        viewModelScope.launch {
            val state = uiState.value
            val dateKey = SalaryCalculator.dateKey(
                state.selectedMonthYear.year,
                state.selectedMonthYear.month,
                dayOfMonth
            )
            repository.markAllPresentForDate(state.workers.map { it.id }, dateKey)
            triggerAutoPushIfEnabled()
        }
    }

    fun addAdvance(workerId: Long, dateKey: String, amount: Double, note: String) {
        viewModelScope.launch {
            if (amount > 0.0) {
                repository.addAdvance(workerId, dateKey, amount, note)
                triggerAutoPushIfEnabled()
            }
        }
    }

    fun deleteAdvance(advanceId: Long) {
        viewModelScope.launch {
            repository.deleteAdvance(advanceId)
            triggerAutoPushIfEnabled()
        }
    }

    fun loadSampleFactoryData() {
        viewModelScope.launch {
            val my = _selectedMonthYear.value
            repository.loadSampleFactoryData(my.year, my.month)
            triggerAutoPushIfEnabled()
        }
    }

    fun saveSheetUrl(url: String) {
        val normalized = AppsScriptSyncService.normalizeInputUrl(url)
        sheetSyncService.saveWebAppUrl(normalized)
        _sheetSyncConfig.value = _sheetSyncConfig.value.copy(webAppUrl = normalized)
    }

    fun setAutoPushEnabled(enabled: Boolean) {
        sheetSyncService.saveAutoPush(enabled)
        _sheetSyncConfig.value = _sheetSyncConfig.value.copy(autoPush = enabled)
    }

    fun testSheetConnection(urlOverride: String? = null, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val targetUrl = AppsScriptSyncService.normalizeInputUrl(
                urlOverride ?: _sheetSyncConfig.value.webAppUrl
            )
            if (targetUrl.isNotEmpty()) {
                sheetSyncService.saveWebAppUrl(targetUrl)
            }
            _isSyncingSheet.value = true
            val result = sheetSyncService.testConnection(targetUrl)
            _isSyncingSheet.value = false
            val isSuccess = result.isSuccess
            val msg = result.fold(
                onSuccess = { it },
                onFailure = { it.message ?: "Connection failed" }
            )
            sheetSyncService.saveLastSyncStatus(msg, isSuccess)
            _sheetSyncConfig.value = _sheetSyncConfig.value.copy(
                webAppUrl = targetUrl,
                lastSyncMessage = msg,
                lastSyncSuccess = isSuccess
            )
            onResult(msg)
        }
    }

    fun pushToGoogleSheet(urlOverride: String? = null, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val targetUrl = AppsScriptSyncService.normalizeInputUrl(
                urlOverride ?: _sheetSyncConfig.value.webAppUrl
            )
            if (targetUrl.isNotEmpty()) {
                sheetSyncService.saveWebAppUrl(targetUrl)
            }
            _isSyncingSheet.value = true
            val state = uiState.value
            val backupJson = repository.exportToJson()
            val result = sheetSyncService.pushToSheet(
                webAppUrl = targetUrl,
                year = state.selectedMonthYear.year,
                month = state.selectedMonthYear.month,
                days = state.daysInMonth,
                summaries = state.summaries,
                attendanceLookup = state.attendanceLookup,
                advancesForMonth = state.advancesForMonth,
                rawBackupJson = backupJson
            )
            _isSyncingSheet.value = false
            val isSuccess = result.isSuccess
            val msg = result.fold(
                onSuccess = { it },
                onFailure = { "Sync failed: ${it.message}" }
            )
            sheetSyncService.saveLastSyncStatus(msg, isSuccess)
            _sheetSyncConfig.value = _sheetSyncConfig.value.copy(
                webAppUrl = targetUrl,
                lastSyncMessage = msg,
                lastSyncSuccess = isSuccess
            )
            onResult(msg)
        }
    }

    fun pullFromGoogleSheet(urlOverride: String? = null, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val targetUrl = AppsScriptSyncService.normalizeInputUrl(
                urlOverride ?: _sheetSyncConfig.value.webAppUrl
            )
            if (targetUrl.isNotEmpty()) {
                sheetSyncService.saveWebAppUrl(targetUrl)
            }
            _isSyncingSheet.value = true
            val pullResult = sheetSyncService.pullFromSheet(targetUrl)
            var isSuccess = false
            val finalMessage = pullResult.fold(
                onSuccess = { jsonText ->
                    repository.importFromJson(jsonText).fold(
                        onSuccess = { count ->
                            isSuccess = true
                            "Synced $count workers from Google Sheet!"
                        },
                        onFailure = { "Invalid sheet backup: ${it.message}" }
                    )
                },
                onFailure = { "Pull failed: ${it.message}" }
            )
            _isSyncingSheet.value = false
            sheetSyncService.saveLastSyncStatus(finalMessage, isSuccess)
            _sheetSyncConfig.value = _sheetSyncConfig.value.copy(
                webAppUrl = targetUrl,
                lastSyncMessage = finalMessage,
                lastSyncSuccess = isSuccess
            )
            onResult(finalMessage)
        }
    }

    private fun triggerAutoPushIfEnabled() {
        val cfg = _sheetSyncConfig.value
        if (!cfg.autoPush || cfg.webAppUrl.isBlank()) return
        autoPushJob?.cancel()
        autoPushJob = viewModelScope.launch {
            delay(1500)
            pushToGoogleSheet()
        }
    }

    suspend fun exportJson(): String = repository.exportToJson()

    suspend fun importJson(jsonText: String): Result<Int> = repository.importFromJson(jsonText)

    fun buildCsv(): String {
        val state = uiState.value
        return repository.buildMonthlyCsv(
            year = state.selectedMonthYear.year,
            month = state.selectedMonthYear.month,
            days = state.daysInMonth,
            summaries = state.summaries,
            attendanceLookup = state.attendanceLookup
        )
    }

    fun buildPrintableHtml(): String {
        val state = uiState.value
        return repository.buildPrintableHtml(
            year = state.selectedMonthYear.year,
            month = state.selectedMonthYear.month,
            summaries = state.summaries
        )
    }
}
