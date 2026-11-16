package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DiagnosticAuditEntity
import com.example.data.local.FinAdvisorLocalDatabase
import com.example.data.model.AiNewsReport
import com.example.data.model.PortfolioHolding
import com.example.data.model.PriceAlert
import com.example.data.model.RiskProfile
import com.example.data.model.SubscriptionTier
import com.example.data.model.WatchlistItem
import com.example.data.repository.FinAdvisorRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: FinAdvisorLocalDatabase

    @Before
    fun setupDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinAdvisorLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDownDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FinAdvisor", appName)
    }

    @Test
    fun `room diagnostic audit dao isolates and persists user traces`() = runBlocking {
        val dao = db.diagnosticAuditDao()
        val id1 = dao.insertAudit(
            DiagnosticAuditEntity(
                userId = "user_alpha",
                scenarioExpression = "PORTFOLIO_AUDIT()",
                tierId = "student",
                weightedBeta = 1.12,
                topConcentrationLabel = "NASDAQ 60.0%",
                anomalyCount = 2,
                contextSummary = "Student Scholar audit"
            )
        )
        dao.insertAudit(
            DiagnosticAuditEntity(
                userId = "user_beta",
                scenarioExpression = "ECB -25BPS && DAX_PE < 16",
                tierId = "adult",
                weightedBeta = 0.94,
                topConcentrationLabel = "XETRA 50.0%",
                anomalyCount = 0,
                contextSummary = "Adult Quant Pro audit"
            )
        )

        val alphaAudits = dao.observeAuditsForUser("user_alpha").first()
        assertEquals(1, alphaAudits.size)
        assertEquals("PORTFOLIO_AUDIT()", alphaAudits.first().scenarioExpression)

        dao.deleteAuditById(id1)
        assertTrue(dao.observeAuditsForUser("user_alpha").first().isEmpty())
        assertEquals(1, dao.observeAuditsForUser("user_beta").first().size)
    }

    @Test
    fun `local repository persists profile tiers holdings alerts reports and watchlist`() = runBlocking {
        val repo = FinAdvisorRepository(db.finAdvisorDao())

        val profile = repo.ensureProfileExists()
        assertEquals(FinAdvisorRepository.DEFAULT_LOCAL_USER_ID, profile.userId)
        assertEquals(SubscriptionTier.STUDENT.id, profile.subscriptionTier)

        repo.updateSubscriptionTier(SubscriptionTier.ADULT)
        repo.updateAdvisoryPreferences(RiskProfile.GROWTH, 3.5, false)
        val updatedProfile = repo.observeProfile().first()
        assertEquals(SubscriptionTier.ADULT.id, updatedProfile?.subscriptionTier)
        assertEquals(RiskProfile.GROWTH.id, updatedProfile?.riskProfile)
        assertEquals(3.5, updatedProfile?.alertThresholdPct ?: 0.0, 0.01)

        repo.seedStarterGlobalPortfolio()
        val seededHoldings = repo.observeHoldings().first()
        assertEquals(4, seededHoldings.size)
        val extraHoldingId = repo.addHolding(
            PortfolioHolding(
                symbol = "MSFT",
                companyName = "Microsoft Corp",
                exchange = "NASDAQ",
                shares = 3.0,
                avgCostPrice = 410.0,
                currency = "USD",
                sector = "Cloud"
            )
        )
        assertEquals(5, repo.observeHoldings().first().size)
        repo.deleteHolding(extraHoldingId)
        assertEquals(4, repo.observeHoldings().first().size)

        val alertId = repo.createAlert(
            PriceAlert(
                symbol = "NVDA",
                exchange = "NASDAQ",
                targetChangePct = 2.5,
                direction = "both",
                triggerMessage = "Monitoring +/-2.5%"
            )
        )
        repo.markAlertTriggered(alertId, "NVDA surged +3.2%")
        val triggeredAlert = repo.observeAlerts().first().first { it.id == alertId }
        assertTrue(triggeredAlert.triggered)
        repo.markAlertRead(alertId)
        assertTrue(repo.observeAlerts().first().first { it.id == alertId }.read)

        val reportId = repo.saveAiReport(
            AiNewsReport(
                tierUsed = SubscriptionTier.ADULT.id,
                headline = "BOJ Policy Shift Lifts Exporters",
                region = "APAC",
                macroImpact = "NIKKEI outperforms DAX",
                recommendedTicker = "7203.T",
                actionSignal = "BUY",
                confidenceScore = 86L,
                rationale = "FX tailwind"
            )
        )
        assertEquals(1, repo.observeAiReports().first().size)
        repo.deleteAiReport(reportId)
        assertTrue(repo.observeAiReports().first().isEmpty())

        val watchDocId = repo.addToWatchlist(
            WatchlistItem(
                symbol = "SAP.DE",
                companyName = "SAP SE",
                exchange = "XETRA"
            )
        )
        assertEquals("SAP_DE", watchDocId)
        repo.updateWatchlistDivergence(
            watchlistId = watchDocId,
            divergenceTag = "DIVERGENCE_ALERT",
            divergenceAlertActive = true,
            divergenceConfidence = 91,
            aiCorrelationSummary = "Decoupled from DAX 40 and NIKKEI 225.",
            targetIndices = "NIKKEI 225, DAX 40, BSE SENSEX"
        )
        val watchItem = repo.observeWatchlist().first().first { it.id == watchDocId }
        assertTrue(watchItem.divergenceAlertActive)
        assertEquals("DIVERGENCE_ALERT", watchItem.divergenceTag)
        assertEquals(91, watchItem.divergenceConfidence)
    }
}
