package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AdvanceEntity
import com.example.domain.SalaryCalculator
import com.example.ui.JslUiState
import com.example.ui.components.EmptyWorkersState
import com.example.ui.components.MonthYearSelectorBar
import com.example.ui.components.SummaryMetricCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLedgerColors
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceLedgerScreen(
    state: JslUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonthYear: (Int, Int) -> Unit,
    onAddAdvance: (workerId: Long, dateKey: String, amount: Double, note: String) -> Unit,
    onDeleteAdvance: (Long) -> Unit,
    onWorkerClick: (Long) -> Unit,
    onAddWorkerClick: () -> Unit,
    onLoadSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    val workers = state.workers
    val workerById = remember(workers) { workers.associateBy { it.id } }

    var selectedWorkerId by remember { mutableStateOf<Long?>(null) }
    var filterWorkerId by remember { mutableStateOf<Long?>(null) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    val defaultDateKey = remember(state.selectedMonthYear) {
        val now = Calendar.getInstance()
        val curY = now.get(Calendar.YEAR)
        val curM = now.get(Calendar.MONTH) + 1
        val day = if (curY == state.selectedMonthYear.year && curM == state.selectedMonthYear.month) {
            now.get(Calendar.DAY_OF_MONTH)
        } else {
            1
        }
        SalaryCalculator.dateKey(state.selectedMonthYear.year, state.selectedMonthYear.month, day)
    }

    var selectedDateKey by remember(state.selectedMonthYear) { mutableStateOf(defaultDateKey) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(workers) {
        if (selectedWorkerId == null || workers.none { it.id == selectedWorkerId }) {
            selectedWorkerId = workers.firstOrNull()?.id
        }
        if (filterWorkerId != null && workers.none { it.id == filterWorkerId }) {
            filterWorkerId = null
        }
    }

    val displayedAdvances = remember(state.advancesForMonth, filterWorkerId) {
        if (filterWorkerId == null) {
            state.advancesForMonth
        } else {
            state.advancesForMonth.filter { it.workerId == filterWorkerId }
        }
    }
    val displayedTotal = remember(displayedAdvances) { displayedAdvances.sumOf { it.amount } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 28.dp),
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

        if (workers.isEmpty() && !state.isLoading) {
            item {
                EmptyWorkersState(
                    onAddWorker = onAddWorkerClick,
                    onLoadSample = onLoadSampleData
                )
            }
            return@LazyColumn
        }

        // Quick Add Advance Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ledgerColors.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Add Advance Entry (उधार / पेशगी)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Worker Dropdown
                    var workerDropdownOpen by remember { mutableStateOf(false) }
                    val selectedWorkerName = workerById[selectedWorkerId]?.name ?: "Select Worker"

                    ExposedDropdownMenuBox(
                        expanded = workerDropdownOpen,
                        onExpandedChange = { workerDropdownOpen = !workerDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = selectedWorkerName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Worker Name") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = workerDropdownOpen)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("advance_worker_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = workerDropdownOpen,
                            onDismissRequest = { workerDropdownOpen = false }
                        ) {
                            workers.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = {
                                        selectedWorkerId = w.id
                                        workerDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("advance_date_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Pick advance date",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = SalaryCalculator.formatReadableDate(selectedDateKey),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                formError = null
                            },
                            label = { Text("Amount (₹)") },
                            placeholder = { Text("2000") },
                            prefix = { Text("₹ ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("advance_amount_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Quick amount chips for fast one-hand entry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(500, 1000, 2000, 5000).forEach { quickAmt ->
                            FilterChip(
                                selected = amountText == quickAmt.toString(),
                                onClick = {
                                    amountText = quickAmt.toString()
                                    formError = null
                                },
                                label = { Text("+₹$quickAmt") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note / Reason (Optional)") },
                        placeholder = { Text("e.g., Medical, Home Kharch, Ration") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advance_note_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (formError != null) {
                        Text(
                            text = formError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            val wId = selectedWorkerId
                            val amt = amountText.toDoubleOrNull()
                            when {
                                wId == null -> formError = "Please select a worker."
                                amt == null || amt <= 0.0 -> formError = "Enter a valid advance amount (> 0)."
                                else -> {
                                    onAddAdvance(wId, selectedDateKey, amt, noteText)
                                    amountText = ""
                                    noteText = ""
                                    formError = null
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_advance_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Advance Entry")
                    }
                }
            }
        }

        // Filter Chips + Total Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterWorkerId == null,
                        onClick = { filterWorkerId = null },
                        label = { Text("All Workers") }
                    )
                    workers.forEach { w ->
                        FilterChip(
                            selected = filterWorkerId == w.id,
                            onClick = { filterWorkerId = w.id },
                            label = { Text(w.name) }
                        )
                    }
                }

                SummaryMetricCard(
                    label = if (filterWorkerId == null) {
                        "Total Advance (${SalaryCalculator.shortMonthName(state.selectedMonthYear.month)} ${state.selectedMonthYear.year})"
                    } else {
                        "${workerById[filterWorkerId]?.name ?: "Worker"} Advance Total"
                    },
                    value = SalaryCalculator.formatInr(displayedTotal),
                    containerColor = ledgerColors.absentBg,
                    contentColor = ledgerColors.absentText,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (displayedAdvances.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ledgerColors.cardBorder)
                ) {
                    Text(
                        text = "No advance entries recorded for this selection.",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = displayedAdvances,
                key = { it.id }
            ) { adv ->
                val wName = workerById[adv.workerId]?.name ?: "Worker"
                AdvanceHistoryRow(
                    advance = adv,
                    workerName = wName,
                    onWorkerClick = { onWorkerClick(adv.workerId) },
                    onDelete = { onDeleteAdvance(adv.id) }
                )
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(selectedDateKey) { dateKeyToUtcMillis(selectedDateKey) }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDateKey = utcMillisToDateKey(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun AdvanceHistoryRow(
    advance: AdvanceEntity,
    workerName: String,
    onWorkerClick: () -> Unit,
    onDelete: () -> Unit
) {
    val ledgerColors = LocalLedgerColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("advance_row_${advance.id}"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ledgerColors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TextButton(
                    onClick = onWorkerClick,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(
                        text = workerName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = SalaryCalculator.formatReadableDate(advance.dateKey),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (advance.note.isNotBlank()) {
                    Text(
                        text = advance.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-${SalaryCalculator.formatInr(advance.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = ledgerColors.absentText
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_advance_${advance.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete advance entry",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

fun dateKeyToUtcMillis(dateKey: String): Long {
    val parts = dateKey.split("-")
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.clear()
    if (parts.size == 3) {
        val y = parts[0].toIntOrNull() ?: 2026
        val m = (parts[1].toIntOrNull() ?: 1) - 1
        val d = parts[2].toIntOrNull() ?: 1
        cal.set(y, m, d, 0, 0, 0)
    }
    return cal.timeInMillis
}

fun utcMillisToDateKey(millis: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = millis
    return SalaryCalculator.dateKey(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}
