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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.model.GlobalIndex
import com.example.data.model.MarketStock
import com.example.data.model.PortfolioHolding
import com.example.data.model.PredictiveTrendAlert
import com.example.data.model.SubscriptionTier
import com.example.data.model.TimeRange
import com.example.data.model.UserAdvisoryProfile
import com.example.ui.components.SparklineChart
import com.example.ui.components.VolatilityConeChart
import com.example.ui.components.indexPaletteMap
import com.example.ui.theme.CrimsonVolatility
import com.example.ui.theme.EmeraldAlpha
import com.example.ui.theme.SovereignGold
import com.example.ui.viewmodel.UiState

@Composable
fun PulseScreen(
    profileState: UiState<UserAdvisoryProfile>,
    holdingsState: UiState<List<PortfolioHolding>>,
    indices: List<GlobalIndex>,
    stocks: List<MarketStock>,
    predictiveAlerts: List<PredictiveTrendAlert>,
    unreadAlertsCount: Int,
    onNavigateCompare: () -> Unit,
    onNavigateAiPicks: () -> Unit,
    onNavigatePortfolio: () -> Unit,
    onNavigatePlans: () -> Unit,
    onNavigateDiagnostics: () -> Unit,
    onNavigateWatchlist: () -> Unit,
    watchlistCount: Int,
    watchlistDivergenceCount: Int,
    onSelectStockForDeepDive: (MarketStock) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val activeTier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)
    val holdings = (holdingsState as? UiState.Success)?.data.orEmpty()
    val stockMap = stocks.associateBy { it.symbol }

    val totalValue = holdings.sumOf { h ->
        val live = stockMap[h.symbol]?.livePriceUsd ?: h.avgCostPrice
        h.shares * live
    }
    val totalCost = holdings.sumOf { it.shares * it.avgCostPrice }
    val totalGain = totalValue - totalCost
    val totalGainPct = if (totalCost > 0.0) (totalGain / totalCost) * 100.0 else 0.0

    val rankedPicks = if (activeTier == SubscriptionTier.STUDENT) {
        stocks.sortedByDescending { it.studentScore }.take(4)
    } else {
        stocks.sortedByDescending { it.adultAlphaScore }.take(4)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Global Consultancy Header Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_market_hero),
                        contentDescription = "FinAdvisor global market overview banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0x55070D17), Color(0xEC070D17))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (activeTier == SubscriptionTier.STUDENT) {
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                                } else {
                                    SovereignGold.copy(alpha = 0.22f)
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = onNavigatePlans)
                                    .testTag("pulse_tier_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (activeTier == SubscriptionTier.STUDENT) Icons.Filled.School else Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (activeTier == SubscriptionTier.STUDENT) MaterialTheme.colorScheme.tertiary else SovereignGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${activeTier.displayName} • ${activeTier.aiModelId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = onNavigatePortfolio)
                                    .testTag("pulse_alerts_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = "Alerts",
                                        tint = if (unreadAlertsCount > 0) SovereignGold else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (unreadAlertsCount > 0) "$unreadAlertsCount Live Alerts" else "Alerts Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Real-Time Global Portfolio",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (holdings.isEmpty()) "$0.00" else "$${String.format("%,.2f", totalValue)}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White
                                )
                                if (holdings.isNotEmpty()) {
                                    val sign = if (totalGain >= 0) "+" else ""
                                    Text(
                                        text = "$sign${String.format("%.2f", totalGainPct)}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalGain >= 0) EmeraldAlpha else CrimsonVolatility,
                                        modifier = Modifier.padding(bottom = 5.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onNavigateAiPicks,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pulse_ai_picks_cta")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("AI News Picks", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = onNavigateCompare,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.14f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pulse_compare_cta")
                            ) {
                                Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Global Indices", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        // Quant Strategy Audit Lab Quick Launch
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateDiagnostics)
                    .testTag("pulse_diagnostics_cta"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Biotech,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quant Strategy Audit Lab",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Run what-if macro scripts, execution trace & metric anomalies",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Quant Audit Lab",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Smart Watchlist Divergence Radar Quick Launch
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateWatchlist)
                    .testTag("pulse_watchlist_cta"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (watchlistDivergenceCount > 0) SovereignGold.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = (if (watchlistDivergenceCount > 0) SovereignGold else EmeraldAlpha).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Radar,
                                contentDescription = null,
                                tint = if (watchlistDivergenceCount > 0) SovereignGold else EmeraldAlpha
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Smart Watchlist",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (watchlistCount == 0) {
                                "Track stocks with AI NIKKEI, DAX & BSE divergence tags"
                            } else {
                                "$watchlistCount watched • $watchlistDivergenceCount Divergence alert(s)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open AI Smart Watchlist",
                        tint = if (watchlistDivergenceCount > 0) SovereignGold else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 2. Global Markets Ticker Strip (US, NIKKEI, DAX, BSE, FTSE)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Global Exchange Pulse",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onNavigateCompare,
                    modifier = Modifier.testTag("pulse_view_divergence_button")
                ) {
                    Text("Divergence Lab")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(indices, key = { it.code }) { idx ->
                    val accent = indexPaletteMap[idx.code] ?: EmeraldAlpha
                    val positive = idx.dailyChangePct >= 0
                    val pts = idx.historicalSeries[TimeRange.ONE_MONTH]?.map { it.level } ?: emptyList()

                    Card(
                        modifier = Modifier
                            .width(176.dp)
                            .clickable(onClick = onNavigateCompare)
                            .testTag("index_card_${idx.code.lowercase()}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = idx.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                            }
                            Text(
                                text = idx.country,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SparklineChart(
                                points = pts,
                                lineColor = if (positive) EmeraldAlpha else CrimsonVolatility,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("%,.0f", idx.currentLevel),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val sign = if (positive) "+" else ""
                                Text(
                                    text = "$sign${String.format("%.2f", idx.dailyChangePct)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (positive) EmeraldAlpha else CrimsonVolatility
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Predictive Volatility & Trend Alerts
        item {
            Text(
                text = "Predictive Volatility Radar",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(predictiveAlerts, key = { it.id }) { alert ->
            val highVol = alert.volatilityRiskPct >= 60
            val badgeColor = if (highVol) CrimsonVolatility else EmeraldAlpha
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (highVol) Icons.Filled.WarningAmber else Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${alert.indexOrTicker} • ${alert.region}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = badgeColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Vol Risk ${alert.volatilityRiskPct}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = alert.regime,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = alert.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        VolatilityConeChart(
                            annualVolPct = alert.volatilityRiskPct / 3.0,
                            modifier = Modifier
                                .width(92.dp)
                                .height(54.dp)
                        )
                    }
                    Text(
                        text = alert.suggestedAction,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 4. Tier-Ranked Top Share Picks
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Top Share Picks (${activeTier.displayName})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (activeTier == SubscriptionTier.STUDENT) {
                            "Ranked for safety, low volatility & compounding"
                        } else {
                            "Ranked by institutional multi-factor Alpha score"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onNavigateAiPicks,
                    modifier = Modifier.testTag("pulse_all_picks_button")
                ) {
                    Text("All Picks")
                }
            }
        }

        items(rankedPicks, key = { it.symbol }) { stock ->
            val score = if (activeTier == SubscriptionTier.STUDENT) stock.studentScore else stock.adultAlphaScore
            val verdict = if (activeTier == SubscriptionTier.STUDENT) stock.studentVerdict else stock.adultVerdict
            val positive = stock.dailyChangePct >= 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectStockForDeepDive(stock) }
                    .testTag("pulse_pick_${stock.symbol.lowercase()}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$score",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stock.symbol,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${stock.exchange} • ${stock.sector}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = verdict,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${String.format("%.2f", stock.livePriceUsd)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (positive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (positive) EmeraldAlpha else CrimsonVolatility,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "${if (positive) "+" else ""}${String.format("%.2f", stock.dailyChangePct)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (positive) EmeraldAlpha else CrimsonVolatility
                            )
                        }
                    }
                }
            }
        }
    }
}
