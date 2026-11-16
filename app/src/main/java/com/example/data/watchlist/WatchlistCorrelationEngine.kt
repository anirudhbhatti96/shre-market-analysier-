package com.example.data.watchlist

import com.example.data.model.GlobalIndex
import com.example.data.model.MarketRegion
import com.example.data.model.MarketStock
import com.example.data.model.TimeRange
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

enum class DivergenceLevel { ALIGNED, WATCH, DIVERGING }

data class CorrelationTelemetry(
    val symbol: String,
    val benchmarkName: String,
    val shortCorr: Double,
    val longCorr: Double,
    val drift: Double,
    val level: DivergenceLevel,
    val plainReason: String,
    val quantReason: String,
    val nikkeiCorr: Double,
    val daxCorr: Double,
    val bseCorr: Double,
    val liveSpreadDeltaPct: Double,
    val dominantDivergenceBenchmark: String,
    val isRealTimeDiverging: Boolean
)

object WatchlistCorrelationEngine {

    fun computeCorrelationTelemetry(
        stock: MarketStock,
        indices: List<GlobalIndex>
    ): CorrelationTelemetry {
        val indexMap = indices.associateBy { it.code }
        val nikkei = indexMap["N225"]
        val dax = indexMap["DAX"]
        val bse = indexMap["SENSEX"]

        val stockShortReturns = computeSeriesReturns(stock.sparkline)
        val nikkeiCorr = pearsonCorrelation(
            stockShortReturns,
            alignIndexReturns(nikkei, TimeRange.ONE_MONTH, stockShortReturns.size)
        )
        val daxCorr = pearsonCorrelation(
            stockShortReturns,
            alignIndexReturns(dax, TimeRange.ONE_MONTH, stockShortReturns.size)
        )
        val bseCorr = pearsonCorrelation(
            stockShortReturns,
            alignIndexReturns(bse, TimeRange.ONE_MONTH, stockShortReturns.size)
        )

        // Primary regional benchmark for short vs. long correlation drift
        val primaryBenchmark = when (stock.region) {
            MarketRegion.APAC -> nikkei
            MarketRegion.EUROPE -> dax
            MarketRegion.INDIA -> bse
            MarketRegion.US, MarketRegion.GLOBAL -> indexMap["SPX"] ?: nikkei ?: dax ?: bse
        }
        val benchmarkName = primaryBenchmark?.name ?: "Global Benchmark"
        val benchmarkLongReturns = alignIndexReturns(primaryBenchmark, TimeRange.FIVE_YEAR, 60)
        val stockLongReturns = synthesizeStockHistoryAlignedWithLiveTail(
            stock = stock,
            benchmarkReturns = benchmarkLongReturns,
            liveTailReturns = stockShortReturns
        )

        val driftMetrics = analyseCorrelationDrift(
            ticker = stock.symbol,
            benchmarkName = benchmarkName,
            stockReturns = stockLongReturns,
            benchmarkReturns = benchmarkLongReturns,
            shortWindow = 12,
            longWindow = 59
        )

        val nikkeiMove = nikkei?.dailyChangePct ?: 0.0
        val daxMove = dax?.dailyChangePct ?: 0.0
        val bseMove = bse?.dailyChangePct ?: 0.0

        val deltas = listOf(
            "NIKKEI 225" to (stock.dailyChangePct - nikkeiMove),
            "DAX 40" to (stock.dailyChangePct - daxMove),
            "BSE SENSEX" to (stock.dailyChangePct - bseMove)
        )
        val maxDeltaPair = deltas.maxByOrNull { abs(it.second) } ?: ("NIKKEI 225" to 0.0)

        val realTimeDiverging = driftMetrics.level != DivergenceLevel.ALIGNED ||
            abs(maxDeltaPair.second) >= 1.35 ||
            abs(nikkeiCorr) < 0.35 ||
            daxCorr < 0.0 ||
            abs(bseCorr) < 0.30

        return CorrelationTelemetry(
            symbol = stock.symbol,
            benchmarkName = driftMetrics.benchmarkName,
            shortCorr = driftMetrics.shortCorr,
            longCorr = driftMetrics.longCorr,
            drift = driftMetrics.drift,
            level = driftMetrics.level,
            plainReason = driftMetrics.plainReason,
            quantReason = driftMetrics.quantReason,
            nikkeiCorr = nikkeiCorr,
            daxCorr = daxCorr,
            bseCorr = bseCorr,
            liveSpreadDeltaPct = maxDeltaPair.second,
            dominantDivergenceBenchmark = maxDeltaPair.first,
            isRealTimeDiverging = realTimeDiverging
        )
    }

    data class DriftMetrics(
        val benchmarkName: String,
        val shortCorr: Double,
        val longCorr: Double,
        val drift: Double,
        val level: DivergenceLevel,
        val plainReason: String,
        val quantReason: String
    )

    /** Compare how a stock moved with its benchmark index recently vs. historically. */
    fun analyseCorrelationDrift(
        ticker: String,
        benchmarkName: String,
        stockReturns: List<Double>,
        benchmarkReturns: List<Double>,
        shortWindow: Int = 20,
        longWindow: Int = 120
    ): DriftMetrics {
        val longC = correlation(stockReturns.takeLast(longWindow), benchmarkReturns.takeLast(longWindow))
        val shortC = correlation(stockReturns.takeLast(shortWindow), benchmarkReturns.takeLast(shortWindow))
        val drift = longC - shortC
        val level = when {
            drift > 0.45 || (shortC < 0.0 && longC > 0.4) -> DivergenceLevel.DIVERGING
            drift > 0.25 -> DivergenceLevel.WATCH
            else -> DivergenceLevel.ALIGNED
        }
        val plain = when (level) {
            DivergenceLevel.ALIGNED -> "$ticker is moving with $benchmarkName as usual."
            DivergenceLevel.WATCH -> "$ticker has started to move differently from $benchmarkName. Keep an eye on it."
            DivergenceLevel.DIVERGING -> "$ticker is now moving very differently from $benchmarkName. Find out why before you invest."
        }
        val quant = String.format(
            Locale.US,
            "ρ(%dd)=%.2f vs ρ(%dd)=%.2f, Δρ=%.2f vs %s",
            minOf(shortWindow, stockReturns.size, benchmarkReturns.size),
            shortC,
            minOf(longWindow, stockReturns.size, benchmarkReturns.size),
            longC,
            drift,
            benchmarkName
        )
        return DriftMetrics(benchmarkName, shortC, longC, drift, level, plain, quant)
    }

    fun rankByDrift(telemetries: List<CorrelationTelemetry>): List<CorrelationTelemetry> =
        telemetries.sortedByDescending { abs(it.drift) }

    fun correlation(a: List<Double>, b: List<Double>): Double {
        val n = minOf(a.size, b.size)
        if (n < 5) return 0.0
        return pearsonCorrelation(a.takeLast(n), b.takeLast(n))
    }

    private fun synthesizeStockHistoryAlignedWithLiveTail(
        stock: MarketStock,
        benchmarkReturns: List<Double>,
        liveTailReturns: List<Double>
    ): List<Double> {
        if (benchmarkReturns.isEmpty()) return liveTailReturns
        val tailLen = liveTailReturns.size.coerceAtMost(benchmarkReturns.size)
        val historicalPrefixLen = (benchmarkReturns.size - tailLen).coerceAtLeast(0)
        val tickerSeed = stock.symbol.sumOf { it.code }.toDouble()
        val prefix = (0 until historicalPrefixLen).map { i ->
            val base = benchmarkReturns[i] * stock.beta
            val idiosyncraticNoise = sin(i * 0.7 + tickerSeed) * 0.25
            base + idiosyncraticNoise
        }
        return prefix + liveTailReturns
    }

    private fun computeSeriesReturns(series: List<Double>): List<Double> {
        if (series.size < 2) return emptyList()
        return (1 until series.size).map { idx ->
            val prev = series[idx - 1].coerceAtLeast(0.0001)
            ((series[idx] - prev) / prev) * 100.0
        }
    }

    private fun alignIndexReturns(index: GlobalIndex?, range: TimeRange, targetSize: Int): List<Double> {
        if (index == null || targetSize <= 0) return emptyList()
        val points = index.historicalSeries[range]?.map { it.level }.orEmpty()
        if (points.size < 2) return emptyList()
        val allReturns = computeSeriesReturns(points)
        return if (allReturns.size >= targetSize) {
            allReturns.takeLast(targetSize)
        } else {
            allReturns
        }
    }

    fun pearsonCorrelation(x: List<Double>, y: List<Double>): Double {
        val n = minOf(x.size, y.size)
        if (n < 2) return 0.0
        val xs = x.takeLast(n)
        val ys = y.takeLast(n)
        val meanX = xs.average()
        val meanY = ys.average()

        var numerator = 0.0
        var sumSqX = 0.0
        var sumSqY = 0.0
        for (i in 0 until n) {
            val dx = xs[i] - meanX
            val dy = ys[i] - meanY
            numerator += dx * dy
            sumSqX += dx * dx
            sumSqY += dy * dy
        }
        val denom = sqrt(sumSqX * sumSqY)
        if (denom < 1e-9) return 0.0
        return (numerator / denom).coerceIn(-1.0, 1.0)
    }
}
