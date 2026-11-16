package com.example.data.market

import com.example.data.model.FactorScores
import com.example.data.model.GlobalIndex
import com.example.data.model.IndexPoint
import com.example.data.model.MarketRegion
import com.example.data.model.MarketStock
import com.example.data.model.PredictiveTrendAlert
import com.example.data.model.TimeRange
import kotlin.math.sin

object GlobalMarketCatalog {

    private fun buildSeries(
        baseLevel: Double,
        totalReturnPct: Double,
        volatilityAmp: Double,
        phaseShift: Double,
        range: TimeRange
    ): List<IndexPoint> {
        val count = range.pointsCount
        val startLevel = baseLevel / (1.0 + totalReturnPct / 100.0)
        return (0 until count).map { i ->
            val progress = i.toDouble() / (count - 1).coerceAtLeast(1)
            val wave = sin(progress * Math.PI * 3.5 + phaseShift) * volatilityAmp * (1.0 - progress * 0.25)
            val normPct = (totalReturnPct * progress) + wave
            val level = startLevel * (1.0 + normPct / 100.0)
            val label = when (range) {
                TimeRange.ONE_MONTH -> "D${i + 1}"
                TimeRange.THREE_MONTH -> "W${(i / 2) + 1}"
                TimeRange.SIX_MONTH -> "W${i + 1}"
                TimeRange.ONE_YEAR -> "M${(i / 4) + 1}"
                TimeRange.FIVE_YEAR -> "Y${2022 + (i / 12)}"
            }
            IndexPoint(
                label = label,
                level = level,
                normalizedPct = normPct
            )
        }
    }

    val initialIndices: List<GlobalIndex> = listOf(
        GlobalIndex(
            code = "SPX",
            name = "S&P 500",
            country = "United States",
            region = MarketRegion.US,
            currency = "USD",
            currentLevel = 5984.40,
            dailyChangePct = 0.74,
            annualizedVolPct = 13.8,
            peRatio = 25.4,
            ytdReturnPct = 21.8,
            macroDriver = "AI capex resilience & Fed rate path stabilization",
            divergenceInsight = "Leading global tech momentum; trading at +4.2x P/E premium vs DAX.",
            historicalSeries = mapOf(
                TimeRange.ONE_MONTH to buildSeries(5984.40, 3.1, 1.1, 0.2, TimeRange.ONE_MONTH),
                TimeRange.THREE_MONTH to buildSeries(5984.40, 7.6, 1.8, 0.4, TimeRange.THREE_MONTH),
                TimeRange.SIX_MONTH to buildSeries(5984.40, 12.9, 2.4, 0.6, TimeRange.SIX_MONTH),
                TimeRange.ONE_YEAR to buildSeries(5984.40, 26.4, 3.2, 0.8, TimeRange.ONE_YEAR),
                TimeRange.FIVE_YEAR to buildSeries(5984.40, 88.5, 6.8, 1.0, TimeRange.FIVE_YEAR)
            )
        ),
        GlobalIndex(
            code = "NDX",
            name = "NASDAQ 100",
            country = "United States",
            region = MarketRegion.US,
            currency = "USD",
            currentLevel = 21048.15,
            dailyChangePct = 1.18,
            annualizedVolPct = 18.6,
            peRatio = 31.2,
            ytdReturnPct = 25.4,
            macroDriver = "Hyperscaler GPU demand & cloud software margin expansion",
            divergenceInsight = "Outperforming European industrials by +14.1% YTD on AI earnings upgrades.",
            historicalSeries = mapOf(
                TimeRange.ONE_MONTH to buildSeries(21048.15, 4.4, 1.6, 0.5, TimeRange.ONE_MONTH),
                TimeRange.THREE_MONTH to buildSeries(21048.15, 9.8, 2.6, 0.9, TimeRange.THREE_MONTH),
                TimeRange.SIX_MONTH to buildSeries(21048.15, 15.7, 3.3, 1.1, TimeRange.SIX_MONTH),
                TimeRange.ONE_YEAR to buildSeries(21048.15, 31.2, 4.5, 1.3, TimeRange.ONE_YEAR),
                TimeRange.FIVE_YEAR to buildSeries(21048.15, 119.4, 9.2, 1.5, TimeRange.FIVE_YEAR)
            )
        ),
        GlobalIndex(
            code = "N225",
            name = "NIKKEI 225",
            country = "Japan",
            region = MarketRegion.APAC,
            currency = "JPY",
            currentLevel = 39180.60,
            dailyChangePct = 0.92,
            annualizedVolPct = 19.4,
            peRatio = 19.1,
            ytdReturnPct = 16.9,
            macroDriver = "Tokyo corporate governance reforms & export JPY tailwinds",
            divergenceInsight = "Sharp divergence vs DAX during BOJ policy shifts; strong foreign inflows.",
            historicalSeries = mapOf(
                TimeRange.ONE_MONTH to buildSeries(39180.60, 2.2, 1.9, 1.2, TimeRange.ONE_MONTH),
                TimeRange.THREE_MONTH to buildSeries(39180.60, 5.4, 3.1, 1.5, TimeRange.THREE_MONTH),
                TimeRange.SIX_MONTH to buildSeries(39180.60, 9.6, 4.2, 1.8, TimeRange.SIX_MONTH),
                TimeRange.ONE_YEAR to buildSeries(39180.60, 22.1, 5.0, 2.1, TimeRange.ONE_YEAR),
                TimeRange.FIVE_YEAR to buildSeries(39180.60, 67.2, 7.5, 2.4, TimeRange.FIVE_YEAR)
            )
        ),
        GlobalIndex(
            code = "DAX",
            name = "DAX 40",
            country = "Germany",
            region = MarketRegion.EUROPE,
            currency = "EUR",
            currentLevel = 19412.80,
            dailyChangePct = -0.31,
            annualizedVolPct = 14.9,
            peRatio = 14.3,
            ytdReturnPct = 11.3,
            macroDriver = "ECB rate cuts offset by Eurozone manufacturing headwinds",
            divergenceInsight = "Deep value discount (14.3x P/E) vs US; software & defense leaders carry index.",
            historicalSeries = mapOf(
                TimeRange.ONE_MONTH to buildSeries(19412.80, 0.8, 1.3, 2.3, TimeRange.ONE_MONTH),
                TimeRange.THREE_MONTH to buildSeries(19412.80, 3.2, 1.9, 2.6, TimeRange.THREE_MONTH),
                TimeRange.SIX_MONTH to buildSeries(19412.80, 6.4, 2.5, 2.8, TimeRange.SIX_MONTH),
                TimeRange.ONE_YEAR to buildSeries(19412.80, 17.5, 3.4, 3.0, TimeRange.ONE_YEAR),
                TimeRange.FIVE_YEAR to buildSeries(19412.80, 48.0, 6.2, 3.2, TimeRange.FIVE_YEAR)
            )
        ),
        GlobalIndex(
            code = "SENSEX",
            name = "BSE SENSEX",
            country = "India",
            region = MarketRegion.INDIA,
            currency = "INR",
            currentLevel = 79842.30,
            dailyChangePct = 0.64,
            annualizedVolPct = 12.6,
            peRatio = 23.1,
            ytdReturnPct = 14.8,
            macroDriver = "Domestic SIP liquidity & infrastructure capex cycle",
            divergenceInsight = "Lowest volatility among emerging peers; decoupled from European PMI slowdown.",
            historicalSeries = mapOf(
                TimeRange.ONE_MONTH to buildSeries(79842.30, 1.9, 1.0, 3.1, TimeRange.ONE_MONTH),
                TimeRange.THREE_MONTH to buildSeries(79842.30, 4.9, 1.5, 3.4, TimeRange.THREE_MONTH),
                TimeRange.SIX_MONTH to buildSeries(79842.30, 10.8, 2.0, 3.7, TimeRange.SIX_MONTH),
                TimeRange.ONE_YEAR to buildSeries(79842.30, 21.6, 2.8, 4.0, TimeRange.ONE_YEAR),
                TimeRange.FIVE_YEAR to buildSeries(79842.30, 96.3, 5.4, 4.2, TimeRange.FIVE_YEAR)
            )
        ),
        GlobalIndex(
            code = "FTSE",
            name = "FTSE 100",
            country = "United Kingdom",
            region = MarketRegion.EUROPE,
            currency = "GBP",
            currentLevel = 8245.10,
            dailyChangePct = 0.21,
            annualizedVolPct = 11.4,
            peRatio = 12.2,
            ytdReturnPct = 7.4,
            macroDriver = "Commodity & global banking dividend yields",
            divergenceInsight = "Defensive income anchor lagging US growth by -14.4% YTD.",
            historicalSeries = mapOf(
                TimeRange.ONE_MONTH to buildSeries(8245.10, 0.5, 0.9, 4.4, TimeRange.ONE_MONTH),
                TimeRange.THREE_MONTH to buildSeries(8245.10, 2.1, 1.2, 4.6, TimeRange.THREE_MONTH),
                TimeRange.SIX_MONTH to buildSeries(8245.10, 4.2, 1.7, 4.8, TimeRange.SIX_MONTH),
                TimeRange.ONE_YEAR to buildSeries(8245.10, 11.2, 2.3, 5.0, TimeRange.ONE_YEAR),
                TimeRange.FIVE_YEAR to buildSeries(8245.10, 26.8, 4.9, 5.1, TimeRange.FIVE_YEAR)
            )
        )
    )

    val initialStocks: List<MarketStock> = listOf(
        MarketStock(
            symbol = "NVDA",
            companyName = "NVIDIA Corporation",
            exchange = "NASDAQ",
            region = MarketRegion.US,
            currency = "USD",
            sector = "AI Semiconductors",
            livePriceUsd = 141.80,
            dailyChangePct = 2.35,
            marketCapLabel = "$3.47T",
            peRatio = 46.2,
            dividendYieldPct = 0.03,
            beta = 1.65,
            studentScore = 76,
            adultAlphaScore = 94,
            studentVerdict = "High growth AI leader—allocate smaller position sizes via DCA due to higher swings.",
            adultVerdict = "Core AI infrastructure alpha pick; hyperscaler capex cycle underpins 2026–27 EPS revisions.",
            sparkline = listOf(128.0, 130.5, 132.1, 131.4, 135.8, 138.2, 139.0, 141.8),
            factors = FactorScores(value = 54, momentum = 96, macroTailwind = 92, volatilityShield = 48)
        ),
        MarketStock(
            symbol = "MSFT",
            companyName = "Microsoft Corp.",
            exchange = "NASDAQ",
            region = MarketRegion.US,
            currency = "USD",
            sector = "Enterprise AI & Cloud",
            livePriceUsd = 428.50,
            dailyChangePct = 0.85,
            marketCapLabel = "$3.18T",
            peRatio = 33.8,
            dividendYieldPct = 0.78,
            beta = 0.92,
            studentScore = 93,
            adultAlphaScore = 88,
            studentVerdict = "Ideal anchor stock for students: fortress balance sheet, steady cloud revenue, low drawdown.",
            adultVerdict = "Quality compounder with Azure AI monetization defending valuation multiple across rate regimes.",
            sparkline = listOf(412.0, 415.4, 418.0, 416.2, 421.5, 424.0, 426.3, 428.5),
            factors = FactorScores(value = 71, momentum = 84, macroTailwind = 89, volatilityShield = 88)
        ),
        MarketStock(
            symbol = "AAPL",
            companyName = "Apple Inc.",
            exchange = "NASDAQ",
            region = MarketRegion.US,
            currency = "USD",
            sector = "Consumer Ecosystem",
            livePriceUsd = 229.40,
            dailyChangePct = 0.54,
            marketCapLabel = "$3.48T",
            peRatio = 32.1,
            dividendYieldPct = 0.44,
            beta = 1.04,
            studentScore = 91,
            adultAlphaScore = 82,
            studentVerdict = "High brand loyalty & recurring Services cash flows make this a resilient beginner holding.",
            adultVerdict = "On-device AI upgrade cycle supports services ARPU while buybacks cushion macro dips.",
            sparkline = listOf(221.0, 222.8, 224.0, 223.1, 225.7, 227.4, 228.1, 229.4),
            factors = FactorScores(value = 68, momentum = 79, macroTailwind = 81, volatilityShield = 86)
        ),
        MarketStock(
            symbol = "TSM",
            companyName = "TSMC (ADR)",
            exchange = "NYSE",
            region = MarketRegion.US,
            currency = "USD",
            sector = "Foundry Semiconductors",
            livePriceUsd = 194.20,
            dailyChangePct = 1.62,
            marketCapLabel = "$1.01T",
            peRatio = 25.9,
            dividendYieldPct = 1.32,
            beta = 1.24,
            studentScore = 82,
            adultAlphaScore = 92,
            studentVerdict = "Essential picks-and-shovels chip maker; diversify alongside US software to manage geopolitical risk.",
            adultVerdict = "Monopoly in 3nm/2nm leading-edge nodes trades at steep valuation discount to US fabless peers.",
            sparkline = listOf(179.0, 182.0, 185.5, 184.0, 188.4, 190.1, 192.3, 194.2),
            factors = FactorScores(value = 82, momentum = 91, macroTailwind = 88, volatilityShield = 67)
        ),
        MarketStock(
            symbol = "7203.T",
            companyName = "Toyota Motor Corp",
            exchange = "TSE",
            region = MarketRegion.APAC,
            currency = "JPY",
            sector = "Hybrid Mobility",
            livePriceUsd = 19.65,
            dailyChangePct = 1.15,
            marketCapLabel = "$264B",
            peRatio = 9.8,
            dividendYieldPct = 2.85,
            beta = 0.78,
            studentScore = 87,
            adultAlphaScore = 86,
            studentVerdict = "Low P/E value leader with strong dividends—great international diversification beyond US tech.",
            adultVerdict = "Nikkei beneficiary of hybrid margin surge and Tokyo Stock Exchange capital efficiency reforms.",
            sparkline = listOf(18.5, 18.7, 18.9, 19.0, 19.2, 19.3, 19.5, 19.65),
            factors = FactorScores(value = 92, momentum = 78, macroTailwind = 85, volatilityShield = 84)
        ),
        MarketStock(
            symbol = "6758.T",
            companyName = "Sony Group Corp",
            exchange = "TSE",
            region = MarketRegion.APAC,
            currency = "JPY",
            sector = "Entertainment & Sensors",
            livePriceUsd = 20.40,
            dailyChangePct = 0.72,
            marketCapLabel = "$124B",
            peRatio = 17.4,
            dividendYieldPct = 0.65,
            beta = 0.94,
            studentScore = 85,
            adultAlphaScore = 84,
            studentVerdict = "Balanced Japanese tech & gaming giant; smooths out semiconductor-heavy student portfolios.",
            adultVerdict = "Music & gaming recurring IP cash flows insulate against JPY appreciation cycles.",
            sparkline = listOf(19.4, 19.6, 19.8, 19.9, 20.1, 20.0, 20.2, 20.4),
            factors = FactorScores(value = 81, momentum = 76, macroTailwind = 80, volatilityShield = 81)
        ),
        MarketStock(
            symbol = "SAP.DE",
            companyName = "SAP SE",
            exchange = "XETRA",
            region = MarketRegion.EUROPE,
            currency = "EUR",
            sector = "Enterprise Software",
            livePriceUsd = 236.90,
            dailyChangePct = 1.04,
            marketCapLabel = "$278B",
            peRatio = 35.6,
            dividendYieldPct = 0.98,
            beta = 0.89,
            studentScore = 84,
            adultAlphaScore = 90,
            studentVerdict = "Europe's flagship cloud software winner; offers Eurozone exposure without industrial cyclicality.",
            adultVerdict = "DAX heavyweight decoupling from German manufacturing slump via sticky S/4HANA cloud backlog.",
            sparkline = listOf(222.0, 225.5, 228.0, 229.1, 231.4, 233.0, 235.2, 236.9),
            factors = FactorScores(value = 66, momentum = 90, macroTailwind = 84, volatilityShield = 83)
        ),
        MarketStock(
            symbol = "SIE.DE",
            companyName = "Siemens AG",
            exchange = "XETRA",
            region = MarketRegion.EUROPE,
            currency = "EUR",
            sector = "Industrial Automation",
            livePriceUsd = 198.10,
            dailyChangePct = -0.48,
            marketCapLabel = "$156B",
            peRatio = 17.8,
            dividendYieldPct = 2.65,
            beta = 1.12,
            studentScore = 79,
            adultAlphaScore = 83,
            studentVerdict = "Solid European dividend payer linked to smart factories and power grid modernization.",
            adultVerdict = "ECB easing cycle + grid electrification order book creates attractive DAX mean-reversion entry.",
            sparkline = listOf(201.0, 200.4, 199.8, 200.9, 199.5, 199.0, 198.7, 198.1),
            factors = FactorScores(value = 84, momentum = 68, macroTailwind = 77, volatilityShield = 76)
        ),
        MarketStock(
            symbol = "RELIANCE.BO",
            companyName = "Reliance Industries",
            exchange = "BSE",
            region = MarketRegion.INDIA,
            currency = "INR",
            sector = "Digital, Retail & Energy",
            livePriceUsd = 35.90,
            dailyChangePct = 0.96,
            marketCapLabel = "$242B",
            peRatio = 26.8,
            dividendYieldPct = 0.38,
            beta = 0.87,
            studentScore = 88,
            adultAlphaScore = 89,
            studentVerdict = "India's bellwether conglomerate spanning Jio telecom, retail & green energy for long-term SIPs.",
            adultVerdict = "BSE SENSEX anchor capturing India's domestic consumption & digital infrastructure compounding.",
            sparkline = listOf(34.1, 34.4, 34.8, 34.6, 35.1, 35.4, 35.6, 35.9),
            factors = FactorScores(value = 74, momentum = 82, macroTailwind = 91, volatilityShield = 85)
        ),
        MarketStock(
            symbol = "TCS.BO",
            companyName = "Tata Consultancy Services",
            exchange = "BSE",
            region = MarketRegion.INDIA,
            currency = "INR",
            sector = "IT Services & AI Consulting",
            livePriceUsd = 49.70,
            dailyChangePct = 0.42,
            marketCapLabel = "$180B",
            peRatio = 29.4,
            dividendYieldPct = 1.48,
            beta = 0.72,
            studentScore = 90,
            adultAlphaScore = 85,
            studentVerdict = "Low-beta Indian IT powerhouse with steady dividends—great stability anchor for students.",
            adultVerdict = "Benefits directly from US enterprise AI integration spend while earning in USD with INR cost base.",
            sparkline = listOf(48.2, 48.5, 48.7, 48.9, 49.1, 49.3, 49.5, 49.7),
            factors = FactorScores(value = 76, momentum = 77, macroTailwind = 86, volatilityShield = 91)
        )
    )

    val initialPredictiveAlerts: List<PredictiveTrendAlert> = listOf(
        PredictiveTrendAlert(
            id = "vol_us_tech",
            indexOrTicker = "NDX / NVDA",
            region = "US Markets",
            regime = "Elevated Momentum",
            volatilityRiskPct = 68,
            forecastDirection = "Bullish with +/- 2.8% event swings",
            summary = "AI semiconductor implied volatility pricing widens ahead of hyperscaler capex guidance.",
            suggestedAction = "Student: Stagger SIP entries. Adult: Trim overweight gamma or pair with MSFT."
        ),
        PredictiveTrendAlert(
            id = "vol_nikkei_fx",
            indexOrTicker = "N225 / 7203.T",
            region = "Japan (NIKKEI)",
            regime = "FX Divergence Watch",
            volatilityRiskPct = 61,
            forecastDirection = "Sideways-Up (+1.9% 30d bias)",
            summary = "USD/JPY rate differentials create cross-currents between Tokyo exporters and domestic banks.",
            suggestedAction = "Accumulate governance-reform exporters on JPY pullbacks."
        ),
        PredictiveTrendAlert(
            id = "vol_dax_ecb",
            indexOrTicker = "DAX / SAP.DE",
            region = "Germany (DAX)",
            regime = "Mean-Reversion Discount",
            volatilityRiskPct = 44,
            forecastDirection = "Gradual Recovery (+2.4% 30d bias)",
            summary = "DAX trades at a 43% P/E discount to S&P 500; ECB rate easing supports software & grid capex.",
            suggestedAction = "Rotate partial US gains into European quality compounders."
        ),
        PredictiveTrendAlert(
            id = "vol_bse_sip",
            indexOrTicker = "SENSEX / RELIANCE.BO",
            region = "India (BSE)",
            regime = "Low-Vol Compounding",
            volatilityRiskPct = 32,
            forecastDirection = "Steady Uptrend (+3.1% 30d bias)",
            summary = "Record domestic mutual fund inflows cushion global geopolitical shocks on BSE blue-chips.",
            suggestedAction = "Maintain core EM allocation in Indian digital & IT exporters."
        )
    )
}
