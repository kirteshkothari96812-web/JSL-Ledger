package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.data.PayType
import com.example.data.WorkerEntity
import com.example.domain.DayMeta
import com.example.domain.SalaryCalculator
import com.example.domain.WorkerMonthlySummary
import com.example.ui.JslUiState
import com.example.ui.components.MonthYearSelectorBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLedgerColors
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailScreen(
    workerId: Long,
    state: JslUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonthYear: (Int, Int) -> Unit,
    onEditWorker: (WorkerEntity) -> Unit,
    onSetAttendance: (workerId: Long, dateKey: String, status: AttendanceStatus) -> Unit,
    onAddAdvance: (workerId: Long, dateKey: String, amount: Double, note: String) -> Unit,
    onDeleteAdvance: (Long) -> Unit
) {
    val summary = state.summaries.firstOrNull { it.worker.id == workerId }
    var showAddAdvanceDialog by remember { mutableStateOf(false) }
    val ledgerColors = LocalLedgerColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = summary?.worker?.name ?: "Worker Details",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (summary != null) {
                            Text(
                                text = summary.worker.payType.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (summary != null) {
                        IconButton(
                            onClick = { onEditWorker(summary.worker) },
                            modifier = Modifier.testTag("detail_edit_worker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit worker"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (summary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Worker not found.")
            }
            return@Scaffold
        }

        val workerAttendance = state.attendanceLookup[workerId].orEmpty()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MonthYearSelectorBar(
                    monthYear = state.selectedMonthYear,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onSelectMonthYear = onSelectMonthYear
                )
            }

            // Salary Calculation Breakdown Card
            item {
                SalaryBreakdownCard(summary = summary)
            }

            // Day-by-Day Attendance Calendar Grid
            item {
                MonthlyCalendarCard(
                    workerId = workerId,
                    days = state.daysInMonth,
                    attendanceMap = workerAttendance,
                    onSetAttendance = { dateKey, status ->
                        onSetAttendance(workerId, dateKey, status)
                    }
                )
            }

            // Date-wise Advance Entries Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Date-Wise Advances",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Total: ${SalaryCalculator.formatInr(summary.totalAdvance)} (${summary.advances.size} entries)",
                            style = MaterialTheme.typography.bodySmall,
                            color = ledgerColors.absentText
                        )
                    }
                    FilledTonalButton(
                        onClick = { showAddAdvanceDialog = true },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("detail_add_advance_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Advance")
                    }
                }
            }

            if (summary.advances.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ledgerColors.cardBorder)
                    ) {
                        Text(
                            text = "No advances taken in ${SalaryCalculator.monthName(state.selectedMonthYear.month)} ${state.selectedMonthYear.year}.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = summary.advances,
                    key = { it.id }
                ) { adv ->
                    AdvanceHistoryRow(
                        advance = adv,
                        workerName = summary.worker.name,
                        onWorkerClick = {},
                        onDelete = { onDeleteAdvance(adv.id) }
                    )
                }
            }
        }
    }

    if (showAddAdvanceDialog && summary != null) {
        QuickAdvanceDialog(
            workerName = summary.worker.name,
            year = state.selectedMonthYear.year,
            month = state.selectedMonthYear.month,
            onDismiss = { showAddAdvanceDialog = false },
            onSave = { dateKey, amount, note ->
                onAddAdvance(workerId, dateKey, amount, note)
                showAddAdvanceDialog = false
            }
        )
    }
}

@Composable
private fun SalaryBreakdownCard(summary: WorkerMonthlySummary) {
    val ledgerColors = LocalLedgerColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ledgerColors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Salary Hisab (${SalaryCalculator.monthName(summary.month)} ${summary.year})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CountTile(
                    label = "Full (P)",
                    value = if (summary.sundayPresentDays > 0) {
                        "${summary.presentDays} (${summary.sundayPresentDays}S)"
                    } else {
                        "${summary.presentDays}"
                    },
                    bg = ledgerColors.presentBg,
                    fg = ledgerColors.presentText,
                    modifier = Modifier.weight(1f)
                )
                CountTile(
                    label = "Half (H)",
                    value = "${summary.halfDays}",
                    bg = ledgerColors.halfDayBg,
                    fg = ledgerColors.halfDayText,
                    modifier = Modifier.weight(1f)
                )
                CountTile(
                    label = "Absent (A)",
                    value = "${summary.absentDays}",
                    bg = ledgerColors.absentBg,
                    fg = ledgerColors.absentText,
                    modifier = Modifier.weight(1f)
                )
                CountTile(
                    label = "Blank",
                    value = "${summary.unmarkedDays}",
                    bg = ledgerColors.unmarkedBg,
                    fg = ledgerColors.unmarkedText,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Calculation Formula",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.formulaBreakdown,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (summary.worker.payType == PayType.MONTHLY_26 && summary.full26DayReference != null) {
                        Text(
                            text = "Full 26-Day Reference: ${SalaryCalculator.formatInr(summary.full26DayReference)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = ledgerColors.subtleDivider)

            LedgerLineItem(
                label = "Gross / Earned Salary",
                amount = SalaryCalculator.formatInr(summary.earnedSalary),
                valueColor = MaterialTheme.colorScheme.onSurface
            )
            LedgerLineItem(
                label = "Less: Total Advance",
                amount = "- ${SalaryCalculator.formatInr(summary.totalAdvance)}",
                valueColor = ledgerColors.absentText
            )
            HorizontalDivider(color = ledgerColors.cardBorder)
            LedgerLineItem(
                label = "Net Payable Salary",
                amount = SalaryCalculator.formatInr(summary.netPayable),
                valueColor = if (summary.netPayable >= 0) ledgerColors.presentText else ledgerColors.absentText,
                emphasize = true
            )
        }
    }
}

@Composable
private fun CountTile(
    label: String,
    value: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = fg)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LedgerLineItem(
    label: String,
    amount: String,
    valueColor: Color,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = amount,
            style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun MonthlyCalendarCard(
    workerId: Long,
    days: List<DayMeta>,
    attendanceMap: Map<String, AttendanceStatus>,
    onSetAttendance: (dateKey: String, status: AttendanceStatus) -> Unit
) {
    val ledgerColors = LocalLedgerColors.current
    val firstDay = days.firstOrNull() ?: return

    // Align Monday=0 .. Sunday=6 so Sunday is the rightmost highlighted column
    val cal = Calendar.getInstance()
    val parts = firstDay.dateKey.split("-")
    cal.clear()
    cal.set(parts[0].toInt(), parts[1].toInt() - 1, 1)
    val dow = cal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2..Sat=7
    val leadingBlanks = if (dow == Calendar.SUNDAY) 6 else dow - 2

    val slots = buildList<DayMeta?> {
        repeat(leadingBlanks) { add(null) }
        addAll(days)
        while (size % 7 != 0) add(null)
    }
    val weeks = slots.chunked(7)
    val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ledgerColors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Day-by-Day Attendance (Tap any date: P / H / A)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Weekday Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weekdayLabels.forEachIndexed { idx, label ->
                    val isSun = idx == 6
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSun) ledgerColors.sundayHeaderBg else Color.Transparent)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSun) ledgerColors.sundayText else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { day ->
                        if (day == null) {
                            Box(modifier = Modifier.weight(1f).height(52.dp))
                        } else {
                            val status = attendanceMap[day.dateKey] ?: AttendanceStatus.UNMARKED
                            DetailDayCell(
                                workerId = workerId,
                                day = day,
                                status = status,
                                onSelect = { newStatus -> onSetAttendance(day.dateKey, newStatus) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailDayCell(
    workerId: Long,
    day: DayMeta,
    status: AttendanceStatus,
    onSelect: (AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    var expanded by remember { mutableStateOf(false) }

    val bg = when (status) {
        AttendanceStatus.PRESENT -> ledgerColors.presentBg
        AttendanceStatus.HALF_DAY -> ledgerColors.halfDayBg
        AttendanceStatus.ABSENT -> ledgerColors.absentBg
        AttendanceStatus.UNMARKED -> if (day.isSunday) ledgerColors.sundayColumnBg else ledgerColors.unmarkedBg.copy(alpha = 0.6f)
    }
    val fg = when (status) {
        AttendanceStatus.PRESENT -> ledgerColors.presentText
        AttendanceStatus.HALF_DAY -> ledgerColors.halfDayText
        AttendanceStatus.ABSENT -> ledgerColors.absentText
        AttendanceStatus.UNMARKED -> if (day.isSunday) ledgerColors.sundayText else ledgerColors.unmarkedText
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (day.isSunday) 1.dp else 0.5.dp,
                color = if (day.isSunday) ledgerColors.sundayHeaderBg else ledgerColors.subtleDivider,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { expanded = true }
            .testTag("detail_day_${workerId}_${day.dayOfMonth}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (day.isSunday) ledgerColors.sundayText else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (status == AttendanceStatus.UNMARKED) "—" else status.code,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = fg,
                textAlign = TextAlign.Center
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("P — Present", color = ledgerColors.presentText, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ledgerColors.presentText) },
                onClick = {
                    expanded = false
                    onSelect(AttendanceStatus.PRESENT)
                }
            )
            DropdownMenuItem(
                text = { Text("H — Half Day (½)", color = ledgerColors.halfDayText, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = ledgerColors.halfDayText) },
                onClick = {
                    expanded = false
                    onSelect(AttendanceStatus.HALF_DAY)
                }
            )
            DropdownMenuItem(
                text = { Text("A — Absent", color = ledgerColors.absentText, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Default.EventAvailable, contentDescription = null, tint = ledgerColors.absentText) },
                onClick = {
                    expanded = false
                    onSelect(AttendanceStatus.ABSENT)
                }
            )
            DropdownMenuItem(
                text = { Text("— Unmarked") },
                onClick = {
                    expanded = false
                    onSelect(AttendanceStatus.UNMARKED)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAdvanceDialog(
    workerName: String,
    year: Int,
    month: Int,
    onDismiss: () -> Unit,
    onSave: (dateKey: String, amount: Double, note: String) -> Unit
) {
    val defaultDateKey = remember(year, month) {
        val now = Calendar.getInstance()
        val d = if (now.get(Calendar.YEAR) == year && now.get(Calendar.MONTH) + 1 == month) {
            now.get(Calendar.DAY_OF_MONTH)
        } else 1
        SalaryCalculator.dateKey(year, month, d)
    }

    var dateKey by remember { mutableStateOf(defaultDateKey) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advance for $workerName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(SalaryCalculator.formatReadableDate(dateKey))
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMsg = null
                    },
                    label = { Text("Advance Amount (₹) *") },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMsg = "Enter a valid amount (> 0)."
                    } else {
                        onSave(dateKey, amt, noteText)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateKeyToUtcMillis(dateKey))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { dateKey = utcMillisToDateKey(it) }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
