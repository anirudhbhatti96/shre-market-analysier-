package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DiagnosticAuditEntity::class,
        UserAdvisoryProfileEntity::class,
        PortfolioHoldingEntity::class,
        PriceAlertEntity::class,
        AiNewsReportEntity::class,
        WatchlistItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FinAdvisorLocalDatabase : RoomDatabase() {
    abstract fun diagnosticAuditDao(): DiagnosticAuditDao
    abstract fun finAdvisorDao(): FinAdvisorDao

    companion object {
        @Volatile
        private var INSTANCE: FinAdvisorLocalDatabase? = null

        fun getInstance(context: Context): FinAdvisorLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinAdvisorLocalDatabase::class.java,
                    "finadvisor_local_diagnostics.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
