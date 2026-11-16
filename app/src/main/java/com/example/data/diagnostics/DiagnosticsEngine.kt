package com.example.data.diagnostics

import com.example.data.model.GlobalIndex
import com.example.data.model.MarketStock
import com.example.data.model.PortfolioHolding
import com.example.data.model.PriceAlert
import com.example.data.model.RiskProfile
import com.example.data.model.SubscriptionTier
import java.util.Locale
import kotlin.math.abs

object DiagnosticsEngine {

    val builtInPresets: List<ScenarioPreset> = listOf(
        ScenarioPreset(
            id = "portfolio_audit",
            label = "Portfolio Health",
            expression = "PORTFOLIO_AUDIT()",
            description = "Full diagnostic scan across holdings, beta drift, regional concentration & alert coverage."
        ),
        ScenarioPreset(
            id = "usdjpy_beta",
            label = "JPY Carry & Beta",
            expression = "USD/JPY +2.5% && BETA > 1.1",
            description = "Stress-tests high-beta US tech and NIKKEI exporter sensitivity against Yen carry shifts."
        ),
        ScenarioPreset(
            id = "ecb_dax",
            label = "ECB Rate Cut",
            expression = "ECB -25BPS && DAX_PE < 16",
            description = "Traces Eurozone easing spillover into undervalued DAX 40 software and industrial shares."
        ),
        ScenarioPreset(
            id = "vix_tech",
            label = "VIX Tech Stress",
            expression = "VIX > 22 && TECH_WT > 40%",
            description = "Evaluates volatility expansion impact on concentrated NASDAQ semiconductor exposure."
        ),
        ScenarioPreset(
            id = "bse_sip",
            label = "BSE Low-Vol SIP",
            expression = "INDIA_SIP_INFLOW && BETA < 1.0",
            description = "Filters steady compounding picks anchored by BSE SENSEX domestic liquidity."
        )
    )

    fun evaluateDiagnostics(
        tier: SubscriptionTier,
        riskProfile: RiskProfile,
        holdings: List<PortfolioHolding>,
        stocks: List<MarketStock>,
        indices: List<GlobalIndex>,
        alerts: List<PriceAlert>,
        rawExpression: String
    ): DiagnosticReportSnapshot {
        val expression = rawExpression.trim().ifBlank { "PORTFOLIO_AUDIT()" }
        val upperExpr = expression.uppercase(Locale.US)
        val stockMap = stocks.associateBy { it.symbol }
        val indexMap = indices.associateBy { it.code }

        // Compute portfolio metrics
        val totalValue = holdings.sumOf { h ->
            val live = stockMap[h.symbol]?.livePriceUsd ?: h.avgCostPrice
            h.shares * live
        }
        val weightedBeta = if (totalValue > 0.0) {
            holdings.sumOf { h ->
                val st = stockMap[h.symbol]
                val live = st?.livePriceUsd ?: h.avgCostPrice
                val weight = (h.shares * live) / totalValue
                weight * (st?.beta ?: 1.0)
            }
        } else {
            stocks.map { it.beta }.average()
        }

        val exchangeBreakdown = if (totalValue > 0.0) {
            holdings.groupBy { it.exchange }.mapValues { (_, list) ->
                val exVal = list.sumOf { h -> h.shares * (stockMap[h.symbol]?.livePriceUsd ?: h.avgCostPrice) }
                (exVal / totalValue) * 100.0
            }
        } else {
            emptyMap()
        }
        val topExchangeEntry = exchangeBreakdown.maxByOrNull { it.value }
        val topConcentrationPct = topExchangeEntry?.value ?: 0.0
        val topConcentrationLabel = if (topExchangeEntry != null) {
            "${topExchangeEntry.key} ${String.format(Locale.US, "%.1f%%", topConcentrationPct)}"
        } else {
            "Unallocated (0%)"
        }

        val spx = indexMap["SPX"]
        val dax = indexMap["DAX"]
        val ndx = indexMap["NDX"]
        val sensex = indexMap["SENSEX"]
        val peSpreadUsDax = (spx?.peRatio ?: 25.4) - (dax?.peRatio ?: 14.3)
        val ytdSpreadNdxDax = (ndx?.ytdReturnPct ?: 25.4) - (dax?.ytdReturnPct ?: 11.3)
        val volSpreadNdxSensex = (ndx?.annualizedVolPct ?: 18.6) - (sensex?.annualizedVolPct ?: 12.6)

        val watchedSymbols = alerts.map { it.symbol }.toSet()
        val unguardedHighBeta = holdings.filter { h ->
            val beta = stockMap[h.symbol]?.beta ?: 1.0
            beta >= 1.15 && h.symbol !in watchedSymbols
        }

        // Scenario rule matching against live catalog & holdings
        val betaThreshold = extractNumericComparison(upperExpr, "BETA")
        val peThreshold = extractNumericComparison(upperExpr, "PE")
        val hasJpyShift = upperExpr.contains("JPY") || upperExpr.contains("NIKKEI") || upperExpr.contains("N225")
        val hasEcbDax = upperExpr.contains("ECB") || upperExpr.contains("DAX")
        val hasVixTech = upperExpr.contains("VIX") || upperExpr.contains("TECH") || upperExpr.contains("NASDAQ")
        val hasIndiaSip = upperExpr.contains("INDIA") || upperExpr.contains("BSE") || upperExpr.contains("SIP")

        val matchedStocks = stocks.filter { st ->
            val betaHit = betaThreshold?.let { (op, value) ->
                if (op == "<") st.beta < value else st.beta > value
            } ?: false
            val peHit = peThreshold?.let { (op, value) ->
                if (op == "<") st.peRatio < value else st.peRatio > value
            } ?: false
            val regionHit = (hasJpyShift && st.exchange == "TSE") ||
                (hasEcbDax && st.exchange == "XETRA") ||
                (hasVixTech && st.exchange in listOf("NASDAQ", "NYSE")) ||
                (hasIndiaSip && st.exchange == "BSE")
            betaHit || peHit || regionHit
        }

        val betaLimit = when (riskProfile) {
            RiskProfile.CONSERVATIVE -> 1.05
            RiskProfile.MODERATE -> 1.25
            RiskProfile.GROWTH -> 1.55
        }
        val betaStatus = when {
            holdings.isEmpty() -> DiagnosticStatus.DRIFT
            weightedBeta > betaLimit + 0.15 -> DiagnosticStatus.ANOMALY
            weightedBeta > betaLimit -> DiagnosticStatus.DRIFT
            else -> DiagnosticStatus.PASS
        }
        val concentrationStatus = when {
            holdings.isEmpty() -> DiagnosticStatus.DRIFT
            topConcentrationPct >= 65.0 -> DiagnosticStatus.ANOMALY
            topConcentrationPct >= 45.0 -> DiagnosticStatus.DRIFT
            else -> DiagnosticStatus.PASS
        }
        val alertCoverageStatus = when {
            unguardedHighBeta.isNotEmpty() -> DiagnosticStatus.ANOMALY
            holdings.isNotEmpty() && alerts.isEmpty() -> DiagnosticStatus.DRIFT
            else -> DiagnosticStatus.PASS
        }
        val divergenceStatus = if (abs(ytdSpreadNdxDax) >= 12.0) DiagnosticStatus.DRIFT else DiagnosticStatus.PASS

        val metricsTable = listOf(
            DiagnosticMetricRow(
                metric = "Portfolio Weighted Beta",
                result = "${String.format(Locale.US, "%.2f", weightedBeta)} (cap ${String.format(Locale.US, "%.2f", betaLimit)})",
                status = betaStatus
            ),
            DiagnosticMetricRow(
                metric = "Top Exchange Weight",
                result = topConcentrationLabel,
                status = concentrationStatus
            ),
            DiagnosticMetricRow(
                metric = "US–DAX P/E Valuation Spread",
                result = "+${String.format(Locale.US, "%.1fx", peSpreadUsDax)} (${String.format(Locale.US, "%.1f%%", ytdSpreadNdxDax)} YTD gap)",
                status = divergenceStatus
            ),
            DiagnosticMetricRow(
                metric = "Unguarded High-Beta Assets",
                result = "${unguardedHighBeta.size} positions without alerts",
                status = alertCoverageStatus
            ),
            DiagnosticMetricRow(
                metric = "Scenario Hits / Tier Model",
                result = "${matchedStocks.size} matched • ${tier.aiModelId}",
                status = if (matchedStocks.isNotEmpty()) DiagnosticStatus.PASS else DiagnosticStatus.DRIFT
            )
        )

        val traceSteps = buildList {
            add(
                ExecutionTraceStep(
                    stepId = "STEP 01",
                    traceOrigin = "context:///tier/${tier.id}",
                    detail = "Routed advisory context to ${tier.displayName} (${tier.aiModelId}) under ${riskProfile.label} risk guardrails.",
                    status = DiagnosticStatus.PASS
                )
            )
            add(
                ExecutionTraceStep(
                    stepId = "STEP 02",
                    traceOrigin = "portfolio:///holdings/${holdings.size}",
                    detail = if (holdings.isEmpty()) {
                        "No live portfolio holdings detected; evaluating against 10-asset global benchmark universe."
                    } else {
                        "Evaluated ${holdings.size} live positions ($${String.format(Locale.US, "%,.0f", totalValue)}); top exposure $topConcentrationLabel."
                    },
                    status = concentrationStatus
                )
            )
            add(
                ExecutionTraceStep(
                    stepId = "STEP 03",
                    traceOrigin = "macro:///divergence/us-jp-eu-in",
                    detail = "NDX vs SENSEX vol spread at ${String.format(Locale.US, "+%.1f%%", volSpreadNdxSensex)}; SPX–DAX P/E spread +${String.format(Locale.US, "%.1fx", peSpreadUsDax)}.",
                    status = divergenceStatus
                )
            )
            add(
                ExecutionTraceStep(
                    stepId = "STEP 04",
                    traceOrigin = "script:///${expression.take(28).replace(" ", "_")}",
                    detail = if (matchedStocks.isNotEmpty()) {
                        "Matched ${matchedStocks.joinToString(", ") { it.symbol }} under rule `$expression`."
                    } else {
                        "Evaluated rule `$expression` across ${stocks.size} global equities with 0 hard rule breaches."
                    },
                    status = if (unguardedHighBeta.isNotEmpty()) DiagnosticStatus.ANOMALY else DiagnosticStatus.PASS
                )
            )
        }

        val diagnosticsBullets = buildList {
            add(
                if (holdings.isEmpty()) {
                    "Portfolio State: Empty portfolio detected—load starter global holdings or inspect benchmark picks to unlock live beta weighting."
                } else {
                    "Exposure Analysis: Weighted portfolio beta is ${String.format(Locale.US, "%.2f", weightedBeta)} against your ${riskProfile.label} ceiling (${String.format(Locale.US, "%.2f", betaLimit)}); top concentration sits in $topConcentrationLabel."
                }
            )
            add(
                "Cross-Exchange Divergence: US NASDAQ 100 leads DAX 40 by ${String.format(Locale.US, "%.1f%%", ytdSpreadNdxDax)} YTD while DAX trades at a ${String.format(Locale.US, "%.1fx", peSpreadUsDax)} P/E discount and BSE SENSEX holds the lowest volatility (${String.format(Locale.US, "%.1f%%", sensex?.annualizedVolPct ?: 12.6)})."
            )
            add(
                if (matchedStocks.isNotEmpty()) {
                    "Rule Signal Trace: Script `$expression` isolated ${matchedStocks.joinToString(", ") { "${it.symbol} (${it.exchange})" }} as primary sensitivity candidates."
                } else {
                    "Rule Signal Trace: Expression `$expression` scanned ${stocks.size} equities across NASDAQ, NYSE, TSE, XETRA, and BSE."
                }
            )
        }

        val actionableFindings = buildList {
            val topUnguarded = unguardedHighBeta.firstOrNull()
            if (topUnguarded != null) {
                add(
                    DiagnosticFinding(
                        id = "finding_hedge_${topUnguarded.symbol}",
                        title = "Unhedged High-Beta Position: ${topUnguarded.symbol}",
                        category = "Volatility Guardrail",
                        description = "${topUnguarded.symbol} (${topUnguarded.exchange}) has elevated beta without an active price-change alert.",
                        actionLabel = "Create 2.5% Hedge Alert",
                        actionType = DiagnosticActionType.CREATE_HEDGE_ALERT,
                        targetSymbol = topUnguarded.symbol,
                        targetExchange = topUnguarded.exchange,
                        recommendedPct = 2.5
                    )
                )
            } else if (holdings.isNotEmpty() && alerts.isEmpty()) {
                val firstHolding = holdings.first()
                add(
                    DiagnosticFinding(
                        id = "finding_first_alert_${firstHolding.symbol}",
                        title = "Enable Price Volatility Trigger on ${firstHolding.symbol}",
                        category = "Alert Coverage",
                        description = "Active positions currently have 0 volatility triggers armed for automated price-move alerts.",
                        actionLabel = "Arm 2.5% Price Alert",
                        actionType = DiagnosticActionType.CREATE_HEDGE_ALERT,
                        targetSymbol = firstHolding.symbol,
                        targetExchange = firstHolding.exchange,
                        recommendedPct = 2.5
                    )
                )
            }

            if (topConcentrationPct >= 45.0 || holdings.isEmpty()) {
                add(
                    DiagnosticFinding(
                        id = "finding_divergence_rotate",
                        title = "Cross-Border Diversification Gap (${String.format(Locale.US, "%.1fx", peSpreadUsDax)} P/E Spread)",
                        category = "Global Divergence",
                        description = if (holdings.isEmpty()) {
                            "Compare US momentum against NIKKEI 225, DAX 40 value discounts, and low-vol BSE SENSEX before allocating."
                        } else {
                            "$topConcentrationLabel concentration leaves your portfolio sensitive to single-region shocks. Inspect NIKKEI, DAX, and BSE divergence."
                        },
                        actionLabel = "Open Divergence Lab",
                        actionType = DiagnosticActionType.EXPLORE_DIVERGENT_COMPARE
                    )
                )
            }

            val topCandidate = matchedStocks.firstOrNull() ?: stocks.first()
            add(
                DiagnosticFinding(
                    id = "finding_ai_catalyst_${topCandidate.symbol}",
                    title = "AI Catalyst Verification for ${topCandidate.symbol}",
                    category = "AI Share Picking",
                    description = "Generate fresh international news-to-share conviction tailored to your ${tier.displayName} tier (${tier.aiModelId}).",
                    actionLabel = "Launch AI News Engine",
                    actionType = DiagnosticActionType.OPEN_AI_PICKS,
                    targetSymbol = topCandidate.symbol,
                    targetExchange = topCandidate.exchange
                )
            )
        }

        val anomalyCount = metricsTable.count { it.status == DiagnosticStatus.ANOMALY } +
            metricsTable.count { it.status == DiagnosticStatus.DRIFT }

        val contextSummary = buildString {
            append("Execution audit under ${tier.displayName} (${tier.aiModelId}) • ")
            append("${riskProfile.label} profile • ")
            if (holdings.isEmpty()) {
                append("0 active holdings (benchmark mode)")
            } else {
                append("${holdings.size} holdings ($${String.format(Locale.US, "%,.2f", totalValue)})")
            }
        }

        return DiagnosticReportSnapshot(
            scenarioExpression = expression,
            contextSummary = contextSummary,
            diagnosticsBullets = diagnosticsBullets,
            traceSteps = traceSteps,
            metricsTable = metricsTable,
            actionableFindings = actionableFindings,
            weightedBeta = weightedBeta,
            topConcentrationLabel = topConcentrationLabel,
            anomalyCount = anomalyCount
        )
    }

    private fun extractNumericComparison(upperExpr: String, keyword: String): Pair<String, Double>? {
        val regex = Regex("""$keyword(?:_PE)?\s*([<>])\s*([0-9]+(?:\.[0-9]+)?)""")
        val match = regex.find(upperExpr) ?: return null
        val op = match.groupValues[1]
        val value = match.groupValues[2].toDoubleOrNull() ?: return null
        return op to value
    }
}
