package com.example.caloriechase.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO for daily statistics
 */
@Dao
public interface DailyStatsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailyStats dailyStats);
    
    @Update
    void update(DailyStats dailyStats);
    
    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    DailyStats getStatsByDate(String date);
    
    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT :limit")
    List<DailyStats> getLastNDays(int limit);
    
    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    List<DailyStats> getAllStats();
    
    @Query("DELETE FROM daily_stats WHERE date < :date")
    void deleteOlderThan(String date);
}
