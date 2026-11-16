package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MarketStock
import com.example.data.repository.StockAiAnalysis
import com.example.ui.theme.CrimsonVolatility
import com.example.ui.theme.EmeraldAlpha
import com.example.ui.viewmodel.AiTaskState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDeepDiveSheet(
    stock: MarketStock,
    analysisState: AiTaskState<StockAiAnalysis>,
    isWatched: Boolean,
    onToggleWatchlist: () -> Unit,
    onWatchAlert: () -> Unit,
    onAddHolding: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val positive = stock.dailyChangePct >= 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stock.symbol} • ${stock.companyName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stock.exchange} • P/E ${stock.peRatio}x • Beta ${stock.beta}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format("%.2f", stock.livePriceUsd)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${if (positive) "+" else ""}${String.format("%.2f", stock.dailyChangePct)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (positive) EmeraldAlpha else CrimsonVolatility
                    )
                }
            }

            SparklineChart(
                points = stock.sparkline,
                lineColor = if (positive) EmeraldAlpha else CrimsonVolatility,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )

            FactorBarCluster(factors = stock.factors)

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (analysisState) {
                        is AiTaskState.Idle -> {
                            Text("Preparing Gemini AI thesis...", style = MaterialTheme.typography.bodySmall)
                        }

                        is AiTaskState.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Running AI consultancy analysis...", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        is AiTaskState.Error -> {
                            Text(
                                text = analysisState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        is AiTaskState.Success -> {
                            val analysis = analysisState.data
                            Text(
                                text = "${analysis.actionSignal} • ${analysis.confidenceScore}% Conviction (${analysis.tier.displayName})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = analysis.thesis,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Global Catalyst: ${analysis.globalCatalyst}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Guardrail: ${analysis.riskGuardrail}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onWatchAlert,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_watch_alert_button")
                ) {
                    Text("Watch +/- 2.5%")
                }
                Button(
                    onClick = onAddHolding,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_add_holding_button")
                ) {
                    Text("Add 5 Shares")
                }
            }
            OutlinedButton(
                onClick = onToggleWatchlist,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sheet_toggle_watchlist_button")
            ) {
                Text(if (isWatched) "Watching in Smart Watchlist (Remove)" else "Add to AI Smart Watchlist")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
