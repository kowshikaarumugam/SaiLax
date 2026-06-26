package com.example.data.dao;

import androidx.room.*;
import com.example.data.model.ProofLog;
import kotlinx.coroutines.flow.Flow;
import java.util.List;

@Dao
public interface ProofLogDao {
    @Query("SELECT * FROM proof_logs ORDER BY timestamp DESC")
    Flow<List<ProofLog>> getAllLogs();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertLog(ProofLog log);

    @Query("DELETE FROM proof_logs")
    void clearLogs();
}
