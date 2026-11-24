package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PayType
import com.example.data.WorkerEntity
import com.example.domain.SalaryCalculator
import com.example.domain.WorkerMonthlySummary
import com.example.ui.JslUiState
import com.example.ui.components.EmptyWorkersState
import com.example.ui.components.MonthYearSelectorBar
import com.example.ui.components.WorkerSearchBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLedgerColors

@Composable
fun WorkersScreen(
    state: JslUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonthYear: (Int, Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddWorkerClick: () -> Unit,
    onEditWorkerClick: (WorkerEntity) -> Unit,
    onDeleteWorker: (WorkerEntity) -> Unit,
    onWorkerClick: (Long) -> Unit,
    onLoadSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var workerToDelete by remember { mutableStateOf<WorkerEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                JslBrandBanner(
                    workerCount = state.workers.size,
                    monthLabel = "${SalaryCalculator.monthName(state.selectedMonthYear.month)} ${state.selectedMonthYear.year}"
                )
            }

            item {
                MonthYearSelectorBar(
                    monthYear = state.selectedMonthYear,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onSelectMonthYear = onSelectMonthYear
                )
            }

            if (state.workers.isEmpty() && !state.isLoading) {
                item {
                    EmptyWorkersState(
                        onAddWorker = onAddWorkerClick,
                        onLoadSample = onLoadSampleData
                    )
                }
            } else {
                item {
                    WorkerSearchBar(
                        query = state.searchQuery,
                        onQueryChange = onSearchQueryChange
                    )
                }

                items(
                    items = state.filteredSummaries,
                    key = { it.worker.id }
                ) { summary ->
                    WorkerDirectoryCard(
                        summary = summary,
                        onClick = { onWorkerClick(summary.worker.id) },
                        onEdit = { onEditWorkerClick(summary.worker) },
                        onDelete = { workerToDelete = summary.worker }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddWorkerClick,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add Worker") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_add_worker")
        )
    }

    workerToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { workerToDelete = null },
            title = { Text("Delete Worker?") },
            text = {
                Text(
                    "Remove ${target.name} and all their attendance and advance records? This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWorker(target)
                        workerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_worker")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { workerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun JslBrandBanner(
    workerCount: Int,
    monthLabel: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_factory_banner),
            contentDescription = "JSL Industries Factory Header",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xE6102642),
                            Color(0xAA173559),
                            Color(0x66173559)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFFF59E0B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "JSL",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF173559)
                    )
                }
                Text(
                    text = "JSL Industries",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Karigar Directory • $monthLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$workerCount Staff",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerDirectoryCard(
    summary: WorkerMonthlySummary,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ledgerColors = LocalLedgerColors.current
    val worker = summary.worker

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("worker_card_${worker.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ledgerColors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = worker.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (worker.roleOrNote.isNotBlank()) {
                            Text(
                                text = worker.roleOrNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_worker_${worker.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit ${worker.name}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_worker_${worker.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete ${worker.name}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (worker.payType == PayType.MONTHLY_26) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = worker.payType.shortLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val rateLabel = if (worker.payType == PayType.MONTHLY_26) {
                    val customTag = if (worker.useCustomDailyRate) " (Custom)" else " (÷26)"
                    "${SalaryCalculator.formatInr(worker.monthlySalary)}/mo • ${SalaryCalculator.formatInr(summary.effectiveDailyRate)}/day$customTag"
                } else {
                    "${SalaryCalculator.formatInr(summary.effectiveDailyRate)}/day"
                }
                Text(
                    text = rateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniStatChip(
                    label = if (summary.sundayPresentDays > 0) {
                        "P: ${summary.presentDays} (${summary.sundayPresentDays} Sun)"
                    } else {
                        "P: ${summary.presentDays}"
                    },
                    bg = ledgerColors.presentBg,
                    fg = ledgerColors.presentText
                )
                if (summary.halfDays > 0) {
                    MiniStatChip(
                        label = "H: ${summary.halfDays}",
                        bg = ledgerColors.halfDayBg,
                        fg = ledgerColors.halfDayText
                    )
                }
                MiniStatChip(
                    label = "A: ${summary.absentDays}",
                    bg = ledgerColors.absentBg,
                    fg = ledgerColors.absentText
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Net: ${SalaryCalculator.formatInr(summary.netPayable)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.netPayable >= 0) ledgerColors.presentText else ledgerColors.absentText
                )
            }
        }
    }
}

@Composable
private fun MiniStatChip(
    label: String,
    bg: Color,
    fg: Color
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}
