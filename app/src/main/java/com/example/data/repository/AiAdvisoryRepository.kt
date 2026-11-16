package com.example.data.repository

import android.util.Log
import com.example.data.diagnostics.AiDiagnosticVerdict
import com.example.data.diagnostics.DiagnosticReportSnapshot
import com.example.data.model.AiGeneratedNewsPick
import com.example.data.model.GlobalIndex
import com.example.data.model.MarketStock
import com.example.data.model.RiskProfile
import com.example.data.model.SubscriptionTier
import com.example.data.model.WatchlistItem
import com.example.data.watchlist.CorrelationTelemetry
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiAdvisoryRepository {

    private val newsPickSchema = Schema.obj(
        properties = mapOf(
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "headline" to Schema.string(description = "Concise international macroeconomic or market news catalyst headline"),
                        "region" to Schema.string(description = "Primary market region: US, APAC, EUROPE, INDIA, or GLOBAL"),
                        "macroImpact" to Schema.string(description = "How this international event creates divergence across US, NIKKEI, DAX, or BSE"),
                        "recommendedTicker" to Schema.string(description = "Specific stock ticker from NASDAQ, NYSE, TSE, XETRA, or BSE"),
                        "actionSignal" to Schema.enumeration(
                            values = listOf("BUY", "ACCUMULATE", "HOLD", "TRIM"),
                            description = "Actionable investment signal"
                        ),
                        "confidenceScore" to Schema.integer(description = "Conviction score from 50 to 98"),
                        "rationale" to Schema.string(description = "Clear explanation tailored to the subscriber tier"),
                        "divergenceEffect" to Schema.string(description = "Relative impact comparing US vs NIKKEI, DAX, or BSE")
                    )
                ),
                description = "List of 3 high-signal international news catalysts mapped to equity picks"
            )
        )
    )

    private val stockDeepDiveSchema = Schema.obj(
        properties = mapOf(
            "actionSignal" to Schema.enumeration(values = listOf("BUY", "ACCUMULATE", "HOLD", "TRIM")),
            "confidenceScore" to Schema.integer(description = "Conviction score 0-100"),
            "thesis" to Schema.string(description = "Tier-tailored share picking thesis"),
            "globalCatalyst" to Schema.string(description = "International news / macro tailwind or headwind"),
            "riskGuardrail" to Schema.string(description = "Key volatility or position-sizing safeguard")
        )
    )

    private val studentNewsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SubscriptionTier.STUDENT.aiModelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = newsPickSchema
            }
        )
    }

    private val adultNewsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SubscriptionTier.ADULT.aiModelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = newsPickSchema
            }
        )
    }

    private val studentStockModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SubscriptionTier.STUDENT.aiModelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = stockDeepDiveSchema
            }
        )
    }

    private val adultStockModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SubscriptionTier.ADULT.aiModelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = stockDeepDiveSchema
            }
        )
    }

    private val diagnosticAuditSchema = Schema.obj(
        properties = mapOf(
            "contextAnalysis" to Schema.string(description = "Structured summary of portfolio & macro regime context"),
            "catalystOrigin" to Schema.string(description = "Primary international market driver or divergence source causing risk or opportunity"),
            "executionRiskSummary" to Schema.string(description = "Assessment of beta, concentration, and alert gaps"),
            "remediationSteps" to Schema.array(
                items = Schema.string(description = "Actionable step to hedge, diversify, or optimize share selection"),
                description = "Exactly 3 concrete, practical remediation actions"
            ),
            "recommendedShareToInspect" to Schema.string(description = "Ticker symbol from US, NIKKEI, DAX, or BSE best suited as next review target")
        )
    )

    private val studentDiagnosticModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SubscriptionTier.STUDENT.aiModelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = diagnosticAuditSchema
            }
        )
    }

    private val adultDiagnosticModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SubscriptionTier.ADULT.aiModelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = diagnosticAuditSchema
            }
        )
    }

    private val watchlistDivergenceSchema = Schema.obj(
        properties = mapOf(
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "symbol" to Schema.string(description = "Stock ticker symbol matching one of the watched items"),
                        "divergenceTag" to Schema.enumeration(
                            values = listOf("DIVERGENCE_ALERT", "BULLISH_DECOUPLING", "CORRELATION_BREAK", "ALIGNED"),
                            description = "Use DIVERGENCE_ALERT, BULLISH_DECOUPLING, or CORRELATION_BREAK when historical NIKKEI/DAX/BSE correlations diverge; ALIGNED otherwise"
                        ),
                        "divergenceAlertActive" to Schema.boolean(description = "True when a potential Divergence alert is detected"),
                        "confidenceScore" to Schema.integer(description = "Conviction score from 55 to 98"),
                        "correlatedAnchor" to Schema.string(description = "Primary regional index driver e.g. 'NIKKEI 225, DAX 40, BSE SENSEX'"),
                        "rationale" to Schema.string(description = "Concise explanation referencing historical NIKKEI, DAX, and BSE correlations")
                    )
                ),
                description = "Per-stock AI divergence alert tags for the user's watchlist"
            )
        )
    )

    private val studentWatchlistModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SubscriptionTier.STUDENT.aiModelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = watchlistDivergenceSchema
            }
        )
    }

    private val adultWatchlistModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SubscriptionTier.ADULT.aiModelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = watchlistDivergenceSchema
            }
        )
    }

    private val volatilityChatModel by lazy {
        Firebase.ai.generativeModel(modelName = SubscriptionTier.ADULT.aiModelId)
    }

    suspend fun generateInternationalNewsPicks(
        tier: SubscriptionTier,
        focusRegion: String,
        riskProfile: RiskProfile,
        holdingsSummary: String
    ): List<AiGeneratedNewsPick> {
        val model = if (tier == SubscriptionTier.STUDENT) studentNewsModel else adultNewsModel
        val tierPersona = if (tier == SubscriptionTier.STUDENT) {
            "You are FinAdvisor's Student Scholar AI Model (educational, plain-English, capital-preservation first, SIP/DCA friendly, clear risk guardrails)."
        } else {
            "You are FinAdvisor's Adult Quant Pro AI Model (institutional macro strategist, cross-border FX/rate divergence, factor alpha & tactical allocation)."
        }

        val prompt = """
            $tierPersona
            Generate 3 realistic, timely international macroeconomic & geopolitical news catalysts focusing on $focusRegion and global spillovers across US (S&P 500/NASDAQ), Japan (NIKKEI 225), Germany (DAX 40), and India (BSE SENSEX).
            Subscriber Risk Profile: ${riskProfile.label}.
            Current Portfolio Context: $holdingsSummary.
            Prefer actionable tickers from: NVDA, MSFT, AAPL, TSM, 7203.T, 6758.T, SAP.DE, SIE.DE, RELIANCE.BO, TCS.BO.
            Explain clearly which share to pick and why based on global exchange divergence.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("AI model returned an empty news analysis.")
            val root = JSONObject(rawJson)
            val arr = root.getJSONArray("items")
            return (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                AiGeneratedNewsPick(
                    headline = obj.getString("headline"),
                    region = obj.getString("region"),
                    macroImpact = obj.getString("macroImpact"),
                    recommendedTicker = obj.getString("recommendedTicker"),
                    actionSignal = obj.getString("actionSignal"),
                    confidenceScore = obj.optInt("confidenceScore", 80).coerceIn(0, 100),
                    rationale = obj.getString("rationale"),
                    divergenceEffect = obj.getString("divergenceEffect")
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "News pick generation timed out", e)
            throw IllegalStateException("AI news synthesis timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "News pick generation failed", e)
            throw e
        }
    }

    suspend fun analyzeStockForTier(
        stock: MarketStock,
        tier: SubscriptionTier,
        indices: List<GlobalIndex>
    ): StockAiAnalysis {
        val model = if (tier == SubscriptionTier.STUDENT) studentStockModel else adultStockModel
        val indexSnapshot = indices.joinToString("; ") {
            "${it.name}: ${String.format("%.1f", it.ytdReturnPct)}% YTD (P/E ${it.peRatio}x, Vol ${it.annualizedVolPct}%)"
        }
        val tierInstructions = if (tier == SubscriptionTier.STUDENT) {
            "Explain in approachable, educational terms for a student investor with a small budget."
        } else {
            "Deliver a concise institutional quant thesis referencing valuation spread, FX/macro sensitivity, and cross-index divergence."
        }

        val prompt = """
            Analyze ${stock.symbol} (${stock.companyName}) on ${stock.exchange} (${stock.sector}).
            Live Price: $${stock.livePriceUsd}, Daily Change: ${stock.dailyChangePct}%, P/E: ${stock.peRatio}x, Beta: ${stock.beta}, Div Yield: ${stock.dividendYieldPct}%.
            Global Market Backdrop: $indexSnapshot.
            $tierInstructions
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty AI stock analysis.")
            val json = JSONObject(rawJson)
            return StockAiAnalysis(
                symbol = stock.symbol,
                tier = tier,
                actionSignal = json.getString("actionSignal"),
                confidenceScore = json.optInt("confidenceScore", 82).coerceIn(0, 100),
                thesis = json.getString("thesis"),
                globalCatalyst = json.getString("globalCatalyst"),
                riskGuardrail = json.getString("riskGuardrail")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Stock analysis timed out", e)
            throw IllegalStateException("Stock analysis timed out after 60s.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Stock analysis failed", e)
            throw e
        }
    }

    suspend fun generateDivergenceForecast(
        indices: List<GlobalIndex>,
        tier: SubscriptionTier
    ): String {
        val indexLines = indices.joinToString("\n") { idx ->
            "- ${idx.name} (${idx.country}): Level ${idx.currentLevel}, Daily ${idx.dailyChangePct}%, YTD ${idx.ytdReturnPct}%, Vol ${idx.annualizedVolPct}%, P/E ${idx.peRatio}x"
        }
        val prompt = """
            As FinAdvisor's ${tier.displayName} AI strategist (${tier.aiEngineLabel}), provide a sharp 3-bullet comparative divergence & volatility forecast across US (S&P 500/NASDAQ), Japan (NIKKEI 225), Germany (DAX 40), and India (BSE SENSEX) based on these live readings:
            $indexLines
            Include 1 concrete cross-border share-picking allocation suggestion. Keep it under 140 words.
        """.trimIndent()

        try {
            val response = withTimeout(45_000L) {
                volatilityChatModel.generateContent(prompt)
            }
            return response.text?.trim()?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Model returned an empty forecast.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Divergence forecast timed out", e)
            throw IllegalStateException("Divergence forecast timed out.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Divergence forecast failed", e)
            throw e
        }
    }

    suspend fun runDiagnosticAudit(
        tier: SubscriptionTier,
        riskProfile: RiskProfile,
        snapshot: DiagnosticReportSnapshot,
        indices: List<GlobalIndex>
    ): AiDiagnosticVerdict {
        val model = if (tier == SubscriptionTier.STUDENT) studentDiagnosticModel else adultDiagnosticModel
        val metricsSummary = snapshot.metricsTable.joinToString("; ") {
            "${it.metric}=${it.result} [${it.status.label}]"
        }
        val indexSummary = indices.joinToString("; ") {
            "${it.name} ${String.format("%.1f", it.ytdReturnPct)}% YTD (P/E ${it.peRatio}x)"
        }
        val persona = if (tier == SubscriptionTier.STUDENT) {
            "Use clear, educational guidance suitable for a student investor prioritizing risk guardrails and steady SIP diversification."
        } else {
            "Use sharp institutional quant commentary addressing factor beta, cross-border FX/valuation divergence, and tactical hedging."
        }

        val prompt = """
            Perform a structured Quant Strategy & Portfolio Diagnostic Audit for FinAdvisor.
            $persona
            Execution Context: ${snapshot.contextSummary}
            Scenario Expression: ${snapshot.scenarioExpression}
            Risk Profile: ${riskProfile.label}
            Metric Table Telemetry: $metricsSummary
            Global Index Backdrop: $indexSummary
            Deliver structured findings and 3 remediation steps referencing US, NIKKEI, DAX, or BSE shares.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("AI diagnostic audit returned empty output.")
            val json = JSONObject(rawJson)
            val stepsArr = json.optJSONArray("remediationSteps")
            val steps = if (stepsArr != null) {
                (0 until stepsArr.length()).mapNotNull { i ->
                    stepsArr.optString(i).takeIf { it.isNotBlank() }
                }
            } else {
                emptyList()
            }
            return AiDiagnosticVerdict(
                contextAnalysis = json.getString("contextAnalysis"),
                catalystOrigin = json.getString("catalystOrigin"),
                executionRiskSummary = json.getString("executionRiskSummary"),
                remediationSteps = steps,
                recommendedShareToInspect = json.optString("recommendedShareToInspect", "MSFT")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Diagnostic audit timed out", e)
            throw IllegalStateException("AI diagnostic audit timed out after 60s.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Diagnostic audit failed", e)
            throw e
        }
    }

    suspend fun analyzeWatchlistDivergenceAlerts(
        tier: SubscriptionTier,
        watchlist: List<WatchlistItem>,
        stocks: List<MarketStock>,
        indices: List<GlobalIndex>,
        telemetryMap: Map<String, CorrelationTelemetry>
    ): List<WatchlistDivergenceAiTag> {
        if (watchlist.isEmpty()) return emptyList()
        val model = if (tier == SubscriptionTier.STUDENT) studentWatchlistModel else adultWatchlistModel
        val stockMap = stocks.associateBy { it.symbol }

        val benchmarkContext = indices.filter { it.code in setOf("N225", "DAX", "SENSEX") }
            .joinToString("; ") { idx ->
                "${idx.name} (${idx.code}): Level ${String.format("%.1f", idx.currentLevel)}, Daily ${String.format("%.2f", idx.dailyChangePct)}%, YTD ${String.format("%.1f", idx.ytdReturnPct)}%, Vol ${idx.annualizedVolPct}%"
            }

        val watchedLines = watchlist.joinToString("\n") { item ->
            val st = stockMap[item.symbol]
            val tel = telemetryMap[item.symbol]
            val priceStr = st?.let { "$${String.format("%.2f", it.livePriceUsd)} (${String.format("%.2f", it.dailyChangePct)}% today)" } ?: "N/A"
            val corrStr = tel?.let {
                "Corr(N225=${String.format("%.2f", it.nikkeiCorr)}, DAX=${String.format("%.2f", it.daxCorr)}, BSE=${String.format("%.2f", it.bseCorr)}), LiveSpread=${String.format("%.2f", it.liveSpreadDeltaPct)}% vs ${it.dominantDivergenceBenchmark}"
            } ?: "No live telemetry"
            "- ${item.symbol} (${item.exchange}, ${st?.sector ?: "Equities"}): $priceStr | $corrStr"
        }

        val tierStyle = if (tier == SubscriptionTier.STUDENT) {
            "Explain divergence alerts in clear, beginner-friendly language highlighting diversification opportunities."
        } else {
            "Provide crisp institutional correlation-break diagnostics referencing NIKKEI, DAX, and BSE historical divergence."
        }

        val prompt = """
            Analyze this real-time FinAdvisor Smart Watchlist and tag each stock with potential 'Divergence' alerts based on historical NIKKEI 225, DAX 40, and BSE SENSEX correlations.
            $tierStyle
            Regional Benchmarks: $benchmarkContext
            Watched Stocks Telemetry:
            $watchedLines
            Return one item per watched symbol with accurate divergenceTag, divergenceAlertActive, confidenceScore, correlatedAnchor, and rationale.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI watchlist scan returned empty output.")
            val root = JSONObject(rawJson)
            val arr = root.getJSONArray("items")
            return (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                WatchlistDivergenceAiTag(
                    symbol = obj.getString("symbol").trim().uppercase(),
                    divergenceTag = obj.getString("divergenceTag"),
                    divergenceAlertActive = obj.optBoolean("divergenceAlertActive", true),
                    confidenceScore = obj.optInt("confidenceScore", 78).coerceIn(0, 100),
                    correlatedAnchor = obj.optString("correlatedAnchor", "NIKKEI 225, DAX 40, BSE SENSEX"),
                    rationale = obj.getString("rationale")
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Watchlist divergence scan timed out", e)
            throw IllegalStateException("AI watchlist divergence scan timed out after 60s.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Watchlist divergence scan failed", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "AiAdvisoryRepo"
    }
}

data class WatchlistDivergenceAiTag(
    val symbol: String,
    val divergenceTag: String,
    val divergenceAlertActive: Boolean,
    val confidenceScore: Int,
    val correlatedAnchor: String,
    val rationale: String
)

data class StockAiAnalysis(
    val symbol: String,
    val tier: SubscriptionTier,
    val actionSignal: String,
    val confidenceScore: Int,
    val thesis: String,
    val globalCatalyst: String,
    val riskGuardrail: String
)
