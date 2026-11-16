package com.example.data.diagnostics

enum class DiagnosticStatus(val label: String) {
    PASS("PASS"),
    DRIFT("DRIFT"),
    ANOMALY("ANOMALY")
}

enum class DiagnosticActionType {
    CREATE_HEDGE_ALERT,
    EXPLORE_DIVERGENT_COMPARE,
    OPEN_AI_PICKS,
    REBALANCE_PORTFOLIO
}

data class ScenarioPreset(
    val id: String,
    val label: String,
    val expression: String,
    val description: String
)

data class ExecutionTraceStep(
    val stepId: String,
    val traceOrigin: String,
    val detail: String,
    val status: DiagnosticStatus
)

data class DiagnosticMetricRow(
    val metric: String,
    val result: String,
    val status: DiagnosticStatus
)

data class DiagnosticFinding(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val actionLabel: String,
    val actionType: DiagnosticActionType,
    val targetSymbol: String? = null,
    val targetExchange: String? = null,
    val recommendedPct: Double = 2.5
)

data class DiagnosticReportSnapshot(
    val scenarioExpression: String,
    val contextSummary: String,
    val diagnosticsBullets: List<String>,
    val traceSteps: List<ExecutionTraceStep>,
    val metricsTable: List<DiagnosticMetricRow>,
    val actionableFindings: List<DiagnosticFinding>,
    val weightedBeta: Double,
    val topConcentrationLabel: String,
    val anomalyCount: Int,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class AiDiagnosticVerdict(
    val contextAnalysis: String,
    val catalystOrigin: String,
    val executionRiskSummary: String,
    val remediationSteps: List<String>,
    val recommendedShareToInspect: String
)
