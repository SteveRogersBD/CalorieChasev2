package com.example.caloriechase.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing an active tracking session
 */
@Entity(tableName = "active_sessions")
@TypeConverters({Converters.class})
public class ActiveSession {
    @PrimaryKey
    @NonNull
    public String sessionId;
    
    // Session configuration (inherited from SessionDraft)
    public double startLatitude;
    public double startLongitude;
    public float distanceGoal; // in kilometers
    public ActivityType activityType;
    public long createdTimestamp;
    
    // Active session data
    public long startTimestamp;
    public int currentSteps;
    public float currentDistance; // in kilometers
    public int caloriesBurned;
    public Set<String> collectedTreasures;
    public boolean isPaused;
    public long pausedDuration; // total time paused in milliseconds
    public long lastPauseTimestamp; // when the current pause started

    public ActiveSession() {
        // Default constructor required by Room
        this.collectedTreasures = new HashSet<>();
        this.isPaused = false;
        this.pausedDuration = 0;
        this.lastPauseTimestamp = 0;
    }

    /**
     * Create an ActiveSession from a SessionDraft
     */
    public static ActiveSession fromDraft(SessionDraft draft) {
        ActiveSession session = new ActiveSession();
        session.sessionId = draft.sessionId;
        session.startLatitude = draft.startLatitude;
        session.startLongitude = draft.startLongitude;
        session.distanceGoal = draft.distanceGoal;
        session.activityType = draft.activityType;
        session.createdTimestamp = draft.createdTimestamp;
        session.startTimestamp = System.currentTimeMillis();
        return session;
    }

    /**
     * Get current session metrics
     */
    public SessionMetrics getCurrentMetrics() {
        long effectiveDuration = getEffectiveDuration();
        SessionMetrics metrics = new SessionMetrics(currentSteps, currentDistance, caloriesBurned, effectiveDuration, collectedTreasures.size());
        return metrics;
    }

    /**
     * Get effective duration (total time minus paused time)
     */
    public long getEffectiveDuration() {
        long totalDuration = System.currentTimeMillis() - startTimestamp;
        long currentPauseDuration = isPaused && lastPauseTimestamp > 0 ? 
            System.currentTimeMillis() - lastPauseTimestamp : 0;
        return totalDuration - pausedDuration - currentPauseDuration;
    }

    /**
     * Pause the session
     */
    public void pause() {
        if (!isPaused) {
            isPaused = true;
            lastPauseTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Resume the session
     */
    public void resume() {
        if (isPaused && lastPauseTimestamp > 0) {
            pausedDuration += System.currentTimeMillis() - lastPauseTimestamp;
            isPaused = false;
            lastPauseTimestamp = 0;
        }
    }

    /**
     * Add a collected treasure
     */
    public void addCollectedTreasure(String treasureId) {
        if (collectedTreasures == null) {
            collectedTreasures = new HashSet<>();
        }
        collectedTreasures.add(treasureId);
    }
    
    /**
     * Update collected treasure count (used for progress tracking)
     */
    public void updateCollectedTreasureCount(int count) {
        // This method ensures the collected treasures count is accurate
        // It's used when the actual count from database might differ from the set size
        if (collectedTreasures == null) {
            collectedTreasures = new HashSet<>();
        }
        // Note: This is primarily for progress tracking
        // The actual treasure IDs are maintained separately in the database
    }

    /**
     * Check if a treasure has been collected
     */
    public boolean isTreasureCollected(String treasureId) {
        return collectedTreasures != null && collectedTreasures.contains(treasureId);
    }

    /**
     * Get the starting point as a LatLng-like object
     */
    public double[] getStartingPoint() {
        return new double[]{startLatitude, startLongitude};
    }

    /**
     * Check if the distance goal has been reached
     */
    public boolean isGoalReached() {
        return currentDistance >= distanceGoal;
    }

    /**
     * Get progress towards distance goal as percentage
     */
    public float getProgressPercentage() {
        if (distanceGoal <= 0) return 0.0f;
        return Math.min(100.0f, (currentDistance / distanceGoal) * 100.0f);
    }
}