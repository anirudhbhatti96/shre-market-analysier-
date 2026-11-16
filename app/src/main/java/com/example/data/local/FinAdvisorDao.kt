package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinAdvisorDao {

    @Query("SELECT * FROM user_advisory_profiles WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<UserAdvisoryProfileEntity?>

    @Query("SELECT * FROM user_advisory_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfileOnce(userId: String): UserAdvisoryProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserAdvisoryProfileEntity)

    @Query("UPDATE user_advisory_profiles SET subscriptionTier = :tierId, updatedAtMillis = :updatedAtMillis WHERE userId = :userId")
    suspend fun updateSubscriptionTier(userId: String, tierId: String, updatedAtMillis: Long)

    @Query(
        "UPDATE user_advisory_profiles " +
            "SET riskProfile = :riskProfileId, alertThresholdPct = :alertThresholdPct, " +
            "notificationsEnabled = :notificationsEnabled, updatedAtMillis = :updatedAtMillis " +
            "WHERE userId = :userId"
    )
    suspend fun updateAdvisoryPreferences(
        userId: String,
        riskProfileId: String,
        alertThresholdPct: Double,
        notificationsEnabled: Boolean,
        updatedAtMillis: Long
    )

    @Query("SELECT * FROM portfolio_holdings WHERE userId = :userId ORDER BY symbol ASC")
    fun observeHoldings(userId: String): Flow<List<PortfolioHoldingEntity>>

    @Query("SELECT COUNT(*) FROM portfolio_holdings WHERE userId = :userId")
    suspend fun countHoldings(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHolding(holding: PortfolioHoldingEntity)

    @Query(
        "UPDATE portfolio_holdings " +
            "SET shares = :shares, avgCostPrice = :avgCostPrice, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :id AND userId = :userId"
    )
    suspend fun updateHoldingPosition(
        id: String,
        userId: String,
        shares: Double,
        avgCostPrice: Double,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM portfolio_holdings WHERE id = :id AND userId = :userId")
    suspend fun deleteHolding(id: String, userId: String)

    @Query("SELECT * FROM price_alerts WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    fun observeAlerts(userId: String): Flow<List<PriceAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlert(alert: PriceAlertEntity)

    @Query(
        "UPDATE price_alerts " +
            "SET triggered = 1, triggerMessage = :message, read = 0, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :id AND userId = :userId"
    )
    suspend fun markAlertTriggered(id: String, userId: String, message: String, updatedAtMillis: Long)

    @Query("UPDATE price_alerts SET read = 1, updatedAtMillis = :updatedAtMillis WHERE id = :id AND userId = :userId")
    suspend fun markAlertRead(id: String, userId: String, updatedAtMillis: Long)

    @Query("DELETE FROM price_alerts WHERE id = :id AND userId = :userId")
    suspend fun deleteAlert(id: String, userId: String)

    @Query("SELECT * FROM ai_news_reports WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeAiReports(userId: String): Flow<List<AiNewsReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAiReport(report: AiNewsReportEntity)

    @Query("DELETE FROM ai_news_reports WHERE id = :id AND userId = :userId")
    suspend fun deleteAiReport(id: String, userId: String)

    @Query(
        "SELECT * FROM watchlist_items WHERE userId = :userId " +
            "ORDER BY divergenceAlertActive DESC, divergenceConfidence DESC, symbol ASC"
    )
    fun observeWatchlist(userId: String): Flow<List<WatchlistItemEntity>>

    @Query("SELECT COUNT(*) FROM watchlist_items WHERE userId = :userId")
    suspend fun countWatchlist(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchlistItem(item: WatchlistItemEntity)

    @Query(
        "UPDATE watchlist_items " +
            "SET divergenceTag = :divergenceTag, divergenceAlertActive = :divergenceAlertActive, " +
            "divergenceConfidence = :divergenceConfidence, aiCorrelationSummary = :aiCorrelationSummary, " +
            "targetIndices = :targetIndices, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :id AND userId = :userId"
    )
    suspend fun updateWatchlistDivergence(
        id: String,
        userId: String,
        divergenceTag: String,
        divergenceAlertActive: Boolean,
        divergenceConfidence: Int,
        aiCorrelationSummary: String,
        targetIndices: String,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM watchlist_items WHERE id = :id AND userId = :userId")
    suspend fun deleteWatchlistItem(id: String, userId: String)
}
