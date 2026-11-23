package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.SalaryCalculator
import com.example.domain.WorkerMonthlySummary
import com.example.ui.JslUiState
import com.example.ui.components.EmptyWorkersState
import com.example.ui.components.MonthYearSelectorBar
import com.example.ui.components.SummaryMetricCard
import com.example.ui.components.WorkerSearchBar
import com.example.ui.components.printHtmlDocument
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLedgerColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun SalarySummaryScreen(
    state: JslUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonthYear: (Int, Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onWorkerClick: (Long) -> Unit,
    onAddWorkerClick: () -> Unit,
    onLoadSampleData: () -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: suspend (String) -> Result<Int>,
    onBuildCsv: () -> String,
    onBuildPrintableHtml: () -> String,
    onOpenSheetSync: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ledgerColors = LocalLedgerColors.current
    val tableScrollState = rememberScrollState()

    val monthSlug = String.format(
        Locale.US,
        "%04d_%02d",
        state.selectedMonthYear.year,
        state.selectedMonthYear.month
    )

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val json = onExportJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("Unable to open file for writing")
                    }
                }
                if (result.isSuccess) {
                    onShowMessage("JSON backup saved safely.")
                } else {
                    onShowMessage("JSON export failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Unable to read selected file")
                    }
                    onImportJson(text).getOrThrow()
                }
                if (result.isSuccess) {
                    onShowMessage("Restored ${result.getOrDefault(0)} workers from backup.")
                } else {
                    onShowMessage("Import failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val csv = onBuildCsv()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(csv.toByteArray(Charsets.UTF_8))
                        } ?: error("Unable to open CSV file for writing")
                    }
                }
                if (result.isSuccess) {
                    onShowMessage("CSV salary sheet exported.")
                } else {
                    onShowMessage("CSV export failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

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

        // Backup & Export Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onOpenSheetSync,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("summary_sheet_sync_button")
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Google Sheet", maxLines = 1)
            }
            ActionPillButton(
                label = "Share Sync",
                icon = Icons.Default.Share,
                onClick = {
                    scope.launch {
                        val result = runCatching {
                            val json = onExportJson()
                            withContext(Dispatchers.IO) {
                                val dir = File(context.cacheDir, "backups").apply { mkdirs() }
                                val file = File(dir, "jsl_ledger_sync_$monthSlug.json")
                                file.writeText(json, Charsets.UTF_8)
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                            }
                        }
                        result.onSuccess { shareUri ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                putExtra(Intent.EXTRA_SUBJECT, "JSL Industries Ledger Backup ($monthSlug)")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Send Sync File to Phone"))
                        }.onFailure { err ->
                            onShowMessage("Share failed: ${err.message}")
                        }
                    }
                },
                tag = "share_sync_button",
                enabled = state.workers.isNotEmpty()
            )
            ActionPillButton(
                label = "Export JSON",
                icon = Icons.Default.Download,
                onClick = { exportJsonLauncher.launch("jsl_ledger_backup_$monthSlug.json") },
                tag = "export_json_button"
            )
            ActionPillButton(
                label = "Import JSON",
                icon = Icons.Default.Upload,
                onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                tag = "import_json_button"
            )
            ActionPillButton(
                label = "Export CSV",
                icon = Icons.Default.TableView,
                onClick = { exportCsvLauncher.launch("jsl_salary_sheet_$monthSlug.csv") },
                tag = "export_csv_button",
                enabled = state.workers.isNotEmpty()
            )
            FilledTonalButton(
                onClick = {
                    val html = onBuildPrintableHtml()
                    printHtmlDocument(context, "JSL_Salary_Sheet_$monthSlug", html)
                },
                enabled = state.workers.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("print_summary_button")
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Print / PDF", maxLines = 1)
            }
        }

        if (state.workers.isEmpty() && !state.isLoading) {
            EmptyWorkersState(
                onAddWorker = onAddWorkerClick,
                onLoadSample = onLoadSampleData
            )
            return@Column
        }

        // KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryMetricCard(
                label = "Gross Earned",
                value = SalaryCalculator.formatInr(state.totalGrossEarned),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                label = "Advance",
                value = SalaryCalculator.formatInr(state.totalAdvance),
                containerColor = ledgerColors.absentBg,
                contentColor = ledgerColors.absentText,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                label = "Net Payable",
                value = SalaryCalculator.formatInr(state.totalNetPayable),
                containerColor = ledgerColors.presentBg,
                contentColor = ledgerColors.presentText,
                modifier = Modifier.weight(1.1f)
            )
        }

        WorkerSearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchQueryChange
        )

        // Final Monthly Salary Sheet Table
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ledgerColors.cardBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    HeaderCell(text = "Worker Name", width = 132.dp, align = TextAlign.Start)
                    Row(modifier = Modifier.horizontalScroll(tableScrollState)) {
                        HeaderCell(text = "Present (P)", width = 94.dp)
                        HeaderCell(text = "Absent (A)", width = 76.dp)
                        HeaderCell(text = "Rate / Day", width = 92.dp)
                        HeaderCell(text = "Earned (₹)", width = 104.dp)
                        HeaderCell(text = "Advance (₹)", width = 104.dp)
                        HeaderCell(text = "Net Payable", width = 114.dp)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = state.filteredSummaries,
                        key = { it.worker.id }
                    ) { summary ->
                        SummaryLedgerRow(
                            summary = summary,
                            scrollState = tableScrollState,
                            onClick = { onWorkerClick(summary.worker.id) }
                        )
                        HorizontalDivider(color = ledgerColors.subtleDivider)
                    }
                }

                // Totals Footer Row
                HorizontalDivider(thickness = 1.5.dp, color = ledgerColors.cardBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(132.dp)
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "TOTAL (${state.filteredSummaries.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(modifier = Modifier.horizontalScroll(tableScrollState)) {
                        val fPresent = state.filteredSummaries.sumOf { it.presentDays }
                        val fAbsent = state.filteredSummaries.sumOf { it.absentDays }
                        val fEarned = state.filteredSummaries.sumOf { it.earnedSalary }
                        val fAdvance = state.filteredSummaries.sumOf { it.totalAdvance }
                        val fNet = state.filteredSummaries.sumOf { it.netPayable }

                        ValueCell(text = "$fPresent d", width = 94.dp, bold = true)
                        ValueCell(text = "$fAbsent d", width = 76.dp, bold = true)
                        ValueCell(text = "—", width = 92.dp)
                        ValueCell(text = SalaryCalculator.formatInr(fEarned), width = 104.dp, bold = true)
                        ValueCell(
                            text = SalaryCalculator.formatInr(fAdvance),
                            width = 104.dp,
                            color = ledgerColors.absentText,
                            bold = true
                        )
                        ValueCell(
                            text = SalaryCalculator.formatInr(fNet),
                            width = 114.dp,
                            color = ledgerColors.presentText,
                            bold = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier
            .height(48.dp)
            .testTag(tag)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun HeaderCell(
    text: String,
    width: Dp,
    align: TextAlign = TextAlign.End
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = align,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ValueCell(
    text: String,
    width: Dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false
) {
    val ledgerColors = LocalLedgerColors.current
    Box(
        modifier = Modifier
            .width(width)
            .border(0.5.dp, ledgerColors.subtleDivider.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SummaryLedgerRow(
    summary: WorkerMonthlySummary,
    scrollState: androidx.compose.foundation.ScrollState,
    onClick: () -> Unit
) {
    val ledgerColors = LocalLedgerColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("summary_row_${summary.worker.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(132.dp)
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Text(
                text = summary.worker.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = summary.worker.payType.shortLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            val presentLabel = if (summary.sundayPresentDays > 0) {
                "${summary.presentDays} (${summary.sundayPresentDays}S)"
            } else {
                "${summary.presentDays}"
            }
            ValueCell(
                text = presentLabel,
                width = 94.dp,
                color = ledgerColors.presentText,
                bold = true
            )
            ValueCell(
                text = "${summary.absentDays}",
                width = 76.dp,
                color = if (summary.absentDays > 0) ledgerColors.absentText else MaterialTheme.colorScheme.onSurfaceVariant
            )
            ValueCell(
                text = SalaryCalculator.formatInr(summary.effectiveDailyRate),
                width = 92.dp
            )
            ValueCell(
                text = SalaryCalculator.formatInr(summary.earnedSalary),
                width = 104.dp,
                bold = true
            )
            ValueCell(
                text = SalaryCalculator.formatInr(summary.totalAdvance),
                width = 104.dp,
                color = if (summary.totalAdvance > 0) ledgerColors.absentText else MaterialTheme.colorScheme.onSurfaceVariant
            )
            ValueCell(
                text = SalaryCalculator.formatInr(summary.netPayable),
                width = 114.dp,
                color = if (summary.netPayable >= 0) ledgerColors.presentText else ledgerColors.absentText,
                bold = true
            )
        }
    }
}
