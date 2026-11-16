package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AiNewsReport
import com.example.data.model.PortfolioHolding
import com.example.data.model.PriceAlert
import com.example.data.model.RiskProfile
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserAdvisoryProfile
import com.example.data.model.WatchlistItem

@Entity(tableName = "user_advisory_profiles")
data class UserAdvisoryProfileEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val subscriptionTier: String = SubscriptionTier.STUDENT.id,
    val riskProfile: String = RiskProfile.MODERATE.id,
    val alertThresholdPct: Double = 2.5,
    val notificationsEnabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): UserAdvisoryProfile = UserAdvisoryProfile(
        id = userId,
        userId = userId,
        displayName = displayName,
        subscriptionTier = subscriptionTier,
        riskProfile = riskProfile,
        alertThresholdPct = alertThresholdPct,
        notificationsEnabled = notificationsEnabled,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "portfolio_holdings")
data class PortfolioHoldingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val symbol: String,
    val companyName: String,
    val exchange: String,
    val shares: Double,
    val avgCostPrice: Double,
    val currency: String,
    val sector: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): PortfolioHolding = PortfolioHolding(
        id = id,
        userId = userId,
        symbol = symbol,
        companyName = companyName,
        exchange = exchange,
        shares = shares,
        avgCostPrice = avgCostPrice,
        currency = currency,
        sector = sector,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val symbol: String,
    val exchange: String,
    val targetChangePct: Double,
    val direction: String,
    val triggerMessage: String,
    val triggered: Boolean,
    val read: Boolean,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): PriceAlert = PriceAlert(
        id = id,
        userId = userId,
        symbol = symbol,
        exchange = exchange,
        targetChangePct = targetChangePct,
        direction = direction,
        triggerMessage = triggerMessage,
        triggered = triggered,
        read = read,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "ai_news_reports")
data class AiNewsReportEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val tierUsed: String,
    val headline: String,
    val region: String,
    val macroImpact: String,
    val recommendedTicker: String,
    val actionSignal: String,
    val confidenceScore: Long,
    val rationale: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): AiNewsReport = AiNewsReport(
        id = id,
        userId = userId,
        tierUsed = tierUsed,
        headline = headline,
        region = region,
        macroImpact = macroImpact,
        recommendedTicker = recommendedTicker,
        actionSignal = actionSignal,
        confidenceScore = confidenceScore,
        rationale = rationale,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "watchlist_items")
data class WatchlistItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val symbol: String,
    val companyName: String,
    val exchange: String,
    val divergenceTag: String,
    val divergenceAlertActive: Boolean,
    val divergenceConfidence: Int,
    val aiCorrelationSummary: String,
    val targetIndices: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): WatchlistItem = WatchlistItem(
        id = id,
        userId = userId,
        symbol = symbol,
        companyName = companyName,
        exchange = exchange,
        divergenceTag = divergenceTag,
        divergenceAlertActive = divergenceAlertActive,
        divergenceConfidence = divergenceConfidence,
        aiCorrelationSummary = aiCorrelationSummary,
        targetIndices = targetIndices,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}
