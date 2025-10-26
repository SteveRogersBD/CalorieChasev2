package com.example.caloriechase.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a completed session record
 */
@Entity(tableName = "session_records")
@TypeConverters({Converters.class})
public class SessionRecord {
    @PrimaryKey
    @NonNull
    public String sessionId;
    
    // Session configuration (inherited from SessionDraft)
    public double startLatitude;
    public double startLongitude;
    public float distanceGoal; // in kilometers
    public ActivityType activityType;
    public long createdTimestamp;
    
    // Session tracking data (inherited from ActiveSession)
    public long startTimestamp;
    public int currentSteps;
    public float currentDistance; // in kilometers
    public int caloriesBurned;
    public Set<String> collectedTreasures;
    public long pausedDuration; // total time paused in milliseconds
    
    // Completion data
    public long endTimestamp;
    public long totalDuration; // effective duration (excluding paused time)
    public float averagePace; // in minutes per kilometer
    public int totalTreasures; // total treasures that were available
    public List<LocationUpdate> routePoints; // GPS track of the session

    public SessionRecord() {
        // Default constructor required by Room
        this.collectedTreasures = new HashSet<>();
        this.routePoints = new ArrayList<>();
    }

    /**
     * Create a SessionRecord from an ActiveSession
     */
    public static SessionRecord fromActiveSession(ActiveSession activeSession, int totalTreasuresAvailable) {
        SessionRecord record = new SessionRecord();
        
        // Copy session configuration
        record.sessionId = activeSession.sessionId;
        record.startLatitude = activeSession.startLatitude;
        record.startLongitude = activeSession.startLongitude;
        record.distanceGoal = activeSession.distanceGoal;
        record.activityType = activeSession.activityType;
        record.createdTimestamp = activeSession.createdTimestamp;
        
        // Copy tracking data
        record.startTimestamp = activeSession.startTimestamp;
        record.currentSteps = activeSession.currentSteps;
        record.currentDistance = activeSession.currentDistance;
        record.caloriesBurned = activeSession.caloriesBurned;
        record.collectedTreasures = new HashSet<>(activeSession.collectedTreasures);
        record.pausedDuration = activeSession.pausedDuration;
        
        // Set completion data
        record.endTimestamp = System.currentTimeMillis();
        record.totalDuration = activeSession.getEffectiveDuration();
        record.totalTreasures = totalTreasuresAvailable;
        record.averagePace = record.calculateAveragePace();
        
        return record;
    }

    /**
     * Calculate average pace in minutes per kilometer
     */
    public float calculateAveragePace() {
        if (currentDistance <= 0) return 0.0f;
        return (totalDuration / 60000.0f) / currentDistance; // Convert milliseconds to minutes, divide by distance
    }

    /**
     * Get session metrics for this completed session
     */
    public SessionMetrics getSessionMetrics() {
        return new SessionMetrics(currentSteps, currentDistance, caloriesBurned, totalDuration, collectedTreasures.size());
    }

    /**
     * Get treasure collection rate as percentage
     */
    public float getTreasureCollectionRate() {
        if (totalTreasures <= 0) return 0.0f;
        return ((float) collectedTreasures.size() / totalTreasures) * 100.0f;
    }

    /**
     * Check if the distance goal was achieved
     */
    public boolean wasGoalAchieved() {
        return currentDistance >= distanceGoal;
    }

    /**
     * Get the starting point as a LatLng-like object
     */
    public double[] getStartingPoint() {
        return new double[]{startLatitude, startLongitude};
    }

    /**
     * Add a route point to track the path taken
     */
    public void addRoutePoint(LocationUpdate locationUpdate) {
        if (routePoints == null) {
            routePoints = new ArrayList<>();
        }
        routePoints.add(locationUpdate);
    }

    /**
     * Get formatted duration (HH:MM:SS)
     */
    public String getFormattedDuration() {
        long seconds = totalDuration / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }

    /**
     * Get formatted pace (MM:SS per km)
     */
    public String getFormattedPace() {
        if (averagePace <= 0) return "00:00";
        
        int minutes = (int) averagePace;
        int seconds = (int) ((averagePace - minutes) * 60);
        return String.format("%02d:%02d", minutes, seconds);
    }
}