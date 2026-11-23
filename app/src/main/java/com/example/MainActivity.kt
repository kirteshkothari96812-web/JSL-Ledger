package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.WorkerEntity
import com.example.ui.JslViewModel
import com.example.ui.components.GoogleSheetSyncDialog
import com.example.ui.components.WorkerFormDialog
import com.example.ui.screens.AdvanceLedgerScreen
import com.example.ui.screens.AttendanceGridScreen
import com.example.ui.screens.SalarySummaryScreen
import com.example.ui.screens.WorkerDetailScreen
import com.example.ui.screens.WorkersScreen
import com.example.ui.theme.JslLedgerTheme
import kotlinx.coroutines.launch

const val ROUTE_ATTENDANCE = "attendance"
const val ROUTE_WORKERS = "workers"
const val ROUTE_ADVANCE = "advance"
const val ROUTE_SUMMARY = "summary"
const val ROUTE_WORKER_DETAIL = "worker_detail/{workerId}"

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopTabs = listOf(
    NavTab(
        route = ROUTE_ATTENDANCE,
        label = "Hajri",
        selectedIcon = Icons.Filled.CalendarViewMonth,
        unselectedIcon = Icons.Outlined.CalendarViewMonth,
        testTag = "tab_attendance"
    ),
    NavTab(
        route = ROUTE_WORKERS,
        label = "Workers",
        selectedIcon = Icons.Filled.Engineering,
        unselectedIcon = Icons.Outlined.Engineering,
        testTag = "tab_workers"
    ),
    NavTab(
        route = ROUTE_ADVANCE,
        label = "Advance",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "tab_advance"
    ),
    NavTab(
        route = ROUTE_SUMMARY,
        label = "Salary Sheet",
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        testTag = "tab_summary"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JslLedgerTheme {
                JslLedgerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JslLedgerApp(
    viewModel: JslViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val isTopLevelRoute = TopTabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showWorkerDialog by remember { mutableStateOf(false) }
    var editingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var showSheetSyncDialog by remember { mutableStateOf(false) }

    val openAddWorker: () -> Unit = {
        editingWorker = null
        showWorkerDialog = true
    }

    val openEditWorker: (WorkerEntity) -> Unit = { worker ->
        editingWorker = worker
        showWorkerDialog = true
    }

    val showMessage: (String) -> Unit = { msg ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (isTopLevelRoute) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "JSL Industries Ledger",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { showSheetSyncDialog = true },
                                modifier = Modifier.testTag("open_sheet_sync_button")
                            ) {
                                if (state.isSyncingSheet) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = "Google Sheet Sync",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useNavRail) {
                    NavigationBar {
                        TopTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelRoute && useNavRail) {
                    NavigationRail {
                        TopTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_ATTENDANCE
                    ) {
                        composable(ROUTE_ATTENDANCE) {
                            AttendanceGridScreen(
                                state = state,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onSelectMonthYear = viewModel::selectMonthYear,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSetAttendance = viewModel::setAttendance,
                                onMarkAllPresentForDay = { day ->
                                    viewModel.markAllPresentForDay(day)
                                    showMessage("Marked all workers Present for Day $day")
                                },
                                onWorkerClick = { id ->
                                    navController.navigate("worker_detail/$id")
                                },
                                onAddWorkerClick = openAddWorker,
                                onLoadSampleData = {
                                    viewModel.loadSampleFactoryData()
                                    showMessage("Loaded sample JSL Industries staff")
                                }
                            )
                        }

                        composable(ROUTE_WORKERS) {
                            WorkersScreen(
                                state = state,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onSelectMonthYear = viewModel::selectMonthYear,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onAddWorkerClick = openAddWorker,
                                onEditWorkerClick = openEditWorker,
                                onDeleteWorker = { worker ->
                                    viewModel.deleteWorker(worker)
                                    showMessage("Removed ${worker.name}")
                                },
                                onWorkerClick = { id ->
                                    navController.navigate("worker_detail/$id")
                                },
                                onLoadSampleData = {
                                    viewModel.loadSampleFactoryData()
                                    showMessage("Loaded sample JSL Industries staff")
                                }
                            )
                        }

                        composable(ROUTE_ADVANCE) {
                            AdvanceLedgerScreen(
                                state = state,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onSelectMonthYear = viewModel::selectMonthYear,
                                onAddAdvance = { wId, dateKey, amt, note ->
                                    viewModel.addAdvance(wId, dateKey, amt, note)
                                    showMessage("Advance saved")
                                },
                                onDeleteAdvance = { advId ->
                                    viewModel.deleteAdvance(advId)
                                    showMessage("Advance entry deleted")
                                },
                                onWorkerClick = { id ->
                                    navController.navigate("worker_detail/$id")
                                },
                                onAddWorkerClick = openAddWorker,
                                onLoadSampleData = {
                                    viewModel.loadSampleFactoryData()
                                    showMessage("Loaded sample JSL Industries staff")
                                }
                            )
                        }

                        composable(ROUTE_SUMMARY) {
                            SalarySummaryScreen(
                                state = state,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onSelectMonthYear = viewModel::selectMonthYear,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onWorkerClick = { id ->
                                    navController.navigate("worker_detail/$id")
                                },
                                onAddWorkerClick = openAddWorker,
                                onLoadSampleData = {
                                    viewModel.loadSampleFactoryData()
                                    showMessage("Loaded sample JSL Industries staff")
                                },
                                onExportJson = viewModel::exportJson,
                                onImportJson = viewModel::importJson,
                                onBuildCsv = viewModel::buildCsv,
                                onBuildPrintableHtml = viewModel::buildPrintableHtml,
                                onOpenSheetSync = { showSheetSyncDialog = true },
                                onShowMessage = showMessage
                            )
                        }

                        composable(
                            route = ROUTE_WORKER_DETAIL,
                            arguments = listOf(navArgument("workerId") { type = NavType.LongType })
                        ) { entry ->
                            val workerId = entry.arguments?.getLong("workerId") ?: 0L
                            WorkerDetailScreen(
                                workerId = workerId,
                                state = state,
                                onBack = { navController.popBackStack() },
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onSelectMonthYear = viewModel::selectMonthYear,
                                onEditWorker = openEditWorker,
                                onSetAttendance = viewModel::setAttendance,
                                onAddAdvance = { wId, dateKey, amt, note ->
                                    viewModel.addAdvance(wId, dateKey, amt, note)
                                    showMessage("Advance saved")
                                },
                                onDeleteAdvance = { advId ->
                                    viewModel.deleteAdvance(advId)
                                    showMessage("Advance entry deleted")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWorkerDialog) {
        WorkerFormDialog(
            initialWorker = editingWorker,
            onDismiss = { showWorkerDialog = false },
            onSave = { name, role, payType, monthlySalary, dailyRate, useCustom ->
                val isEdit = editingWorker != null
                viewModel.saveWorker(
                    existingId = editingWorker?.id ?: 0L,
                    name = name,
                    roleOrNote = role,
                    payType = payType,
                    monthlySalary = monthlySalary,
                    dailyRate = dailyRate,
                    useCustomDailyRate = useCustom,
                    onSaved = {
                        showWorkerDialog = false
                        showMessage(if (isEdit) "Updated $name" else "Added $name")
                    }
                )
            }
        )
    }

    if (showSheetSyncDialog) {
        GoogleSheetSyncDialog(
            config = state.sheetSyncConfig,
            isSyncing = state.isSyncingSheet,
            hasWorkers = state.workers.isNotEmpty(),
            onDismiss = { showSheetSyncDialog = false },
            onSaveUrl = viewModel::saveSheetUrl,
            onToggleAutoPush = viewModel::setAutoPushEnabled,
            onPushToSheet = { url ->
                viewModel.pushToGoogleSheet(urlOverride = url) { msg ->
                    showMessage(msg)
                }
            },
            onPullFromSheet = { url ->
                viewModel.pullFromGoogleSheet(urlOverride = url) { msg ->
                    showMessage(msg)
                }
            },
            onShowMessage = showMessage
        )
    }
}
