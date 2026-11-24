package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.domain.DayMeta
import com.example.domain.SalaryCalculator
import com.example.domain.WorkerMonthlySummary
import com.example.ui.JslUiState
import com.example.ui.components.EmptyWorkersState
import com.example.ui.components.MonthYearSelectorBar
import com.example.ui.components.SummaryMetricCard
import com.example.ui.components.WorkerSearchBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLedgerColors

@Composable
fun AttendanceGridScreen(
    state: JslUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonthYear: (Int, Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSetAttendance: (workerId: Long, dateKey: String, status: AttendanceStatus) -> Unit,
    onMarkAllPresentForDay: (Int) -> Unit,
    onWorkerClick: (Long) -> Unit,
    onAddWorkerClick: () -> Unit,
    onLoadSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    val horizontalScrollState = rememberScrollState()
    var showMarkAllDayMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MonthYearSelectorBar(
            monthYear = state.selectedMonthYear,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
            onSelectMonthYear = onSelectMonthYear
        )

        if (state.workers.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                EmptyWorkersState(
                    onAddWorker = onAddWorkerClick,
                    onLoadSample = onLoadSampleData
                )
            }
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryMetricCard(
                label = "Workers",
                value = state.workers.size.toString(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(0.9f)
            )
            SummaryMetricCard(
                label = "Payable Days",
                value = SalaryCalculator.formatDays(state.totalPayableDays),
                containerColor = ledgerColors.presentBg,
                contentColor = ledgerColors.presentText,
                modifier = Modifier.weight(1.0f)
            )
            SummaryMetricCard(
                label = "Net Payable",
                value = SalaryCalculator.formatInr(state.totalNetPayable),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1.4f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkerSearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.weight(1f)
            )

            Box {
                FilledTonalButton(
                    onClick = { showMarkAllDayMenu = true },
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("mark_all_day_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Mark all present for a day",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("All P", maxLines = 1)
                }

                DropdownMenu(
                    expanded = showMarkAllDayMenu,
                    onDismissRequest = { showMarkAllDayMenu = false },
                    modifier = Modifier.height(340.dp)
                ) {
                    state.daysInMonth.forEach { day ->
                        DropdownMenuItem(
                            text = {
                                val tag = if (day.isSunday) " (Sun)" else " (${day.dayShortName})"
                                Text("Day ${day.dayOfMonth}$tag — Mark All P")
                            },
                            onClick = {
                                showMarkAllDayMenu = false
                                onMarkAllPresentForDay(day.dayOfMonth)
                            }
                        )
                    }
                }
            }
        }

        // Legend Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendChip("P", "Present", ledgerColors.presentBg, ledgerColors.presentText)
            LegendChip("H", "Half Day", ledgerColors.halfDayBg, ledgerColors.halfDayText)
            LegendChip("A", "Absent", ledgerColors.absentBg, ledgerColors.absentText)
            LegendChip("—", "Unmarked", ledgerColors.unmarkedBg, ledgerColors.unmarkedText)
            LegendChip("SUN", "Sunday", ledgerColors.sundayHeaderBg, ledgerColors.sundayText)
        }

        // Synchronized Horizontal + Vertical Attendance Matrix
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ledgerColors.cardBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Sticky Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .width(134.dp)
                            .height(52.dp)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Worker (P/H/A)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(horizontalScrollState)
                    ) {
                        state.daysInMonth.forEach { day ->
                            val headerBg = if (day.isSunday) ledgerColors.sundayHeaderBg else Color.Transparent
                            val headerText = if (day.isSunday) ledgerColors.sundayText else MaterialTheme.colorScheme.onSurface
                            Column(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(52.dp)
                                    .background(headerBg)
                                    .border(0.5.dp, ledgerColors.subtleDivider),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (day.isSunday) "SUN" else day.dayShortName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (day.isSunday) FontWeight.Bold else FontWeight.Medium,
                                    color = headerText
                                )
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = headerText
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = ledgerColors.cardBorder)

                if (state.filteredSummaries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching workers found for \"${state.searchQuery}\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = state.filteredSummaries,
                            key = { it.worker.id }
                        ) { summary ->
                            AttendanceWorkerRow(
                                summary = summary,
                                days = state.daysInMonth,
                                workerAttendance = state.attendanceLookup[summary.worker.id].orEmpty(),
                                horizontalScrollState = horizontalScrollState,
                                onWorkerClick = { onWorkerClick(summary.worker.id) },
                                onSetAttendance = { dateKey, status ->
                                    onSetAttendance(summary.worker.id, dateKey, status)
                                }
                            )
                            HorizontalDivider(color = ledgerColors.subtleDivider)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendChip(
    badge: String,
    label: String,
    bgColor: Color,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(bgColor)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AttendanceWorkerRow(
    summary: WorkerMonthlySummary,
    days: List<DayMeta>,
    workerAttendance: Map<String, AttendanceStatus>,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onWorkerClick: () -> Unit,
    onSetAttendance: (String, AttendanceStatus) -> Unit
) {
    val ledgerColors = LocalLedgerColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sticky Left Worker Name + Count
        Column(
            modifier = Modifier
                .width(134.dp)
                .height(56.dp)
                .clickable(onClick = onWorkerClick)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("grid_worker_${summary.worker.id}"),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = summary.worker.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sunPart = if (summary.sundayPresentDays > 0) "(${summary.sundayPresentDays}S)" else ""
            val halfPart = if (summary.halfDays > 0) " • H:${summary.halfDays}" else ""
            Text(
                text = "P:${summary.presentDays}$sunPart$halfPart • A:${summary.absentDays}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Scrollable Day Cells
        Row(
            modifier = Modifier.horizontalScroll(horizontalScrollState)
        ) {
            days.forEach { day ->
                val status = workerAttendance[day.dateKey] ?: AttendanceStatus.UNMARKED
                AttendanceCellDropdown(
                    workerId = summary.worker.id,
                    day = day,
                    status = status,
                    onSelectStatus = { newStatus -> onSetAttendance(day.dateKey, newStatus) }
                )
            }
        }
    }
}

@Composable
fun AttendanceCellDropdown(
    workerId: Long,
    day: DayMeta,
    status: AttendanceStatus,
    onSelectStatus: (AttendanceStatus) -> Unit
) {
    val ledgerColors = LocalLedgerColors.current
    var expanded by remember { mutableStateOf(false) }

    val columnBg = if (day.isSunday) ledgerColors.sundayColumnBg else Color.Transparent

    val (pillBg, pillText, pillCode) = when (status) {
        AttendanceStatus.PRESENT -> Triple(ledgerColors.presentBg, ledgerColors.presentText, "P")
        AttendanceStatus.HALF_DAY -> Triple(ledgerColors.halfDayBg, ledgerColors.halfDayText, "H")
        AttendanceStatus.ABSENT -> Triple(ledgerColors.absentBg, ledgerColors.absentText, "A")
        AttendanceStatus.UNMARKED -> Triple(
            if (day.isSunday) ledgerColors.sundayHeaderBg.copy(alpha = 0.45f) else ledgerColors.unmarkedBg,
            if (day.isSunday) ledgerColors.sundayText else ledgerColors.unmarkedText,
            "—"
        )
    }

    Box(
        modifier = Modifier
            .width(50.dp)
            .height(56.dp)
            .background(columnBg)
            .border(0.5.dp, ledgerColors.subtleDivider)
            .clickable { expanded = true }
            .testTag("att_cell_${workerId}_${day.dayOfMonth}"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(pillBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pillCode,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = pillText,
                textAlign = TextAlign.Center
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "P — Present (हाज़िरी)",
                        fontWeight = FontWeight.SemiBold,
                        color = ledgerColors.presentText
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ledgerColors.presentText
                    )
                },
                onClick = {
                    expanded = false
                    onSelectStatus(AttendanceStatus.PRESENT)
                },
                modifier = Modifier.testTag("att_opt_P")
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "H — Half Day (आधा दिन ½)",
                        fontWeight = FontWeight.SemiBold,
                        color = ledgerColors.halfDayText
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = ledgerColors.halfDayText
                    )
                },
                onClick = {
                    expanded = false
                    onSelectStatus(AttendanceStatus.HALF_DAY)
                },
                modifier = Modifier.testTag("att_opt_H")
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "A — Absent (गैर-हाज़िर)",
                        fontWeight = FontWeight.SemiBold,
                        color = ledgerColors.absentText
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = ledgerColors.absentText
                    )
                },
                onClick = {
                    expanded = false
                    onSelectStatus(AttendanceStatus.ABSENT)
                },
                modifier = Modifier.testTag("att_opt_A")
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "— Unmarked (खाली)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {
                    expanded = false
                    onSelectStatus(AttendanceStatus.UNMARKED)
                },
                modifier = Modifier.testTag("att_opt_UNMARKED")
            )
        }
    }
}
