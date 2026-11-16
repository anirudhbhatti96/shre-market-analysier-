package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.diagnostics.AiDiagnosticVerdict
import com.example.data.diagnostics.DiagnosticReportSnapshot
import com.example.data.diagnostics.DiagnosticsEngine
import com.example.data.local.DiagnosticAuditEntity
import com.example.data.local.DiagnosticAuditRepository
import com.example.data.market.GlobalMarketCatalog
import com.example.data.model.AiGeneratedNewsPick
import com.example.data.model.AiNewsReport
import com.example.data.model.GlobalIndex
import com.example.data.model.MarketStock
import com.example.data.model.PortfolioHolding
import com.example.data.model.PredictiveTrendAlert
import com.example.data.model.PriceAlert
import com.example.data.model.RiskProfile
import com.example.data.model.SubscriptionTier
import com.example.data.model.TimeRange
import com.example.data.model.UserAdvisoryProfile
import com.example.data.model.WatchlistItem
import com.example.data.repository.AiAdvisoryRepository
import com.example.data.repository.FinAdvisorRepository
import com.example.data.repository.StockAiAnalysis
import com.example.data.watchlist.CorrelationTelemetry
import com.example.data.watchlist.WatchlistCorrelationEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiTaskState<out T> {
    data object Idle : AiTaskState<Nothing>
    data object Loading : AiTaskState<Nothing>
    data class Success<T>(val data: T) : AiTaskState<T>
    data class Error(val message: String) : AiTaskState<Nothing>
}

data class NotificationDispatchEvent(
    val id: Int,
    val title: String,
    val body: String
)

class FinAdvisorViewModel(
    private val repository: FinAdvisorRepository,
    private val aiRepository: AiAdvisoryRepository,
    private val currentUserId: String,
    private val currentUserDisplayName: String,
    private val diagnosticAuditRepository: DiagnosticAuditRepository? = null
) : ViewModel() {

    val profileState: StateFlow<UiState<UserAdvisoryProfile>> = repository.observeProfile(currentUserId)
        .map<UserAdvisoryProfile?, UiState<UserAdvisoryProfile>> { profile ->
            UiState.Success(
                profile ?: UserAdvisoryProfile(
                    id = currentUserId,
                    userId = currentUserId,
                    displayName = currentUserDisplayName
                )
            )
        }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing profile", error)
            emit(UiState.Error(error.message ?: "Failed to load advisory profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val holdingsState: StateFlow<UiState<List<PortfolioHolding>>> = repository.observeHoldings(currentUserId)
        .map<List<PortfolioHolding>, UiState<List<PortfolioHolding>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing holdings", error)
            emit(UiState.Error(error.message ?: "Failed to load portfolio holdings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val alertsState: StateFlow<UiState<List<PriceAlert>>> = repository.observeAlerts(currentUserId)
        .map<List<PriceAlert>, UiState<List<PriceAlert>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing alerts", error)
            emit(UiState.Error(error.message ?: "Failed to load alerts"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val savedReportsState: StateFlow<UiState<List<AiNewsReport>>> = repository.observeAiReports(currentUserId)
        .map<List<AiNewsReport>, UiState<List<AiNewsReport>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing AI reports", error)
            emit(UiState.Error(error.message ?: "Failed to load saved AI reports"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val watchlistState: StateFlow<UiState<List<WatchlistItem>>> = repository.observeWatchlist(currentUserId)
        .map<List<WatchlistItem>, UiState<List<WatchlistItem>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing watchlist", error)
            emit(UiState.Error(error.message ?: "Failed to load smart watchlist"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _indices = MutableStateFlow(GlobalMarketCatalog.initialIndices)
    val indices: StateFlow<List<GlobalIndex>> = _indices.asStateFlow()

    private val _stocks = MutableStateFlow(GlobalMarketCatalog.initialStocks)
    val stocks: StateFlow<List<MarketStock>> = _stocks.asStateFlow()

    private val _predictiveAlerts = MutableStateFlow(GlobalMarketCatalog.initialPredictiveAlerts)
    val predictiveAlerts: StateFlow<List<PredictiveTrendAlert>> = _predictiveAlerts.asStateFlow()

    private val _selectedRange = MutableStateFlow(TimeRange.ONE_YEAR)
    val selectedRange: StateFlow<TimeRange> = _selectedRange.asStateFlow()

    private val _selectedCompareCodes = MutableStateFlow(setOf("SPX", "N225", "DAX", "SENSEX"))
    val selectedCompareCodes: StateFlow<Set<String>> = _selectedCompareCodes.asStateFlow()

    private val _aiNewsState = MutableStateFlow<AiTaskState<List<AiGeneratedNewsPick>>>(AiTaskState.Idle)
    val aiNewsState: StateFlow<AiTaskState<List<AiGeneratedNewsPick>>> = _aiNewsState.asStateFlow()

    private val _stockAnalysisState = MutableStateFlow<AiTaskState<StockAiAnalysis>>(AiTaskState.Idle)
    val stockAnalysisState: StateFlow<AiTaskState<StockAiAnalysis>> = _stockAnalysisState.asStateFlow()

    private val _divergenceForecastState = MutableStateFlow<AiTaskState<String>>(AiTaskState.Idle)
    val divergenceForecastState: StateFlow<AiTaskState<String>> = _divergenceForecastState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _scenarioExpression = MutableStateFlow("PORTFOLIO_AUDIT()")
    val scenarioExpression: StateFlow<String> = _scenarioExpression.asStateFlow()

    private val _aiDiagnosticState = MutableStateFlow<AiTaskState<AiDiagnosticVerdict>>(AiTaskState.Idle)
    val aiDiagnosticState: StateFlow<AiTaskState<AiDiagnosticVerdict>> = _aiDiagnosticState.asStateFlow()

    private val _watchlistAiScanState = MutableStateFlow<AiTaskState<Int>>(AiTaskState.Idle)
    val watchlistAiScanState: StateFlow<AiTaskState<Int>> = _watchlistAiScanState.asStateFlow()

    val watchlistTelemetry: StateFlow<Map<String, CorrelationTelemetry>> = combine(
        _stocks,
        _indices
    ) { currentStocks, currentIndices ->
        currentStocks.associate { st ->
            st.symbol to WatchlistCorrelationEngine.computeCorrelationTelemetry(st, currentIndices)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = GlobalMarketCatalog.initialStocks.associate { st ->
            st.symbol to WatchlistCorrelationEngine.computeCorrelationTelemetry(st, GlobalMarketCatalog.initialIndices)
        }
    )

    val localAuditHistory: StateFlow<List<DiagnosticAuditEntity>> = (
        diagnosticAuditRepository?.observeUserAudits(currentUserId) ?: flowOf(emptyList())
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val diagnosticReport: StateFlow<DiagnosticReportSnapshot> = combine(
        profileState,
        holdingsState,
        alertsState,
        _stocks,
        _indices,
        _scenarioExpression
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val profile = (values[0] as? UiState.Success<UserAdvisoryProfile>)?.data
        @Suppress("UNCHECKED_CAST")
        val holdings = (values[1] as? UiState.Success<List<PortfolioHolding>>)?.data.orEmpty()
        @Suppress("UNCHECKED_CAST")
        val alerts = (values[2] as? UiState.Success<List<PriceAlert>>)?.data.orEmpty()
        @Suppress("UNCHECKED_CAST")
        val currentStocks = values[3] as List<MarketStock>
        @Suppress("UNCHECKED_CAST")
        val currentIndices = values[4] as List<GlobalIndex>
        val expr = values[5] as String

        val tier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)
        val risk = RiskProfile.fromId(profile?.riskProfile ?: RiskProfile.MODERATE.id)
        DiagnosticsEngine.evaluateDiagnostics(
            tier = tier,
            riskProfile = risk,
            holdings = holdings,
            stocks = currentStocks,
            indices = currentIndices,
            alerts = alerts,
            rawExpression = expr
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DiagnosticsEngine.evaluateDiagnostics(
            tier = SubscriptionTier.STUDENT,
            riskProfile = RiskProfile.MODERATE,
            holdings = emptyList(),
            stocks = GlobalMarketCatalog.initialStocks,
            indices = GlobalMarketCatalog.initialIndices,
            alerts = emptyList(),
            rawExpression = "PORTFOLIO_AUDIT()"
        )
    )

    private val _notificationEvents = MutableSharedFlow<NotificationDispatchEvent>(extraBufferCapacity = 8)
    val notificationEvents: SharedFlow<NotificationDispatchEvent> = _notificationEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                repository.ensureProfileExists(currentUserDisplayName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Profile initialization failed", e)
            }
        }
        startLiveMarketTickLoop()
    }

    private fun startLiveMarketTickLoop() {
        viewModelScope.launch {
            while (true) {
                delay(4500L)
                val tickRandom = Random.Default
                _stocks.value = _stocks.value.map { stock ->
                    val driftPct = (tickRandom.nextDouble() - 0.48) * 0.42
                    val nextPrice = (stock.livePriceUsd * (1.0 + driftPct / 100.0)).coerceAtLeast(1.0)
                    val nextDaily = (stock.dailyChangePct + driftPct * 0.65).coerceIn(-12.0, 12.0)
                    val updatedSpark = (stock.sparkline.drop(1) + nextPrice)
                    stock.copy(
                        livePriceUsd = nextPrice,
                        dailyChangePct = nextDaily,
                        sparkline = updatedSpark
                    )
                }
                _indices.value = _indices.value.map { idx ->
                    val driftPct = (tickRandom.nextDouble() - 0.48) * 0.18
                    val nextLevel = (idx.currentLevel * (1.0 + driftPct / 100.0)).coerceAtLeast(100.0)
                    idx.copy(
                        currentLevel = nextLevel,
                        dailyChangePct = (idx.dailyChangePct + driftPct * 0.5).coerceIn(-8.0, 8.0)
                    )
                }
                evaluatePriceAlerts()
            }
        }
    }

    private suspend fun evaluatePriceAlerts() {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        if (!profile.notificationsEnabled) return
        val alerts = (alertsState.value as? UiState.Success)?.data ?: return
        val stockMap = _stocks.value.associateBy { it.symbol }

        alerts.filter { !it.triggered }.forEach { alert ->
            val stock = stockMap[alert.symbol] ?: return@forEach
            val move = stock.dailyChangePct
            val hit = when (alert.direction) {
                "up" -> move >= alert.targetChangePct
                "down" -> move <= -alert.targetChangePct
                else -> kotlin.math.abs(move) >= alert.targetChangePct
            }
            if (hit) {
                val sign = if (move >= 0) "+" else ""
                val msg = "${stock.symbol} (${stock.exchange}) moved $sign${String.format("%.2f", move)}% crossing your ${alert.targetChangePct}% trigger."
                try {
                    repository.markAlertTriggered(alert.id, msg)
                    _notificationEvents.tryEmit(
                        NotificationDispatchEvent(
                            id = alert.id.hashCode(),
                            title = "FinAdvisor Price Alert: ${stock.symbol}",
                            body = msg
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }
    }

    fun selectTimeRange(range: TimeRange) {
        _selectedRange.value = range
    }

    fun toggleCompareIndex(code: String) {
        val current = _selectedCompareCodes.value
        _selectedCompareCodes.value = if (code in current) {
            if (current.size > 1) current - code else current
        } else {
            current + code
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun clearStockAnalysis() {
        _stockAnalysisState.value = AiTaskState.Idle
    }

    fun switchTier(tier: SubscriptionTier) {
        viewModelScope.launch {
            try {
                repository.updateSubscriptionTier(tier)
                _statusBanner.value = "Switched to ${tier.displayName} (${tier.aiEngineLabel})"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Failed to update subscription tier."
            }
        }
    }

    fun updatePreferences(
        riskProfile: RiskProfile,
        alertThresholdPct: Double,
        notificationsEnabled: Boolean
    ) {
        viewModelScope.launch {
            try {
                repository.updateAdvisoryPreferences(riskProfile, alertThresholdPct, notificationsEnabled)
                _statusBanner.value = "Advisory preferences saved."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Failed to save preferences."
            }
        }
    }

    fun addHolding(stock: MarketStock, shares: Double, avgCostPrice: Double) {
        viewModelScope.launch {
            try {
                repository.addHolding(
                    PortfolioHolding(
                        symbol = stock.symbol,
                        companyName = stock.companyName,
                        exchange = stock.exchange,
                        shares = shares,
                        avgCostPrice = avgCostPrice,
                        currency = stock.currency,
                        sector = stock.sector
                    )
                )
                _statusBanner.value = "Added $shares shares of ${stock.symbol} to real-time portfolio."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Could not add holding."
            }
        }
    }

    fun updateHolding(holding: PortfolioHolding, newShares: Double, newAvgCost: Double) {
        viewModelScope.launch {
            try {
                repository.updateHolding(holding.copy(shares = newShares, avgCostPrice = newAvgCost))
                _statusBanner.value = "Updated ${holding.symbol} position."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Could not update holding."
            }
        }
    }

    fun deleteHolding(holdingId: String) {
        viewModelScope.launch {
            try {
                repository.deleteHolding(holdingId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Could not remove holding."
            }
        }
    }

    fun loadStarterGlobalPortfolio() {
        viewModelScope.launch {
            try {
                repository.seedStarterGlobalPortfolio()
                _statusBanner.value = "Loaded global US + NIKKEI + DAX + BSE starter holdings."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Failed to load starter portfolio."
            }
        }
    }

    fun createAlert(symbol: String, exchange: String, targetPct: Double, direction: String) {
        viewModelScope.launch {
            try {
                repository.createAlert(
                    PriceAlert(
                        symbol = symbol,
                        exchange = exchange,
                        targetChangePct = targetPct,
                        direction = direction,
                        triggerMessage = "Monitoring +/- ${String.format("%.1f", targetPct)}% ($direction)",
                        triggered = false,
                        read = false
                    )
                )
                _statusBanner.value = "Personalized alert created for $symbol."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Failed to create alert."
            }
        }
    }

    fun simulateSignificantPriceMove() {
        viewModelScope.launch {
            val targetStock = _stocks.value.firstOrNull() ?: return@launch
            val simulatedMove = 3.85
            _stocks.value = _stocks.value.map { s ->
                if (s.symbol == targetStock.symbol) {
                    val newPrice = s.livePriceUsd * (1.0 + simulatedMove / 100.0)
                    s.copy(
                        livePriceUsd = newPrice,
                        dailyChangePct = simulatedMove,
                        sparkline = s.sparkline.drop(1) + newPrice
                    )
                } else {
                    s
                }
            }
            val msg = "${targetStock.symbol} (${targetStock.exchange}) surged +$simulatedMove% on AI semiconductor demand."
            try {
                val id = repository.createAlert(
                    PriceAlert(
                        symbol = targetStock.symbol,
                        exchange = targetStock.exchange,
                        targetChangePct = 2.5,
                        direction = "up",
                        triggerMessage = msg,
                        triggered = true,
                        read = false
                    )
                )
                _notificationEvents.tryEmit(
                    NotificationDispatchEvent(
                        id = id.hashCode(),
                        title = "FinAdvisor Volatility Alert: ${targetStock.symbol}",
                        body = msg
                    )
                )
                _statusBanner.value = "Triggered personalized alert for ${targetStock.symbol}."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Failed to trigger alert."
            }
        }
    }

    fun markAlertRead(alertId: String) {
        viewModelScope.launch {
            try {
                repository.markAlertRead(alertId)
            } catch (_: Exception) {
            }
        }
    }

    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAlert(alertId)
            } catch (_: Exception) {
            }
        }
    }

    fun generateInternationalNewsPicks(focusRegion: String) {
        viewModelScope.launch {
            _aiNewsState.value = AiTaskState.Loading
            val profile = (profileState.value as? UiState.Success)?.data
            val tier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)
            val risk = RiskProfile.fromId(profile?.riskProfile ?: RiskProfile.MODERATE.id)
            val holdings = (holdingsState.value as? UiState.Success)?.data.orEmpty()
            val holdingsSummary = if (holdings.isEmpty()) {
                "No holdings yet (looking for initial global share picks)"
            } else {
                holdings.joinToString(", ") { "${it.symbol} (${it.exchange})" }
            }

            try {
                val picks = aiRepository.generateInternationalNewsPicks(
                    tier = tier,
                    focusRegion = focusRegion,
                    riskProfile = risk,
                    holdingsSummary = holdingsSummary
                )
                _aiNewsState.value = AiTaskState.Success(picks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiNewsState.value = AiTaskState.Error(e.localizedMessage ?: "Failed to generate AI news analysis.")
            }
        }
    }

    fun saveAiPick(pick: AiGeneratedNewsPick) {
        viewModelScope.launch {
            val profile = (profileState.value as? UiState.Success)?.data
            val tier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)
            try {
                repository.saveAiReport(
                    AiNewsReport(
                        tierUsed = tier.id,
                        headline = pick.headline,
                        region = pick.region,
                        macroImpact = "${pick.macroImpact} Divergence: ${pick.divergenceEffect}",
                        recommendedTicker = pick.recommendedTicker,
                        actionSignal = pick.actionSignal,
                        confidenceScore = pick.confidenceScore.toLong(),
                        rationale = pick.rationale
                    )
                )
                _statusBanner.value = "Saved AI brief for ${pick.recommendedTicker} to your archive."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Failed to save AI report."
            }
        }
    }

    fun deleteSavedReport(reportId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAiReport(reportId)
            } catch (_: Exception) {
            }
        }
    }

    fun analyzeStockWithAi(stock: MarketStock) {
        viewModelScope.launch {
            _stockAnalysisState.value = AiTaskState.Loading
            val profile = (profileState.value as? UiState.Success)?.data
            val tier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)
            try {
                val result = aiRepository.analyzeStockForTier(stock, tier, _indices.value)
                _stockAnalysisState.value = AiTaskState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _stockAnalysisState.value = AiTaskState.Error(e.localizedMessage ?: "Stock AI deep dive failed.")
            }
        }
    }

    fun generateDivergenceForecast() {
        viewModelScope.launch {
            _divergenceForecastState.value = AiTaskState.Loading
            val profile = (profileState.value as? UiState.Success)?.data
            val tier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)
            try {
                val forecast = aiRepository.generateDivergenceForecast(_indices.value, tier)
                _divergenceForecastState.value = AiTaskState.Success(forecast)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _divergenceForecastState.value = AiTaskState.Error(e.localizedMessage ?: "Forecast generation failed.")
            }
        }
    }

    fun runDiagnosticTrace(expression: String, saveToLocalLedger: Boolean = true) {
        val cleanExpr = expression.trim().ifBlank { "PORTFOLIO_AUDIT()" }
        _scenarioExpression.value = cleanExpr
        _aiDiagnosticState.value = AiTaskState.Idle

        if (saveToLocalLedger && diagnosticAuditRepository != null) {
            val profile = (profileState.value as? UiState.Success)?.data
            val tier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)
            val risk = RiskProfile.fromId(profile?.riskProfile ?: RiskProfile.MODERATE.id)
            val holdings = (holdingsState.value as? UiState.Success)?.data.orEmpty()
            val alerts = (alertsState.value as? UiState.Success)?.data.orEmpty()
            val snapshot = DiagnosticsEngine.evaluateDiagnostics(
                tier = tier,
                riskProfile = risk,
                holdings = holdings,
                stocks = _stocks.value,
                indices = _indices.value,
                alerts = alerts,
                rawExpression = cleanExpr
            )
            viewModelScope.launch {
                try {
                    diagnosticAuditRepository.recordAudit(
                        DiagnosticAuditEntity(
                            userId = currentUserId,
                            scenarioExpression = snapshot.scenarioExpression,
                            tierId = tier.id,
                            weightedBeta = snapshot.weightedBeta,
                            topConcentrationLabel = snapshot.topConcentrationLabel,
                            anomalyCount = snapshot.anomalyCount,
                            contextSummary = snapshot.contextSummary
                        )
                    )
                    _statusBanner.value = "Diagnostic trace logged to local SQLite audit ledger."
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed saving diagnostic audit locally", e)
                }
            }
        }
    }

    fun runAiDeepDiagnosticAudit() {
        viewModelScope.launch {
            _aiDiagnosticState.value = AiTaskState.Loading
            val profile = (profileState.value as? UiState.Success)?.data
            val tier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)
            val risk = RiskProfile.fromId(profile?.riskProfile ?: RiskProfile.MODERATE.id)
            val snapshot = diagnosticReport.value
            try {
                val verdict = aiRepository.runDiagnosticAudit(
                    tier = tier,
                    riskProfile = risk,
                    snapshot = snapshot,
                    indices = _indices.value
                )
                _aiDiagnosticState.value = AiTaskState.Success(verdict)
                diagnosticAuditRepository?.recordAudit(
                    DiagnosticAuditEntity(
                        userId = currentUserId,
                        scenarioExpression = "${snapshot.scenarioExpression} [AI]",
                        tierId = tier.id,
                        weightedBeta = snapshot.weightedBeta,
                        topConcentrationLabel = snapshot.topConcentrationLabel,
                        anomalyCount = snapshot.anomalyCount,
                        contextSummary = snapshot.contextSummary,
                        aiVerdict = verdict.catalystOrigin
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiDiagnosticState.value = AiTaskState.Error(e.localizedMessage ?: "AI deep diagnostic audit failed.")
            }
        }
    }

    fun deleteLocalAudit(id: Long) {
        val repo = diagnosticAuditRepository ?: return
        viewModelScope.launch {
            try {
                repo.deleteAudit(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting local audit", e)
            }
        }
    }

    fun clearLocalAudits() {
        val repo = diagnosticAuditRepository ?: return
        viewModelScope.launch {
            try {
                repo.clearUserAudits(currentUserId)
                _statusBanner.value = "Cleared local SQLite diagnostic trace history."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed clearing local audits", e)
            }
        }
    }

    fun toggleWatchlistStock(stock: MarketStock) {
        viewModelScope.launch {
            val currentList = (watchlistState.value as? UiState.Success)?.data.orEmpty()
            val existing = currentList.firstOrNull { it.symbol.equals(stock.symbol, ignoreCase = true) }
            try {
                if (existing != null) {
                    repository.removeFromWatchlist(existing.id)
                    _statusBanner.value = "Removed ${stock.symbol} from Smart Watchlist."
                } else {
                    val tel = watchlistTelemetry.value[stock.symbol]
                    val preFlagged = tel?.isRealTimeDiverging == true
                    val initialSummary = if (tel != null) {
                        "Live spread ${String.format("%.2f", tel.liveSpreadDeltaPct)}% vs ${tel.dominantDivergenceBenchmark} (N225 r=${String.format("%.2f", tel.nikkeiCorr)}, DAX r=${String.format("%.2f", tel.daxCorr)}, BSE r=${String.format("%.2f", tel.bseCorr)}). Run AI scan for deep tag."
                    } else {
                        "Added to Smart Watchlist. Run AI Divergence Scan to evaluate NIKKEI, DAX, and BSE correlations."
                    }
                    repository.addToWatchlist(
                        WatchlistItem(
                            symbol = stock.symbol,
                            companyName = stock.companyName,
                            exchange = stock.exchange,
                            divergenceTag = if (preFlagged) "DIVERGENCE_ALERT" else "NONE",
                            divergenceAlertActive = preFlagged,
                            divergenceConfidence = if (preFlagged) 72 else 0,
                            aiCorrelationSummary = initialSummary,
                            targetIndices = "NIKKEI 225, DAX 40, BSE SENSEX"
                        )
                    )
                    _statusBanner.value = "Added ${stock.symbol} to real-time Smart Watchlist."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Failed updating Smart Watchlist."
            }
        }
    }

    fun loadStarterWatchlist() {
        viewModelScope.launch {
            try {
                repository.seedStarterWatchlist()
                _statusBanner.value = "Loaded multi-exchange Smart Watchlist (US, NIKKEI, DAX, BSE)."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Failed loading starter watchlist."
            }
        }
    }

    fun removeWatchlistItem(watchlistId: String) {
        viewModelScope.launch {
            try {
                repository.removeFromWatchlist(watchlistId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Failed removing watchlist item."
            }
        }
    }

    fun scanWatchlistForDivergenceAlerts() {
        viewModelScope.launch {
            val watched = (watchlistState.value as? UiState.Success)?.data.orEmpty()
            if (watched.isEmpty()) {
                _statusBanner.value = "Add at least one stock to your Smart Watchlist before running AI scan."
                return@launch
            }
            _watchlistAiScanState.value = AiTaskState.Loading
            val profile = (profileState.value as? UiState.Success)?.data
            val tier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)
            try {
                val aiTags = aiRepository.analyzeWatchlistDivergenceAlerts(
                    tier = tier,
                    watchlist = watched,
                    stocks = _stocks.value,
                    indices = _indices.value,
                    telemetryMap = watchlistTelemetry.value
                )
                val watchedBySymbol = watched.associateBy { it.symbol.uppercase() }
                var alertsTriggered = 0
                var firstAlertSymbol: String? = null

                aiTags.forEach { tag ->
                    val target = watchedBySymbol[tag.symbol.uppercase()] ?: return@forEach
                    repository.updateWatchlistDivergence(
                        watchlistId = target.id,
                        divergenceTag = tag.divergenceTag,
                        divergenceAlertActive = tag.divergenceAlertActive,
                        divergenceConfidence = tag.confidenceScore,
                        aiCorrelationSummary = tag.rationale,
                        targetIndices = tag.correlatedAnchor
                    )
                    if (tag.divergenceAlertActive) {
                        alertsTriggered++
                        if (firstAlertSymbol == null) firstAlertSymbol = tag.symbol
                    }
                }
                _watchlistAiScanState.value = AiTaskState.Success(alertsTriggered)
                _statusBanner.value = if (alertsTriggered > 0) {
                    "AI tagged $alertsTriggered Divergence alert(s) across NIKKEI/DAX/BSE correlations."
                } else {
                    "AI scan complete: watchlist aligned with NIKKEI, DAX & BSE benchmarks."
                }
                if (alertsTriggered > 0 && profile?.notificationsEnabled == true && firstAlertSymbol != null) {
                    _notificationEvents.tryEmit(
                        NotificationDispatchEvent(
                            id = ("div_" + firstAlertSymbol).hashCode(),
                            title = "FinAdvisor AI Divergence Alert: $firstAlertSymbol",
                            body = "$alertsTriggered watched stock(s) flagged for NIKKEI/DAX/BSE correlation divergence."
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _watchlistAiScanState.value = AiTaskState.Error(
                    e.localizedMessage ?: "AI watchlist divergence scan failed."
                )
            }
        }
    }

    private companion object {
        const val TAG = "FinAdvisorVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
