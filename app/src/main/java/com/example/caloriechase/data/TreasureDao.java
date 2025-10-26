package com.example.caloriechase.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * Data Access Object for treasure-related database operations
 */
@Dao
public interface TreasureDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTreasure(TreasureLocation treasure);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTreasures(List<TreasureLocation> treasures);
    
    @Update
    void updateTreasure(TreasureLocation treasure);
    
    @Delete
    void deleteTreasure(TreasureLocation treasure);
    
    @Query("SELECT * FROM treasure_locations WHERE treasureId = :treasureId")
    TreasureLocation getTreasure(String treasureId);
    
    @Query("SELECT * FROM treasure_locations WHERE treasureId = :treasureId")
    TreasureLocation getTreasureById(String treasureId);
    
    @Query("SELECT * FROM treasure_locations WHERE sessionId = :sessionId")
    List<TreasureLocation> getTreasuresForSession(String sessionId);
    
    @Query("SELECT * FROM treasure_locations WHERE sessionId = :sessionId AND isCollected = 0")
    List<TreasureLocation> getUncollectedTreasuresForSession(String sessionId);
    
    @Query("SELECT * FROM treasure_locations WHERE sessionId = :sessionId AND isCollected = 1")
    List<TreasureLocation> getCollectedTreasuresForSession(String sessionId);
    
    @Query("SELECT COUNT(*) FROM treasure_locations WHERE sessionId = :sessionId")
    int getTreasureCountForSession(String sessionId);
    
    @Query("SELECT COUNT(*) FROM treasure_locations WHERE sessionId = :sessionId AND isCollected = 1")
    int getCollectedTreasureCountForSession(String sessionId);
    
    @Query("UPDATE treasure_locations SET isCollected = 1, collectionTimestamp = :timestamp WHERE treasureId = :treasureId")
    void markTreasureCollected(String treasureId, long timestamp);
    
    @Query("DELETE FROM treasure_locations WHERE sessionId = :sessionId")
    void deleteTreasuresForSession(String sessionId);
    
    @Query("DELETE FROM treasure_locations WHERE treasureId = :treasureId")
    void deleteTreasureById(String treasureId);
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM treasure_locations WHERE isCollected = 1")
    int getTotalCollectedTreasures();
    
    @Query("SELECT COUNT(*) FROM treasure_locations")
    int getTotalTreasures();
    
    @Query("SELECT type, COUNT(*) as count FROM treasure_locations WHERE isCollected = 1 GROUP BY type")
    List<TreasureTypeCount> getCollectedTreasuresByType();
    
    // Helper class for treasure type statistics
    class TreasureTypeCount {
        public TreasureType type;
        public int count;
    }
}