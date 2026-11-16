package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MarketStock
import com.example.data.model.PortfolioHolding
import com.example.data.model.PriceAlert
import com.example.ui.components.PortfolioAllocationDonut
import com.example.ui.theme.CrimsonVolatility
import com.example.ui.theme.EmeraldAlpha
import com.example.ui.theme.SovereignGold
import com.example.ui.viewmodel.UiState
import com.example.util.PriceNotificationHelper

@Composable
fun PortfolioAlertsScreen(
    holdingsState: UiState<List<PortfolioHolding>>,
    alertsState: UiState<List<PriceAlert>>,
    stocks: List<MarketStock>,
    onAddHolding: (MarketStock, Double, Double) -> Unit,
    onDeleteHolding: (String) -> Unit,
    onLoadStarterPortfolio: () -> Unit,
    onCreateAlert: (String, String, Double, String) -> Unit,
    onSimulatePriceMove: () -> Unit,
    onMarkAlertRead: (String) -> Unit,
    onDeleteAlert: (String) -> Unit,
    onNavigateDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotificationPerm by remember {
        mutableStateOf(PriceNotificationHelper.hasNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPerm = granted
    }

    var showAddHoldingDialog by remember { mutableStateOf(false) }
    var showAddAlertDialog by remember { mutableStateOf(false) }

    val stockMap = remember(stocks) { stocks.associateBy { it.symbol } }
    val holdings = (holdingsState as? UiState.Success)?.data.orEmpty()
    val alerts = (alertsState as? UiState.Success)?.data.orEmpty()

    val totalValue = holdings.sumOf { h ->
        val live = stockMap[h.symbol]?.livePriceUsd ?: h.avgCostPrice
        h.shares * live
    }
    val totalCost = holdings.sumOf { it.shares * it.avgCostPrice }
    val totalPnl = totalValue - totalCost
    val totalPnlPct = if (totalCost > 0.0) (totalPnl / totalCost) * 100.0 else 0.0

    val exchangeAllocations = remember(holdings, stockMap) {
        holdings.groupBy { it.exchange }
            .map { (ex, list) ->
                val value = list.sumOf { h -> h.shares * (stockMap[h.symbol]?.livePriceUsd ?: h.avgCostPrice) }
                ex to value
            }
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Portfolio & Price Alerts",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Real-time multi-exchange valuation & volatility triggers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddHoldingDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_add_holding_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Asset")
                }
            }
        }

        // Real-Time Portfolio Summary & Donut
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (holdingsState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is UiState.Error -> {
                            Text(
                                text = holdingsState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        is UiState.Success -> {
                            if (holdings.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PieChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Your portfolio is empty",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Add any US, NIKKEI, DAX, or BSE share, or load a diversified global starter basket.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = onLoadStarterPortfolio,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("load_starter_portfolio_button")
                                    ) {
                                        Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Load Global Starter Portfolio")
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Live Market Value",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$${String.format("%,.2f", totalValue)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    val sign = if (totalPnl >= 0) "+" else ""
                                    Surface(
                                        color = (if (totalPnl >= 0) EmeraldAlpha else CrimsonVolatility).copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "$sign$${String.format("%.2f", totalPnl)} ($sign${String.format("%.2f", totalPnlPct)}%)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (totalPnl >= 0) EmeraldAlpha else CrimsonVolatility,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                PortfolioAllocationDonut(
                                    allocations = exchangeAllocations,
                                    totalValueFormatted = "$${String.format("%,.0f", totalValue)}"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Holdings List
        if (holdings.isNotEmpty()) {
            item {
                Text(
                    text = "Active Global Positions",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(holdings, key = { it.id }) { h ->
                val livePrice = stockMap[h.symbol]?.livePriceUsd ?: h.avgCostPrice
                val dailyPct = stockMap[h.symbol]?.dailyChangePct ?: 0.0
                val positionVal = h.shares * livePrice
                val pnl = (livePrice - h.avgCostPrice) * h.shares
                val positive = pnl >= 0

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = h.symbol,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = h.exchange,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${String.format("%.2f", h.shares)} sh • Avg $${String.format("%.2f", h.avgCostPrice)} • Live $${String.format("%.2f", livePrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${String.format("%,.2f", positionVal)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${if (positive) "+" else ""}$${String.format("%.2f", pnl)} (${if (dailyPct >= 0) "+" else ""}${String.format("%.2f", dailyPct)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (positive) EmeraldAlpha else CrimsonVolatility
                            )
                        }
                        IconButton(
                            onClick = { onDeleteHolding(h.id) },
                            modifier = Modifier.testTag("delete_holding_${h.symbol.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Remove ${h.symbol}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Personalized Price-Change Alerts Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personalized Price Alerts",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(
                    onClick = { showAddAlertDialog = true },
                    modifier = Modifier.testTag("open_add_alert_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Alert")
                }
            }
        }

        if (!hasNotificationPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable System Notifications",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Receive instant Android alerts when your assets cross volatility thresholds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.testTag("grant_notification_permission_button")
                        ) {
                            Text("Enable")
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    if (!hasNotificationPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onSimulatePriceMove()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("simulate_price_alert_button")
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Simulate Significant Price Change & Notify")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onNavigateDiagnostics,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("portfolio_diagnostics_cta")
            ) {
                Icon(Icons.Filled.Biotech, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Quant Strategy & Risk Audit Lab")
            }
        }

        if (alerts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "No personalized price alerts yet. Create a threshold rule or test a live price alert above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                val accent = if (alert.triggered) SovereignGold else MaterialTheme.colorScheme.primary
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = accent
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${alert.symbol} (${alert.exchange}) • ${if (alert.triggered) "TRIGGERED" else "WATCHING +/- ${alert.targetChangePct}%"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                            Text(
                                text = alert.triggerMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (alert.triggered && !alert.read) {
                            IconButton(
                                onClick = { onMarkAlertRead(alert.id) },
                                modifier = Modifier.testTag("mark_alert_read_${alert.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircleOutline,
                                    contentDescription = "Mark alert read",
                                    tint = EmeraldAlpha
                                )
                            }
                        }
                        IconButton(
                            onClick = { onDeleteAlert(alert.id) },
                            modifier = Modifier.testTag("delete_alert_${alert.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete alert",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddHoldingDialog) {
        AddHoldingDialog(
            stocks = stocks,
            onDismiss = { showAddHoldingDialog = false },
            onConfirm = { stock, shares, cost ->
                onAddHolding(stock, shares, cost)
                showAddHoldingDialog = false
            }
        )
    }

    if (showAddAlertDialog) {
        AddAlertDialog(
            stocks = stocks,
            onDismiss = { showAddAlertDialog = false },
            onConfirm = { stock, pct, dir ->
                onCreateAlert(stock.symbol, stock.exchange, pct, dir)
                showAddAlertDialog = false
            }
        )
    }
}

@Composable
private fun AddHoldingDialog(
    stocks: List<MarketStock>,
    onDismiss: () -> Unit,
    onConfirm: (MarketStock, Double, Double) -> Unit
) {
    var selectedStock by remember { mutableStateOf(stocks.first()) }
    var sharesText by remember { mutableStateOf("5") }
    var costText by remember(selectedStock) {
        mutableStateOf(String.format("%.2f", selectedStock.livePriceUsd))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Global Holding", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Market Asset:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stocks.take(4).forEach { st ->
                        FilterChip(
                            selected = selectedStock.symbol == st.symbol,
                            onClick = { selectedStock = st },
                            label = { Text(st.symbol, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stocks.drop(4).take(4).forEach { st ->
                        FilterChip(
                            selected = selectedStock.symbol == st.symbol,
                            onClick = { selectedStock = st },
                            label = { Text(st.symbol, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = sharesText,
                    onValueChange = { sharesText = it },
                    label = { Text("Quantity (Shares)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("holding_shares_input")
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Avg Cost Basis (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("holding_cost_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shares = sharesText.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
                    val cost = costText.toDoubleOrNull()?.takeIf { it > 0 } ?: selectedStock.livePriceUsd
                    onConfirm(selectedStock, shares, cost)
                },
                modifier = Modifier.testTag("confirm_add_holding_button")
            ) {
                Text("Save Position")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddAlertDialog(
    stocks: List<MarketStock>,
    onDismiss: () -> Unit,
    onConfirm: (MarketStock, Double, String) -> Unit
) {
    var selectedStock by remember { mutableStateOf(stocks.first()) }
    var pctText by remember { mutableStateOf("2.5") }
    var direction by remember { mutableStateOf("both") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Personalized Price Alert", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    stocks.take(4).forEach { st ->
                        FilterChip(
                            selected = selectedStock.symbol == st.symbol,
                            onClick = { selectedStock = st },
                            label = { Text(st.symbol, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = pctText,
                    onValueChange = { pctText = it },
                    label = { Text("Trigger Move (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_pct_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("both", "up", "down").forEach { dir ->
                        FilterChip(
                            selected = direction == dir,
                            onClick = { direction = dir },
                            label = { Text(dir.uppercase()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pct = pctText.toDoubleOrNull()?.coerceIn(0.25, 50.0) ?: 2.5
                    onConfirm(selectedStock, pct, direction)
                },
                modifier = Modifier.testTag("confirm_add_alert_button")
            ) {
                Text("Create Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
