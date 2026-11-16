package com.example.data.repository

import android.content.Context
import com.example.data.local.AiNewsReportEntity
import com.example.data.local.FinAdvisorDao
import com.example.data.local.FinAdvisorLocalDatabase
import com.example.data.local.PortfolioHoldingEntity
import com.example.data.local.PriceAlertEntity
import com.example.data.local.UserAdvisoryProfileEntity
import com.example.data.local.WatchlistItemEntity
import com.example.data.model.AiNewsReport
import com.example.data.model.PortfolioHolding
import com.example.data.model.PriceAlert
import com.example.data.model.RiskProfile
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserAdvisoryProfile
import com.example.data.model.WatchlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FinAdvisorRepository(
    private val dao: FinAdvisorDao,
    private val localUserId: String = DEFAULT_LOCAL_USER_ID
) {

    constructor(context: Context) : this(
        dao = FinAdvisorLocalDatabase.getInstance(context.applicationContext).finAdvisorDao()
    )

    fun observeProfile(userId: String = localUserId): Flow<UserAdvisoryProfile?> =
        dao.observeProfile(userId).map { it?.toDomain() }

    suspend fun ensureProfileExists(displayName: String = DEFAULT_DISPLAY_NAME): UserAdvisoryProfile {
        val existing = dao.getProfileOnce(localUserId)
        if (existing != null) {
            return existing.toDomain()
        }
        val now = System.currentTimeMillis()
        val safeName = displayName.ifBlank { DEFAULT_DISPLAY_NAME }.take(80)
        val entity = UserAdvisoryProfileEntity(
            userId = localUserId,
            displayName = safeName,
            subscriptionTier = SubscriptionTier.STUDENT.id,
            riskProfile = RiskProfile.MODERATE.id,
            alertThresholdPct = 2.5,
            notificationsEnabled = true,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        dao.upsertProfile(entity)
        return entity.toDomain()
    }

    suspend fun updateSubscriptionTier(tier: SubscriptionTier) {
        ensureProfileExists()
        dao.updateSubscriptionTier(
            userId = localUserId,
            tierId = tier.id,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun updateAdvisoryPreferences(
        riskProfile: RiskProfile,
        alertThresholdPct: Double,
        notificationsEnabled: Boolean
    ) {
        ensureProfileExists()
        val clampedThreshold = alertThresholdPct.coerceIn(0.5, 25.0)
        dao.updateAdvisoryPreferences(
            userId = localUserId,
            riskProfileId = riskProfile.id,
            alertThresholdPct = clampedThreshold,
            notificationsEnabled = notificationsEnabled,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun observeHoldings(userId: String = localUserId): Flow<List<PortfolioHolding>> =
        dao.observeHoldings(userId).map { list -> list.map { it.toDomain() } }

    suspend fun addHolding(holding: PortfolioHolding): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = PortfolioHoldingEntity(
            id = id,
            userId = localUserId,
            symbol = holding.symbol.trim().uppercase().take(20),
            companyName = holding.companyName.trim().take(100),
            exchange = holding.exchange,
            shares = holding.shares.coerceAtLeast(0.01),
            avgCostPrice = holding.avgCostPrice.coerceAtLeast(0.01),
            currency = holding.currency.ifBlank { "USD" }.take(10),
            sector = holding.sector.ifBlank { "Equities" }.take(60),
            createdAtMillis = now,
            updatedAtMillis = now
        )
        dao.upsertHolding(entity)
        return id
    }

    suspend fun updateHolding(holding: PortfolioHolding) {
        require(holding.id.isNotBlank()) { "Holding ID must not be blank for update" }
        dao.updateHoldingPosition(
            id = holding.id,
            userId = localUserId,
            shares = holding.shares.coerceAtLeast(0.01),
            avgCostPrice = holding.avgCostPrice.coerceAtLeast(0.01),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun deleteHolding(holdingId: String) {
        require(holdingId.isNotBlank()) { "Holding ID must not be blank for delete" }
        dao.deleteHolding(holdingId, localUserId)
    }

    suspend fun seedStarterGlobalPortfolio() {
        if (dao.countHoldings(localUserId) > 0) return
        val starters = listOf(
            PortfolioHolding(
                symbol = "NVDA",
                companyName = "NVIDIA Corporation",
                exchange = "NASDAQ",
                shares = 10.0,
                avgCostPrice = 124.50,
                currency = "USD",
                sector = "AI Semiconductors"
            ),
            PortfolioHolding(
                symbol = "7203.T",
                companyName = "Toyota Motor Corp",
                exchange = "TSE",
                shares = 45.0,
                avgCostPrice = 18.90,
                currency = "JPY",
                sector = "Mobility & Robotics"
            ),
            PortfolioHolding(
                symbol = "SAP.DE",
                companyName = "SAP SE",
                exchange = "XETRA",
                shares = 6.0,
                avgCostPrice = 212.00,
                currency = "EUR",
                sector = "Enterprise Cloud"
            ),
            PortfolioHolding(
                symbol = "RELIANCE.BO",
                companyName = "Reliance Industries",
                exchange = "BSE",
                shares = 30.0,
                avgCostPrice = 33.80,
                currency = "INR",
                sector = "Energy & Digital"
            )
        )
        starters.forEach { addHolding(it) }
    }

    fun observeAlerts(userId: String = localUserId): Flow<List<PriceAlert>> =
        dao.observeAlerts(userId).map { list -> list.map { it.toDomain() } }

    suspend fun createAlert(alert: PriceAlert): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = PriceAlertEntity(
            id = id,
            userId = localUserId,
            symbol = alert.symbol.trim().uppercase().take(20),
            exchange = alert.exchange.ifBlank { "NASDAQ" }.take(20),
            targetChangePct = alert.targetChangePct.coerceIn(0.25, 50.0),
            direction = alert.direction,
            triggerMessage = alert.triggerMessage.take(300),
            triggered = alert.triggered,
            read = alert.read,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        dao.upsertAlert(entity)
        return id
    }

    suspend fun markAlertTriggered(alertId: String, message: String) {
        dao.markAlertTriggered(
            id = alertId,
            userId = localUserId,
            message = message.take(300),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun markAlertRead(alertId: String) {
        dao.markAlertRead(
            id = alertId,
            userId = localUserId,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun deleteAlert(alertId: String) {
        dao.deleteAlert(alertId, localUserId)
    }

    fun observeAiReports(userId: String = localUserId): Flow<List<AiNewsReport>> =
        dao.observeAiReports(userId).map { list -> list.map { it.toDomain() } }

    suspend fun saveAiReport(report: AiNewsReport): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = AiNewsReportEntity(
            id = id,
            userId = localUserId,
            tierUsed = report.tierUsed,
            headline = report.headline.take(240),
            region = report.region.take(40),
            macroImpact = report.macroImpact.take(1200),
            recommendedTicker = report.recommendedTicker.take(24),
            actionSignal = report.actionSignal,
            confidenceScore = report.confidenceScore.coerceIn(0L, 100L),
            rationale = report.rationale.take(1500),
            createdAtMillis = now,
            updatedAtMillis = now
        )
        dao.upsertAiReport(entity)
        return id
    }

    suspend fun deleteAiReport(reportId: String) {
        dao.deleteAiReport(reportId, localUserId)
    }

    fun observeWatchlist(userId: String = localUserId): Flow<List<WatchlistItem>> =
        dao.observeWatchlist(userId).map { list -> list.map { it.toDomain() } }

    suspend fun addToWatchlist(item: WatchlistItem): String {
        val safeDocId = item.symbol.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "_").take(128)
        val now = System.currentTimeMillis()
        val entity = WatchlistItemEntity(
            id = safeDocId,
            userId = localUserId,
            symbol = item.symbol.trim().uppercase().take(20),
            companyName = item.companyName.trim().take(100),
            exchange = item.exchange,
            divergenceTag = item.divergenceTag,
            divergenceAlertActive = item.divergenceAlertActive,
            divergenceConfidence = item.divergenceConfidence.coerceIn(0, 100),
            aiCorrelationSummary = item.aiCorrelationSummary.take(1200),
            targetIndices = item.targetIndices.ifBlank { "NIKKEI 225, DAX 40, BSE SENSEX" }.take(120),
            createdAtMillis = now,
            updatedAtMillis = now
        )
        dao.upsertWatchlistItem(entity)
        return safeDocId
    }

    suspend fun updateWatchlistDivergence(
        watchlistId: String,
        divergenceTag: String,
        divergenceAlertActive: Boolean,
        divergenceConfidence: Int,
        aiCorrelationSummary: String,
        targetIndices: String = "NIKKEI 225, DAX 40, BSE SENSEX"
    ) {
        require(watchlistId.isNotBlank()) { "Watchlist ID must not be blank for update" }
        dao.updateWatchlistDivergence(
            id = watchlistId,
            userId = localUserId,
            divergenceTag = divergenceTag,
            divergenceAlertActive = divergenceAlertActive,
            divergenceConfidence = divergenceConfidence.coerceIn(0, 100),
            aiCorrelationSummary = aiCorrelationSummary.take(1200),
            targetIndices = targetIndices.take(120),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun removeFromWatchlist(watchlistId: String) {
        require(watchlistId.isNotBlank()) { "Watchlist ID must not be blank for delete" }
        dao.deleteWatchlistItem(watchlistId, localUserId)
    }

    suspend fun seedStarterWatchlist() {
        if (dao.countWatchlist(localUserId) > 0) return
        val starters = listOf(
            WatchlistItem(
                symbol = "NVDA",
                companyName = "NVIDIA Corporation",
                exchange = "NASDAQ",
                divergenceTag = "NONE",
                divergenceAlertActive = false,
                divergenceConfidence = 0,
                aiCorrelationSummary = "Awaiting AI scan against historical NIKKEI 225, DAX 40 & BSE SENSEX correlations.",
                targetIndices = "NIKKEI 225, DAX 40, BSE SENSEX"
            ),
            WatchlistItem(
                symbol = "7203.T",
                companyName = "Toyota Motor Corp",
                exchange = "TSE",
                divergenceTag = "NONE",
                divergenceAlertActive = false,
                divergenceConfidence = 0,
                aiCorrelationSummary = "Awaiting AI scan against historical NIKKEI 225, DAX 40 & BSE SENSEX correlations.",
                targetIndices = "NIKKEI 225, DAX 40, BSE SENSEX"
            ),
            WatchlistItem(
                symbol = "SAP.DE",
                companyName = "SAP SE",
                exchange = "XETRA",
                divergenceTag = "NONE",
                divergenceAlertActive = false,
                divergenceConfidence = 0,
                aiCorrelationSummary = "Awaiting AI scan against historical NIKKEI 225, DAX 40 & BSE SENSEX correlations.",
                targetIndices = "NIKKEI 225, DAX 40, BSE SENSEX"
            ),
            WatchlistItem(
                symbol = "RELIANCE.BO",
                companyName = "Reliance Industries",
                exchange = "BSE",
                divergenceTag = "NONE",
                divergenceAlertActive = false,
                divergenceConfidence = 0,
                aiCorrelationSummary = "Awaiting AI scan against historical NIKKEI 225, DAX 40 & BSE SENSEX correlations.",
                targetIndices = "NIKKEI 225, DAX 40, BSE SENSEX"
            )
        )
        starters.forEach { addToWatchlist(it) }
    }

    companion object {
        const val DEFAULT_LOCAL_USER_ID = "local_investor"
        const val DEFAULT_DISPLAY_NAME = "FinAdvisor Investor"
    }
}
