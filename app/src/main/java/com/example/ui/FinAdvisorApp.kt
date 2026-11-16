package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.diagnostics.DiagnosticActionType
import com.example.data.local.DiagnosticAuditRepository
import com.example.data.local.FinAdvisorLocalDatabase
import com.example.data.model.MarketStock
import com.example.data.repository.AiAdvisoryRepository
import com.example.data.repository.FinAdvisorRepository
import com.example.ui.components.StockDeepDiveSheet
import com.example.ui.screens.AiSharePicksScreen
import com.example.ui.screens.CompareMarketsScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.PlansProfileScreen
import com.example.ui.screens.PortfolioAlertsScreen
import com.example.ui.screens.PulseScreen
import com.example.ui.screens.SmartWatchlistScreen
import com.example.ui.screens.WebReplicaScreen
import com.example.ui.viewmodel.FinAdvisorViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.PriceNotificationHelper

const val ROUTE_PULSE = "pulse"
const val ROUTE_COMPARE = "compare"
const val ROUTE_AI_PICKS = "ai_picks"
const val ROUTE_PORTFOLIO = "portfolio"
const val ROUTE_PLANS = "plans"
const val ROUTE_DIAGNOSTICS = "diagnostics"
const val ROUTE_WATCHLIST = "watchlist"
const val ROUTE_WEB_REPLICA = "web_replica"

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val appTabs = listOf(
    NavTab(ROUTE_PULSE, "Pulse", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    NavTab(ROUTE_COMPARE, "Compare", Icons.AutoMirrored.Filled.ShowChart, Icons.AutoMirrored.Filled.ShowChart),
    NavTab(ROUTE_AI_PICKS, "AI Picks", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    NavTab(ROUTE_PORTFOLIO, "Portfolio", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    NavTab(ROUTE_PLANS, "Plans", Icons.Filled.WorkspacePremium, Icons.Outlined.WorkspacePremium)
)

@Composable
fun FinAdvisorApp() {
    FinAdvisorMainShell()
}

@Composable
private fun FinAdvisorMainShell() {
    val context = LocalContext.current

    val viewModel: FinAdvisorViewModel = viewModel(
        key = FinAdvisorRepository.DEFAULT_LOCAL_USER_ID,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val localDb = FinAdvisorLocalDatabase.getInstance(app)
                FinAdvisorViewModel(
                    repository = FinAdvisorRepository(localDb.finAdvisorDao()),
                    aiRepository = AiAdvisoryRepository(),
                    currentUserId = FinAdvisorRepository.DEFAULT_LOCAL_USER_ID,
                    currentUserDisplayName = FinAdvisorRepository.DEFAULT_DISPLAY_NAME,
                    diagnosticAuditRepository = DiagnosticAuditRepository(localDb.diagnosticAuditDao())
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val holdingsState by viewModel.holdingsState.collectAsStateWithLifecycle()
    val alertsState by viewModel.alertsState.collectAsStateWithLifecycle()
    val savedReportsState by viewModel.savedReportsState.collectAsStateWithLifecycle()
    val indices by viewModel.indices.collectAsStateWithLifecycle()
    val stocks by viewModel.stocks.collectAsStateWithLifecycle()
    val predictiveAlerts by viewModel.predictiveAlerts.collectAsStateWithLifecycle()
    val selectedRange by viewModel.selectedRange.collectAsStateWithLifecycle()
    val selectedCompareCodes by viewModel.selectedCompareCodes.collectAsStateWithLifecycle()
    val aiNewsState by viewModel.aiNewsState.collectAsStateWithLifecycle()
    val stockAnalysisState by viewModel.stockAnalysisState.collectAsStateWithLifecycle()
    val divergenceForecastState by viewModel.divergenceForecastState.collectAsStateWithLifecycle()
    val diagnosticReport by viewModel.diagnosticReport.collectAsStateWithLifecycle()
    val scenarioExpression by viewModel.scenarioExpression.collectAsStateWithLifecycle()
    val aiDiagnosticState by viewModel.aiDiagnosticState.collectAsStateWithLifecycle()
    val localAuditHistory by viewModel.localAuditHistory.collectAsStateWithLifecycle()
    val watchlistState by viewModel.watchlistState.collectAsStateWithLifecycle()
    val watchlistTelemetry by viewModel.watchlistTelemetry.collectAsStateWithLifecycle()
    val watchlistAiScanState by viewModel.watchlistAiScanState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.notificationEvents.collect { evt ->
            PriceNotificationHelper.postPriceAlertNotification(
                context = context,
                notificationId = evt.id,
                title = evt.title,
                body = evt.body
            )
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var deepDiveStock by remember { mutableStateOf<MarketStock?>(null) }

    val alertsList = (alertsState as? UiState.Success)?.data.orEmpty()
    val unreadAlertsCount = alertsList.count { it.triggered && !it.read }
    val watchedItems = (watchlistState as? UiState.Success)?.data.orEmpty()
    val watchedSymbols = remember(watchedItems) { watchedItems.map { it.symbol.uppercase() }.toSet() }
    val watchlistDivergenceCount = watchedItems.count { item ->
        item.divergenceAlertActive || (watchlistTelemetry[item.symbol]?.isRealTimeDiverging == true)
    }

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isWide) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        appTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
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
                if (isWide) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(12.dp))
                        appTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_PULSE,
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxSize()
                    ) {
                        composable(ROUTE_PULSE) {
                            PulseScreen(
                                profileState = profileState,
                                holdingsState = holdingsState,
                                indices = indices,
                                stocks = stocks,
                                predictiveAlerts = predictiveAlerts,
                                unreadAlertsCount = unreadAlertsCount,
                                onNavigateCompare = { navigateTab(ROUTE_COMPARE) },
                                onNavigateAiPicks = { navigateTab(ROUTE_AI_PICKS) },
                                onNavigatePortfolio = { navigateTab(ROUTE_PORTFOLIO) },
                                onNavigatePlans = { navigateTab(ROUTE_PLANS) },
                                onNavigateDiagnostics = { navController.navigate(ROUTE_DIAGNOSTICS) },
                                onNavigateWatchlist = { navController.navigate(ROUTE_WATCHLIST) },
                                onNavigateWebReplica = { navController.navigate(ROUTE_WEB_REPLICA) },
                                watchlistCount = watchedItems.size,
                                watchlistDivergenceCount = watchlistDivergenceCount,
                                onSelectStockForDeepDive = { stock ->
                                    deepDiveStock = stock
                                    viewModel.analyzeStockWithAi(stock)
                                }
                            )
                        }
                        composable(ROUTE_COMPARE) {
                            CompareMarketsScreen(
                                indices = indices,
                                selectedRange = selectedRange,
                                selectedCompareCodes = selectedCompareCodes,
                                divergenceForecastState = divergenceForecastState,
                                onSelectRange = viewModel::selectTimeRange,
                                onToggleIndex = viewModel::toggleCompareIndex,
                                onRunAiDivergenceForecast = viewModel::generateDivergenceForecast
                            )
                        }
                        composable(ROUTE_AI_PICKS) {
                            AiSharePicksScreen(
                                profileState = profileState,
                                stocks = stocks,
                                aiNewsState = aiNewsState,
                                savedReportsState = savedReportsState,
                                watchedSymbols = watchedSymbols,
                                onGenerateNewsPicks = viewModel::generateInternationalNewsPicks,
                                onSaveAiPick = viewModel::saveAiPick,
                                onDeleteSavedReport = viewModel::deleteSavedReport,
                                onSelectStockDeepDive = { stock ->
                                    deepDiveStock = stock
                                    viewModel.analyzeStockWithAi(stock)
                                },
                                onQuickAddHolding = { stock ->
                                    viewModel.addHolding(stock, shares = 5.0, avgCostPrice = stock.livePriceUsd)
                                },
                                onToggleWatchlistStock = viewModel::toggleWatchlistStock
                            )
                        }
                        composable(ROUTE_PORTFOLIO) {
                            PortfolioAlertsScreen(
                                holdingsState = holdingsState,
                                alertsState = alertsState,
                                stocks = stocks,
                                onAddHolding = viewModel::addHolding,
                                onDeleteHolding = viewModel::deleteHolding,
                                onLoadStarterPortfolio = viewModel::loadStarterGlobalPortfolio,
                                onCreateAlert = viewModel::createAlert,
                                onSimulatePriceMove = viewModel::simulateSignificantPriceMove,
                                onMarkAlertRead = viewModel::markAlertRead,
                                onDeleteAlert = viewModel::deleteAlert,
                                onNavigateDiagnostics = { navController.navigate(ROUTE_DIAGNOSTICS) }
                            )
                        }
                        composable(ROUTE_PLANS) {
                            PlansProfileScreen(
                                profileState = profileState,
                                onSelectTier = viewModel::switchTier,
                                onSavePreferences = viewModel::updatePreferences
                            )
                        }
                        composable(ROUTE_DIAGNOSTICS) {
                            DiagnosticsScreen(
                                report = diagnosticReport,
                                scenarioExpression = scenarioExpression,
                                aiDiagnosticState = aiDiagnosticState,
                                localAuditHistory = localAuditHistory,
                                onRunTrace = { expr -> viewModel.runDiagnosticTrace(expr) },
                                onRunAiAudit = viewModel::runAiDeepDiagnosticAudit,
                                onExecuteFindingAction = { finding ->
                                    when (finding.actionType) {
                                        DiagnosticActionType.CREATE_HEDGE_ALERT -> {
                                            val sym = finding.targetSymbol
                                            val ex = finding.targetExchange
                                            if (sym != null && ex != null) {
                                                viewModel.createAlert(sym, ex, finding.recommendedPct, "both")
                                            }
                                        }
                                        DiagnosticActionType.EXPLORE_DIVERGENT_COMPARE -> navigateTab(ROUTE_COMPARE)
                                        DiagnosticActionType.OPEN_AI_PICKS -> navigateTab(ROUTE_AI_PICKS)
                                        DiagnosticActionType.REBALANCE_PORTFOLIO -> navigateTab(ROUTE_PORTFOLIO)
                                    }
                                },
                                onInspectRecommendedTicker = { ticker ->
                                    val match = stocks.firstOrNull { it.symbol.equals(ticker.trim(), ignoreCase = true) }
                                        ?: stocks.firstOrNull()
                                    if (match != null) {
                                        deepDiveStock = match
                                        viewModel.analyzeStockWithAi(match)
                                    }
                                },
                                onDeleteLocalAudit = viewModel::deleteLocalAudit,
                                onClearLocalAudits = viewModel::clearLocalAudits,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_WATCHLIST) {
                            SmartWatchlistScreen(
                                profileState = profileState,
                                watchlistState = watchlistState,
                                stocks = stocks,
                                indices = indices,
                                telemetryMap = watchlistTelemetry,
                                scanState = watchlistAiScanState,
                                onToggleWatchlistStock = viewModel::toggleWatchlistStock,
                                onLoadStarterWatchlist = viewModel::loadStarterWatchlist,
                                onRemoveWatchlistItem = viewModel::removeWatchlistItem,
                                onRunAiDivergenceScan = viewModel::scanWatchlistForDivergenceAlerts,
                                onSelectStockDeepDive = { stock ->
                                    deepDiveStock = stock
                                    viewModel.analyzeStockWithAi(stock)
                                },
                                onCreateHedgeAlert = { stock ->
                                    viewModel.createAlert(stock.symbol, stock.exchange, 2.5, "both")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_WEB_REPLICA) {
                            WebReplicaScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    val activeStock = deepDiveStock
    if (activeStock != null) {
        StockDeepDiveSheet(
            stock = activeStock,
            analysisState = stockAnalysisState,
            isWatched = activeStock.symbol.uppercase() in watchedSymbols,
            onToggleWatchlist = { viewModel.toggleWatchlistStock(activeStock) },
            onWatchAlert = {
                viewModel.createAlert(
                    symbol = activeStock.symbol,
                    exchange = activeStock.exchange,
                    targetPct = 2.5,
                    direction = "both"
                )
            },
            onAddHolding = {
                viewModel.addHolding(activeStock, 5.0, activeStock.livePriceUsd)
                deepDiveStock = null
                viewModel.clearStockAnalysis()
            },
            onDismiss = {
                deepDiveStock = null
                viewModel.clearStockAnalysis()
            }
        )
    }
}
