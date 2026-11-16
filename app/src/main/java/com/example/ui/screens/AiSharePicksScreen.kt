package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Newspaper
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AiGeneratedNewsPick
import com.example.data.model.AiNewsReport
import com.example.data.model.MarketRegion
import com.example.data.model.MarketStock
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserAdvisoryProfile
import com.example.ui.components.FactorBarCluster
import com.example.ui.components.SparklineChart
import com.example.ui.theme.CrimsonVolatility
import com.example.ui.theme.EmeraldAlpha
import com.example.ui.theme.SovereignGold
import com.example.ui.viewmodel.AiTaskState
import com.example.ui.viewmodel.UiState

@Composable
fun AiSharePicksScreen(
    profileState: UiState<UserAdvisoryProfile>,
    stocks: List<MarketStock>,
    aiNewsState: AiTaskState<List<AiGeneratedNewsPick>>,
    savedReportsState: UiState<List<AiNewsReport>>,
    watchedSymbols: Set<String>,
    onGenerateNewsPicks: (String) -> Unit,
    onSaveAiPick: (AiGeneratedNewsPick) -> Unit,
    onDeleteSavedReport: (String) -> Unit,
    onSelectStockDeepDive: (MarketStock) -> Unit,
    onQuickAddHolding: (MarketStock) -> Unit,
    onToggleWatchlistStock: (MarketStock) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val activeTier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)
    var selectedRegion by remember { mutableStateOf(MarketRegion.GLOBAL) }
    val regions = MarketRegion.entries

    val filteredStocks = remember(stocks, selectedRegion, activeTier) {
        val list = if (selectedRegion == MarketRegion.GLOBAL) stocks else stocks.filter { it.region == selectedRegion }
        if (activeTier == SubscriptionTier.STUDENT) {
            list.sortedByDescending { it.studentScore }
        } else {
            list.sortedByDescending { it.adultAlphaScore }
        }
    }

    val savedReports = (savedReportsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "AI News & Share Picker",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Powered by ${activeTier.aiEngineLabel}. Generates international macro catalysts and maps which US or Global share to pick.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Region Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                regions.forEach { region ->
                    FilterChip(
                        selected = region == selectedRegion,
                        onClick = { selectedRegion = region },
                        label = { Text(region.label) },
                        modifier = Modifier.testTag("region_chip_${region.name.lowercase()}")
                    )
                }
            }
        }

        // Automatic International News Generator Card
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Newspaper,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "International Macro Catalyst Synthesizer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Model: ${activeTier.aiModelId} (${activeTier.displayName} Tier)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Button(
                        onClick = { onGenerateNewsPicks(selectedRegion.label) },
                        enabled = aiNewsState !is AiTaskState.Loading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_ai_news_button")
                    ) {
                        if (aiNewsState is AiTaskState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Analyzing Global News & Share Impact...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Analyze ${selectedRegion.label} News & Pick Shares")
                        }
                    }

                    AnimatedVisibility(visible = aiNewsState is AiTaskState.Error) {
                        val err = (aiNewsState as? AiTaskState.Error)?.message.orEmpty()
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Generated AI News Picks
        if (aiNewsState is AiTaskState.Success) {
            items(aiNewsState.data) { pick ->
                val sigColor = when (pick.actionSignal) {
                    "BUY", "ACCUMULATE" -> EmeraldAlpha
                    "TRIM" -> CrimsonVolatility
                    else -> SovereignGold
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, sigColor.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = sigColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${pick.actionSignal} ${pick.recommendedTicker} • ${pick.confidenceScore}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = sigColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = pick.region,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = pick.headline,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Macro Effect: ${pick.macroImpact}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Exchange Divergence: ${pick.divergenceEffect}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = pick.rationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { onSaveAiPick(pick) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_ai_pick_${pick.recommendedTicker.lowercase()}")
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Brief to Archive")
                        }
                    }
                }
            }
        }

        // Ranked Global Equity Screener (US + TSE + XETRA + BSE)
        item {
            Text(
                text = "Screened Equities (${selectedRegion.label})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(filteredStocks, key = { it.symbol }) { stock ->
            val score = if (activeTier == SubscriptionTier.STUDENT) stock.studentScore else stock.adultAlphaScore
            val positive = stock.dailyChangePct >= 0
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectStockDeepDive(stock) }
                    .testTag("screener_stock_${stock.symbol.lowercase()}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stock.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = stock.exchange,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${stock.companyName} • ${stock.marketCapLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        SparklineChart(
                            points = stock.sparkline,
                            lineColor = if (positive) EmeraldAlpha else CrimsonVolatility,
                            modifier = Modifier
                                .width(64.dp)
                                .height(30.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${String.format("%.2f", stock.livePriceUsd)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${if (positive) "+" else ""}${String.format("%.2f", stock.dailyChangePct)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (positive) EmeraldAlpha else CrimsonVolatility
                            )
                        }
                        val isWatched = stock.symbol.uppercase() in watchedSymbols
                        IconButton(
                            onClick = { onToggleWatchlistStock(stock) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("screener_watchlist_${stock.symbol.lowercase()}")
                        ) {
                            Icon(
                                imageVector = if (isWatched) Icons.Filled.BookmarkAdded else Icons.Filled.BookmarkAdd,
                                contentDescription = if (isWatched) "Remove ${stock.symbol} from Smart Watchlist" else "Add ${stock.symbol} to Smart Watchlist",
                                tint = if (isWatched) EmeraldAlpha else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FactorBarCluster(factors = stock.factors)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSelectStockDeepDive(stock) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("ai_deep_dive_${stock.symbol.lowercase()}")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI Thesis ($score)")
                        }
                        Button(
                            onClick = { onQuickAddHolding(stock) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("track_stock_${stock.symbol.lowercase()}")
                        ) {
                            Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Track Asset")
                        }
                    }
                }
            }
        }

        // Saved Cloud AI Briefs
        if (savedReports.isNotEmpty()) {
            item {
                Text(
                    text = "Saved Advisory Briefs",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(savedReports, key = { it.id }) { report ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${report.actionSignal} ${report.recommendedTicker} • ${report.tierUsed.uppercase()} (${report.confidenceScore}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = report.headline,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = report.rationale,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onDeleteSavedReport(report.id) },
                            modifier = Modifier.testTag("delete_report_${report.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete saved report",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
