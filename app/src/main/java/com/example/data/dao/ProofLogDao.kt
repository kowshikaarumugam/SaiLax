package com.example.data.dao

import androidx.room.*
import com.example.data.model.ProofLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ProofLogDao {
    @Query("SELECT * FROM proof_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ProofLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ProofLog): Long

    @Query("DELETE FROM proof_logs")
    suspend fun clearLogs()
}
