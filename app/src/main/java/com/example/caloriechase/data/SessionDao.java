package com.example.caloriechase.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * Data Access Object for session-related database operations
 */
@Dao
public interface SessionDao {
    
    // SessionDraft operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSessionDraft(SessionDraft sessionDraft);
    
    @Update
    void updateSessionDraft(SessionDraft sessionDraft);
    
    @Delete
    void deleteSessionDraft(SessionDraft sessionDraft);
    
    @Query("SELECT * FROM session_drafts WHERE sessionId = :sessionId")
    SessionDraft getSessionDraft(String sessionId);
    
    @Query("SELECT * FROM session_drafts ORDER BY createdTimestamp DESC")
    List<SessionDraft> getAllSessionDrafts();
    
    @Query("DELETE FROM session_drafts WHERE sessionId = :sessionId")
    void deleteSessionDraftById(String sessionId);
    
    // ActiveSession operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertActiveSession(ActiveSession activeSession);
    
    @Update
    void updateActiveSession(ActiveSession activeSession);
    
    @Delete
    void deleteActiveSession(ActiveSession activeSession);
    
    @Query("SELECT * FROM active_sessions WHERE sessionId = :sessionId")
    ActiveSession getActiveSession(String sessionId);
    
    @Query("SELECT * FROM active_sessions ORDER BY startTimestamp DESC LIMIT 1")
    ActiveSession getCurrentActiveSession();
    
    @Query("SELECT * FROM active_sessions ORDER BY startTimestamp DESC")
    List<ActiveSession> getAllActiveSessions();
    
    @Query("SELECT COUNT(*) FROM active_sessions")
    int getActiveSessionCount();
    
    @Query("DELETE FROM active_sessions WHERE sessionId = :sessionId")
    void deleteActiveSessionById(String sessionId);
    
    // SessionRecord operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSessionRecord(SessionRecord sessionRecord);
    
    @Update
    void updateSessionRecord(SessionRecord sessionRecord);
    
    @Delete
    void deleteSessionRecord(SessionRecord sessionRecord);
    
    @Query("SELECT * FROM session_records WHERE sessionId = :sessionId")
    SessionRecord getSessionRecord(String sessionId);
    
    @Query("SELECT * FROM session_records ORDER BY endTimestamp DESC")
    List<SessionRecord> getAllSessionRecords();
    
    @Query("SELECT * FROM session_records ORDER BY endTimestamp DESC LIMIT :limit")
    List<SessionRecord> getRecentSessionRecords(int limit);
    
    @Query("SELECT * FROM session_records WHERE endTimestamp >= :startTime AND endTimestamp <= :endTime ORDER BY endTimestamp DESC")
    List<SessionRecord> getSessionRecordsByDateRange(long startTime, long endTime);
    
    @Query("DELETE FROM session_records WHERE sessionId = :sessionId")
    void deleteSessionRecordById(String sessionId);
    
    // Utility queries
    @Query("SELECT COUNT(*) FROM session_records")
    int getTotalSessionCount();
    
    @Query("SELECT SUM(currentDistance) FROM session_records")
    float getTotalDistanceTraveled();
    
    @Query("SELECT SUM(currentSteps) FROM session_records")
    int getTotalStepsTaken();
    
    @Query("SELECT SUM(caloriesBurned) FROM session_records")
    int getTotalCaloriesBurned();
}