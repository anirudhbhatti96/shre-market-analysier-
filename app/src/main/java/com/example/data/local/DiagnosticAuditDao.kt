package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticAuditDao {
    @Query("SELECT * FROM diagnostic_audits WHERE userId = :userId ORDER BY createdAtMillis DESC LIMIT 25")
    fun observeAuditsForUser(userId: String): Flow<List<DiagnosticAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: DiagnosticAuditEntity): Long

    @Query("DELETE FROM diagnostic_audits WHERE id = :id")
    suspend fun deleteAuditById(id: Long)

    @Query("DELETE FROM diagnostic_audits WHERE userId = :userId")
    suspend fun clearAuditsForUser(userId: String)
}
