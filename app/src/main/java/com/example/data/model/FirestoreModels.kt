package com.example.data.model

data class UserAdvisoryProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val subscriptionTier: String = SubscriptionTier.STUDENT.id,
    val riskProfile: String = RiskProfile.MODERATE.id,
    val alertThresholdPct: Double = 2.5,
    val notificationsEnabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class PortfolioHolding(
    val id: String = "",
    val userId: String = "",
    val symbol: String = "",
    val companyName: String = "",
    val exchange: String = "NASDAQ",
    val shares: Double = 0.0,
    val avgCostPrice: Double = 0.0,
    val currency: String = "USD",
    val sector: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class PriceAlert(
    val id: String = "",
    val userId: String = "",
    val symbol: String = "",
    val exchange: String = "",
    val targetChangePct: Double = 2.5,
    val direction: String = "both",
    val triggerMessage: String = "",
    val triggered: Boolean = false,
    val read: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class AiNewsReport(
    val id: String = "",
    val userId: String = "",
    val tierUsed: String = SubscriptionTier.STUDENT.id,
    val headline: String = "",
    val region: String = "GLOBAL",
    val macroImpact: String = "",
    val recommendedTicker: String = "",
    val actionSignal: String = "ACCUMULATE",
    val confidenceScore: Long = 80L,
    val rationale: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class WatchlistItem(
    val id: String = "",
    val userId: String = "",
    val symbol: String = "",
    val companyName: String = "",
    val exchange: String = "NASDAQ",
    val divergenceTag: String = "NONE",
    val divergenceAlertActive: Boolean = false,
    val divergenceConfidence: Int = 0,
    val aiCorrelationSummary: String = "",
    val targetIndices: String = "NIKKEI 225, DAX 40, BSE SENSEX",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
