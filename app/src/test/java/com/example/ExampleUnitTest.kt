package com.example

import com.example.data.diagnostics.DiagnosticActionType
import com.example.data.diagnostics.DiagnosticStatus
import com.example.data.diagnostics.DiagnosticsEngine
import com.example.data.market.GlobalMarketCatalog
import com.example.data.model.PortfolioHolding
import com.example.data.model.PriceAlert
import com.example.data.model.RiskProfile
import com.example.data.model.SubscriptionTier
import com.example.data.model.TimeRange
import com.example.data.watchlist.DivergenceLevel
import com.example.data.watchlist.WatchlistCorrelationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun subscriptionTiers_useDistinctAiModels() {
        assertNotEquals(SubscriptionTier.STUDENT.aiModelId, SubscriptionTier.ADULT.aiModelId)
        assertEquals("gemini-3.5-flash-lite", SubscriptionTier.STUDENT.aiModelId)
        assertEquals("gemini-3.8-flash", SubscriptionTier.ADULT.aiModelId)
    }

    @Test
    fun globalMarketCatalog_containsMajorGlobalIndicesAndStocks() {
        val codes = GlobalMarketCatalog.initialIndices.map { it.code }
        assertTrue(codes.containsAll(listOf("SPX", "NDX", "N225", "DAX", "SENSEX", "FTSE")))
        GlobalMarketCatalog.initialIndices.forEach { idx ->
            TimeRange.entries.forEach { range ->
                val pts = idx.historicalSeries[range]
                assertTrue("Expected points for ${idx.code} ${range.label}", !pts.isNullOrEmpty())
            }
        }
        assertTrue(GlobalMarketCatalog.initialStocks.size >= 8)
    }

    @Test
    fun diagnosticsEngine_flagsUnguardedHighBetaAndConcentration() {
        val nvda = GlobalMarketCatalog.initialStocks.first { it.symbol == "NVDA" }
        val holdings = listOf(
            PortfolioHolding(
                id = "h1",
                symbol = nvda.symbol,
                companyName = nvda.companyName,
                exchange = nvda.exchange,
                shares = 10.0,
                avgCostPrice = 120.0
            )
        )

        val snapshot = DiagnosticsEngine.evaluateDiagnostics(
            tier = SubscriptionTier.STUDENT,
            riskProfile = RiskProfile.CONSERVATIVE,
            holdings = holdings,
            stocks = GlobalMarketCatalog.initialStocks,
            indices = GlobalMarketCatalog.initialIndices,
            alerts = emptyList(),
            rawExpression = "VIX > 22 && BETA > 1.2"
        )

        assertTrue(snapshot.weightedBeta > 1.4)
        assertEquals("NASDAQ 100.0%", snapshot.topConcentrationLabel)
        assertTrue(snapshot.metricsTable.any { it.status == DiagnosticStatus.ANOMALY })
        assertTrue(
            snapshot.actionableFindings.any {
                it.actionType == DiagnosticActionType.CREATE_HEDGE_ALERT && it.targetSymbol == "NVDA"
            }
        )
        assertTrue(snapshot.traceSteps.size == 4)
    }

    @Test
    fun diagnosticsEngine_clearsHedgeAlertFlagWhenAlertPresent() {
        val nvda = GlobalMarketCatalog.initialStocks.first { it.symbol == "NVDA" }
        val holdings = listOf(
            PortfolioHolding(
                id = "h1",
                symbol = nvda.symbol,
                companyName = nvda.companyName,
                exchange = nvda.exchange,
                shares = 5.0,
                avgCostPrice = 135.0
            )
        )
        val alerts = listOf(
            PriceAlert(
                id = "a1",
                symbol = "NVDA",
                exchange = "NASDAQ",
                targetChangePct = 2.5
            )
        )

        val snapshot = DiagnosticsEngine.evaluateDiagnostics(
            tier = SubscriptionTier.ADULT,
            riskProfile = RiskProfile.GROWTH,
            holdings = holdings,
            stocks = GlobalMarketCatalog.initialStocks,
            indices = GlobalMarketCatalog.initialIndices,
            alerts = alerts,
            rawExpression = "ECB -25BPS && DAX_PE < 16"
        )

        val unguardedRow = snapshot.metricsTable.first { it.metric == "Unguarded High-Beta Assets" }
        assertEquals(DiagnosticStatus.PASS, unguardedRow.status)
        assertTrue(snapshot.diagnosticsBullets.any { it.contains("SAP.DE") || it.contains("SIE.DE") || it.contains("7203.T") })
    }

    @Test
    fun watchlistCorrelationEngine_computesBoundedPearsonAndDetectsLiveDivergence() {
        val positiveR = WatchlistCorrelationEngine.pearsonCorrelation(
            listOf(1.0, 2.0, 3.0, 4.0),
            listOf(2.0, 4.0, 6.0, 8.0)
        )
        assertEquals(1.0, positiveR, 0.001)

        val negativeR = WatchlistCorrelationEngine.pearsonCorrelation(
            listOf(1.0, 2.0, 3.0, 4.0),
            listOf(8.0, 6.0, 4.0, 2.0)
        )
        assertEquals(-1.0, negativeR, 0.001)

        val nvda = GlobalMarketCatalog.initialStocks.first { it.symbol == "NVDA" }
        val telemetry = WatchlistCorrelationEngine.computeCorrelationTelemetry(
            stock = nvda,
            indices = GlobalMarketCatalog.initialIndices
        )
        assertTrue(telemetry.nikkeiCorr in -1.0..1.0)
        assertTrue(telemetry.daxCorr in -1.0..1.0)
        assertTrue(telemetry.bseCorr in -1.0..1.0)
        assertTrue(telemetry.isRealTimeDiverging)
    }

    @Test
    fun watchlistCorrelationEngine_detectsShortVsLongDriftAndFormatsTierReasons() {
        val benchmark = (0 until 120).map { i -> if (i % 2 == 0) 0.01 else -0.01 }
        // First 100 sessions aligned (+1.0), last 20 sessions inverted (-1.0) -> drift ~ +2.0 -> DIVERGING
        val divergingStock = (0 until 100).map { i -> if (i % 2 == 0) 0.01 else -0.01 } +
            (0 until 20).map { i -> if (i % 2 == 0) -0.01 else 0.01 }

        val metrics = WatchlistCorrelationEngine.analyseCorrelationDrift(
            ticker = "SAP.DE",
            benchmarkName = "DAX 40",
            stockReturns = divergingStock,
            benchmarkReturns = benchmark,
            shortWindow = 20,
            longWindow = 120
        )

        assertEquals(DivergenceLevel.DIVERGING, metrics.level)
        assertTrue(metrics.drift > 0.45)
        assertTrue(metrics.plainReason.contains("SAP.DE"))
        assertTrue(metrics.plainReason.contains("DAX 40"))
        assertTrue(metrics.quantReason.contains("Δρ="))
    }
}
