package com.example.data.model

enum class SubscriptionTier(
    val id: String,
    val displayName: String,
    val priceLabel: String,
    val aiModelId: String,
    val aiEngineLabel: String,
    val targetSegment: String,
    val summary: String,
    val features: List<String>
) {
    STUDENT(
        id = "student",
        displayName = "Student Scholar",
        priceLabel = "$4.99 / mo",
        aiModelId = "gemini-3.5-flash-lite",
        aiEngineLabel = "Literacy & Compounding AI (Flash-Lite)",
        targetSegment = "University & First-Time Investors",
        summary = "Guided low-volatility share picking, plain-English news decoding, and SIP compounding safety rails.",
        features = listOf(
            "Plain-English international news impact briefs",
            "Quality & safety-scored US + Global blue-chip picks",
            "Volatility guardrails & beginner SIP allocation hints",
            "Real-time portfolio P&L and price-change alerts"
        )
    ),
    ADULT(
        id = "adult",
        displayName = "Adult Quant Pro",
        priceLabel = "$19.99 / mo",
        aiModelId = "gemini-3.8-flash",
        aiEngineLabel = "Macro & Alpha Quant AI (Gemini 3.8)",
        targetSegment = "Working Professionals & Active Traders",
        summary = "Institutional macro divergence analysis across US, NIKKEI, DAX, and BSE with tactical entry signals.",
        features = listOf(
            "Deep macro-to-equity catalyst & FX divergence engine",
            "Multi-factor Alpha scores (Momentum, Value, Macro, Shield)",
            "Predictive 30-day volatility regime & hedging alerts",
            "Full cross-border NASDAQ, TSE, XETRA & BSE coverage"
        )
    );

    companion object {
        fun fromId(id: String): SubscriptionTier =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: STUDENT
    }
}

enum class RiskProfile(val id: String, val label: String) {
    CONSERVATIVE("conservative", "Conservative"),
    MODERATE("moderate", "Balanced"),
    GROWTH("growth", "High Growth");

    companion object {
        fun fromId(id: String): RiskProfile =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MODERATE
    }
}

enum class TimeRange(val label: String, val pointsCount: Int) {
    ONE_MONTH("1M", 22),
    THREE_MONTH("3M", 30),
    SIX_MONTH("6M", 36),
    ONE_YEAR("1Y", 48),
    FIVE_YEAR("5Y", 60)
}

enum class MarketRegion(val label: String) {
    GLOBAL("Global"),
    US("US Markets"),
    APAC("Asia-Pacific"),
    EUROPE("Europe"),
    INDIA("India")
}

data class IndexPoint(
    val label: String,
    val level: Double,
    val normalizedPct: Double
)

data class GlobalIndex(
    val code: String,
    val name: String,
    val country: String,
    val region: MarketRegion,
    val currency: String,
    val currentLevel: Double,
    val dailyChangePct: Double,
    val annualizedVolPct: Double,
    val peRatio: Double,
    val ytdReturnPct: Double,
    val macroDriver: String,
    val divergenceInsight: String,
    val historicalSeries: Map<TimeRange, List<IndexPoint>>
)

data class FactorScores(
    val value: Int,
    val momentum: Int,
    val macroTailwind: Int,
    val volatilityShield: Int
)

data class MarketStock(
    val symbol: String,
    val companyName: String,
    val exchange: String,
    val region: MarketRegion,
    val currency: String,
    val sector: String,
    val livePriceUsd: Double,
    val dailyChangePct: Double,
    val marketCapLabel: String,
    val peRatio: Double,
    val dividendYieldPct: Double,
    val beta: Double,
    val studentScore: Int,
    val adultAlphaScore: Int,
    val studentVerdict: String,
    val adultVerdict: String,
    val sparkline: List<Double>,
    val factors: FactorScores
)

data class PredictiveTrendAlert(
    val id: String,
    val indexOrTicker: String,
    val region: String,
    val regime: String,
    val volatilityRiskPct: Int,
    val forecastDirection: String,
    val summary: String,
    val suggestedAction: String
)

data class AiGeneratedNewsPick(
    val headline: String,
    val region: String,
    val macroImpact: String,
    val recommendedTicker: String,
    val actionSignal: String,
    val confidenceScore: Int,
    val rationale: String,
    val divergenceEffect: String
)
