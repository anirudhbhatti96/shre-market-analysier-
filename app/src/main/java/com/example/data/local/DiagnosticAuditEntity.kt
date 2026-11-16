package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_audits")
data class DiagnosticAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val scenarioExpression: String,
    val tierId: String,
    val weightedBeta: Double,
    val topConcentrationLabel: String,
    val anomalyCount: Int,
    val contextSummary: String,
    val aiVerdict: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
