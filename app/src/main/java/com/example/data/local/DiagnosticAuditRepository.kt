package com.example.data.local

import kotlinx.coroutines.flow.Flow

class DiagnosticAuditRepository(private val dao: DiagnosticAuditDao) {
    fun observeUserAudits(userId: String): Flow<List<DiagnosticAuditEntity>> =
        dao.observeAuditsForUser(userId)

    suspend fun recordAudit(audit: DiagnosticAuditEntity): Long =
        dao.insertAudit(audit)

    suspend fun deleteAudit(id: Long) =
        dao.deleteAuditById(id)

    suspend fun clearUserAudits(userId: String) =
        dao.clearAuditsForUser(userId)
}
