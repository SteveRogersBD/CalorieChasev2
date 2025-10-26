package com.example.caloriechase.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Room database for treasure hunt session data
 */
@Database(
    entities = {
        SessionDraft.class,
        ActiveSession.class,
        SessionRecord.class,
        TreasureLocation.class,
        DailyStats.class
    },
    version = 3,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class TreasureHuntDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "treasure_hunt_database";
    private static volatile TreasureHuntDatabase INSTANCE;
    
    public abstract SessionDao sessionDao();
    public abstract TreasureDao treasureDao();
    public abstract DailyStatsDao dailyStatsDao();
    
    /**
     * Get database instance using singleton pattern
     */
    public static TreasureHuntDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TreasureHuntDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        TreasureHuntDatabase.class,
                        DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration() // For development - remove in production
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Close database instance (for testing purposes)
     */
    public static void closeInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}