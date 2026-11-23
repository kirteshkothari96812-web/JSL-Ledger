package com.example.ui.components

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PayType
import com.example.data.WorkerEntity
import com.example.domain.SalaryCalculator
import com.example.ui.MonthYear
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLedgerColors

@Composable
fun MonthYearSelectorBar(
    monthYear: MonthYear,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectMonthYear: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val ledgerColors = LocalLedgerColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ledgerColors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("prev_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showPicker = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("month_year_picker_trigger"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick month and year",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${SalaryCalculator.monthName(monthYear.month)} ${monthYear.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showPicker) {
        MonthYearPickerDialog(
            current = monthYear,
            onDismiss = { showPicker = false },
            onConfirm = { y, m ->
                onSelectMonthYear(y, m)
                showPicker = false
            }
        )
    }
}

@Composable
fun MonthYearPickerDialog(
    current: MonthYear,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(current.year) }
    var selectedMonth by remember { mutableIntStateOf(current.month) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month & Year", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous year")
                    }
                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = JetBrainsMonoFamily
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next year")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (rowIdx in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (colIdx in 0 until 3) {
                                val m = rowIdx * 3 + colIdx + 1
                                val isSelected = m == selectedMonth
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedMonth = m },
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = SalaryCalculator.shortMonthName(m),
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WorkerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag("worker_search_input"),
        placeholder = { Text("Search worker / Karigar...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun EmptyWorkersState(
    onAddWorker: () -> Unit,
    onLoadSample: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ledgerColors.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_workers),
                contentDescription = "JSL Industries attendance register illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = "No Workers Added Yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Add factory staff (e.g., Dharmendra) with 26-Day Monthly or Daily Wage rates to start tracking Hajri & Advances.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddWorker,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("empty_add_worker_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Worker")
                }
                OutlinedButton(
                    onClick = onLoadSample,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("load_sample_data_button")
                ) {
                    Text("Load Sample", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun SummaryMetricCard(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WorkerFormDialog(
    initialWorker: WorkerEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        roleOrNote: String,
        payType: PayType,
        monthlySalary: Double,
        dailyRate: Double,
        useCustomDailyRate: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialWorker?.name ?: "") }
    var role by remember { mutableStateOf(initialWorker?.roleOrNote ?: "") }
    var payType by remember { mutableStateOf(initialWorker?.payType ?: PayType.MONTHLY_26) }
    var monthlySalaryText by remember {
        mutableStateOf(
            if (initialWorker != null && initialWorker.monthlySalary > 0)
                trimZeroDecimals(initialWorker.monthlySalary)
            else "20000"
        )
    }
    var useCustomRate by remember { mutableStateOf(initialWorker?.useCustomDailyRate ?: false) }
    var dailyRateText by remember {
        mutableStateOf(
            if (initialWorker != null && initialWorker.dailyRate > 0)
                trimZeroDecimals(initialWorker.dailyRate)
            else if (payType == PayType.DAILY_WAGE) "450" else "780"
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val parsedMonthly = monthlySalaryText.toDoubleOrNull() ?: 0.0
    val autoPerDay = parsedMonthly / 26.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialWorker == null) "Add Worker (नया कारीगर)" else "Edit Worker",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMsg = null
                    },
                    label = { Text("Worker Name *") },
                    placeholder = { Text("e.g., Dharmendra") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_name_input")
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Trade / Role (Optional)") },
                    placeholder = { Text("e.g., Operator, Welder, Helper") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_role_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pay Type (वेतन प्रकार)", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PayType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = payType == type,
                                onClick = {
                                    payType = type
                                    if (type == PayType.DAILY_WAGE && dailyRateText == "780") {
                                        dailyRateText = "450"
                                    } else if (type == PayType.MONTHLY_26 && dailyRateText == "450") {
                                        dailyRateText = "780"
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, PayType.entries.size),
                                modifier = Modifier.testTag("pay_type_${type.name}")
                            ) {
                                Text(
                                    text = if (type == PayType.MONTHLY_26) "Monthly (26-Day)" else "Daily Wage",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (payType == PayType.MONTHLY_26) {
                    OutlinedTextField(
                        value = monthlySalaryText,
                        onValueChange = {
                            monthlySalaryText = it
                            errorMsg = null
                        },
                        label = { Text("Monthly Salary (₹) *") },
                        placeholder = { Text("e.g., 20000") },
                        prefix = { Text("₹ ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monthly_salary_input")
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "26-Day Formula Rate: ${SalaryCalculator.formatInr(autoPerDay, forceDecimals = true)} / day",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Custom Per-Day Rate Override", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Set fixed rate (e.g., ₹780/day) instead of ÷ 26",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useCustomRate,
                            onCheckedChange = { useCustomRate = it },
                            modifier = Modifier.testTag("custom_rate_switch")
                        )
                    }

                    if (useCustomRate) {
                        OutlinedTextField(
                            value = dailyRateText,
                            onValueChange = {
                                dailyRateText = it
                                errorMsg = null
                            },
                            label = { Text("Custom Per-Day Rate (₹) *") },
                            placeholder = { Text("e.g., 780") },
                            prefix = { Text("₹ ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_daily_rate_input")
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = dailyRateText,
                        onValueChange = {
                            dailyRateText = it
                            errorMsg = null
                        },
                        label = { Text("Per-Day Wage Rate (₹) *") },
                        placeholder = { Text("e.g., 450") },
                        prefix = { Text("₹ ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_wage_rate_input")
                    )
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val mSal = monthlySalaryText.toDoubleOrNull() ?: 0.0
                    val dRate = dailyRateText.toDoubleOrNull() ?: 0.0
                    when {
                        cleanName.isEmpty() -> errorMsg = "Please enter worker name."
                        payType == PayType.MONTHLY_26 && mSal <= 0.0 && !useCustomRate ->
                            errorMsg = "Enter a valid monthly salary (> 0)."
                        payType == PayType.MONTHLY_26 && useCustomRate && dRate <= 0.0 ->
                            errorMsg = "Enter a valid custom per-day rate (> 0)."
                        payType == PayType.DAILY_WAGE && dRate <= 0.0 ->
                            errorMsg = "Enter a valid daily wage rate (> 0)."
                        else -> onSave(cleanName, role, payType, mSal, dRate, useCustomRate)
                    }
                },
                modifier = Modifier.testTag("save_worker_button")
            ) {
                Text(if (initialWorker == null) "Save Worker" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun trimZeroDecimals(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

fun printHtmlDocument(context: Context, jobName: String, htmlContent: String) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null && view != null) {
                val adapter = view.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    adapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
}
