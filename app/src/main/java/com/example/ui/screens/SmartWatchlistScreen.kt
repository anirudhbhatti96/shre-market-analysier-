package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.GlobalIndex
import com.example.data.model.MarketStock
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserAdvisoryProfile
import com.example.data.model.WatchlistItem
import com.example.data.watchlist.CorrelationTelemetry
import com.example.data.watchlist.DivergenceLevel
import com.example.ui.components.SparklineChart
import com.example.ui.theme.CrimsonVolatility
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldAlpha
import com.example.ui.theme.SovereignGold
import com.example.ui.viewmodel.AiTaskState
import com.example.ui.viewmodel.UiState
import java.util.Locale
import kotlin.math.abs

@Composable
fun SmartWatchlistScreen(
    profileState: UiState<UserAdvisoryProfile>,
    watchlistState: UiState<List<WatchlistItem>>,
    stocks: List<MarketStock>,
    indices: List<GlobalIndex>,
    telemetryMap: Map<String, CorrelationTelemetry>,
    scanState: AiTaskState<Int>,
    onToggleWatchlistStock: (MarketStock) -> Unit,
    onLoadStarterWatchlist: () -> Unit,
    onRemoveWatchlistItem: (String) -> Unit,
    onRunAiDivergenceScan: () -> Unit,
    onSelectStockDeepDive: (MarketStock) -> Unit,
    onCreateHedgeAlert: (MarketStock) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val activeTier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)
    val stockMap = remember(stocks) { stocks.associateBy { it.symbol } }
    val watched = (watchlistState as? UiState.Success)?.data.orEmpty()

    var showDivergenceOnly by remember { mutableStateOf(false) }
    var showAddAssetDialog by remember { mutableStateOf(false) }

    val divergenceAlertCount = watched.count { item ->
        item.divergenceAlertActive || (telemetryMap[item.symbol]?.isRealTimeDiverging == true)
    }

    val displayedItems = remember(watched, showDivergenceOnly, telemetryMap) {
        val base = if (!showDivergenceOnly) {
            watched
        } else {
            watched.filter { item ->
                item.divergenceAlertActive || (telemetryMap[item.symbol]?.isRealTimeDiverging == true)
            }
        }
        base.sortedWith(
            compareByDescending<WatchlistItem> { it.divergenceAlertActive }
                .thenByDescending { abs(telemetryMap[it.symbol]?.drift ?: 0.0) }
        )
    }

    val regionalBenchmarks = remember(indices) {
        indices.filter { it.code in setOf("N225", "DAX", "SENSEX") }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("watchlist_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Smart Watchlist",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Real-time NIKKEI, DAX & BSE correlation divergence radar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddAssetDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_add_watchlist_dialog_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Track")
                }
            }
        }

        // AI Divergence Scanner Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Radar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Historical Correlation Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Model: ${activeTier.aiModelId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Surface(
                            color = (if (divergenceAlertCount > 0) SovereignGold else EmeraldAlpha).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "$divergenceAlertCount Divergence",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (divergenceAlertCount > 0) SovereignGold else EmeraldAlpha,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Live Regional Benchmark Strip (NIKKEI, DAX, BSE)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        regionalBenchmarks.forEach { idx ->
                            val pos = idx.dailyChangePct >= 0
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = idx.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${if (pos) "+" else ""}${String.format(Locale.US, "%.2f", idx.dailyChangePct)}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pos) EmeraldAlpha else CrimsonVolatility
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onRunAiDivergenceScan,
                        enabled = scanState !is AiTaskState.Loading && watched.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("scan_watchlist_divergence_button")
                    ) {
                        if (scanState is AiTaskState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Scanning NIKKEI, DAX & BSE...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("AI Tag Divergence Alerts")
                        }
                    }

                    AnimatedVisibility(visible = scanState is AiTaskState.Error) {
                        val err = (scanState as? AiTaskState.Error)?.message.orEmpty()
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Filter Chips
        if (watched.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showDivergenceOnly,
                        onClick = { showDivergenceOnly = false },
                        label = { Text("All Watched (${watched.size})") },
                        modifier = Modifier.testTag("filter_all_watched_chip")
                    )
                    FilterChip(
                        selected = showDivergenceOnly,
                        onClick = { showDivergenceOnly = true },
                        label = { Text("Divergence Alerts ($divergenceAlertCount)") },
                        modifier = Modifier.testTag("filter_divergence_only_chip")
                    )
                }
            }
        }

        // Watchlist Content
        when (watchlistState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is UiState.Error -> {
                item {
                    Text(
                        text = watchlistState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is UiState.Success -> {
                if (watched.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Hub,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Smart Watchlist is empty",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Track global equities in real time and let AI detect divergence against historical NIKKEI 225, DAX 40, and BSE SENSEX correlations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onLoadStarterWatchlist,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("load_starter_watchlist_button")
                                ) {
                                    Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Load Multi-Exchange Starter Watchlist")
                                }
                            }
                        }
                    }
                } else {
                    items(displayedItems, key = { it.id }) { item ->
                        val liveStock = stockMap[item.symbol]
                        val tel = telemetryMap[item.symbol]
                        val hasAlert = item.divergenceAlertActive || (tel?.isRealTimeDiverging == true)
                        val tagColor = when {
                            item.divergenceTag == "CORRELATION_BREAK" -> CrimsonVolatility
                            item.divergenceAlertActive -> SovereignGold
                            tel?.isRealTimeDiverging == true -> ElectricCyan
                            else -> EmeraldAlpha
                        }
                        val tagLabel = when {
                            item.divergenceTag != "NONE" ->
                                "${item.divergenceTag.replace('_', ' ')} • ${item.divergenceConfidence}%"
                            tel?.isRealTimeDiverging == true -> "LIVE SPREAD DIVERGENCE"
                            else -> "CORRELATION ALIGNED"
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                width = if (hasAlert) 1.5.dp else 1.dp,
                                color = if (hasAlert) tagColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Top Header Row: Stock + Live Tick Price + Sparkline
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.symbol,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = item.exchange,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = item.companyName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (liveStock != null) {
                                        val positive = liveStock.dailyChangePct >= 0
                                        SparklineChart(
                                            points = liveStock.sparkline,
                                            lineColor = if (positive) EmeraldAlpha else CrimsonVolatility,
                                            modifier = Modifier
                                                .width(56.dp)
                                                .height(28.dp)
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$${String.format(Locale.US, "%.2f", liveStock.livePriceUsd)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${if (positive) "+" else ""}${String.format(Locale.US, "%.2f", liveStock.dailyChangePct)}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (positive) EmeraldAlpha else CrimsonVolatility
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onRemoveWatchlistItem(item.id) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("remove_watchlist_${item.symbol.lowercase()}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.BookmarkRemove,
                                            contentDescription = "Remove ${item.symbol} from watchlist",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // AI Divergence Alert Badge + Short/Long Drift Level
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = tagColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (hasAlert) Icons.Filled.WarningAmber else Icons.Filled.BookmarkAdded,
                                                contentDescription = null,
                                                tint = tagColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = tagLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = tagColor
                                            )
                                        }
                                    }
                                    if (tel != null) {
                                        val levelColor = when (tel.level) {
                                            DivergenceLevel.ALIGNED -> EmeraldAlpha
                                            DivergenceLevel.WATCH -> SovereignGold
                                            DivergenceLevel.DIVERGING -> CrimsonVolatility
                                        }
                                        Surface(
                                            color = levelColor.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = tel.level.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = levelColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }

                                // Real-Time Correlation Matrix vs NIKKEI, DAX, BSE
                                if (tel != null) {
                                    Text(
                                        text = if (activeTier == SubscriptionTier.ADULT) tel.quantReason else tel.plainReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        CorrStat("N225 r", tel.nikkeiCorr)
                                        CorrStat("DAX r", tel.daxCorr)
                                        CorrStat("BSE r", tel.bseCorr)
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Live Spread",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val spreadSign = if (tel.liveSpreadDeltaPct >= 0) "+" else ""
                                            Text(
                                                text = "$spreadSign${String.format(Locale.US, "%.2f", tel.liveSpreadDeltaPct)}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tel.isRealTimeDiverging) SovereignGold else EmeraldAlpha
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = item.aiCorrelationSummary.ifBlank {
                                        "Benchmarks: ${item.targetIndices}. Tap AI Tag Divergence Alerts above to analyze correlation breakouts."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (liveStock != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onSelectStockDeepDive(liveStock) },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("watchlist_deep_dive_${item.symbol.lowercase()}")
                                        ) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("AI Deep Dive")
                                        }
                                        OutlinedButton(
                                            onClick = { onCreateHedgeAlert(liveStock) },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("watchlist_alert_${item.symbol.lowercase()}")
                                        ) {
                                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Alert +/-2.5%")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = "Educational analysis only, not investment advice.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddAssetDialog) {
        AddToWatchlistDialog(
            stocks = stocks,
            watchedSymbols = watched.map { it.symbol.uppercase() }.toSet(),
            onToggleStock = onToggleWatchlistStock,
            onDismiss = { showAddAssetDialog = false }
        )
    }
}

@Composable
private fun CorrStat(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val sign = if (value >= 0) "+" else ""
        Text(
            text = "$sign${String.format(Locale.US, "%.2f", value)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (value >= 0.35) EmeraldAlpha else if (value <= -0.15) CrimsonVolatility else Color.Unspecified
        )
    }
}

@Composable
private fun AddToWatchlistDialog(
    stocks: List<MarketStock>,
    watchedSymbols: Set<String>,
    onToggleStock: (MarketStock) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Smart Watchlist", style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn(
                modifier = Modifier.height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stocks, key = { it.symbol }) { stock ->
                    val isWatched = stock.symbol.uppercase() in watchedSymbols
                    Surface(
                        color = if (isWatched) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleStock(stock) }
                            .testTag("toggle_watchlist_option_${stock.symbol.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${stock.symbol} (${stock.exchange})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stock.companyName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = if (isWatched) "Watching" else "+ Track",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isWatched) EmeraldAlpha else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
