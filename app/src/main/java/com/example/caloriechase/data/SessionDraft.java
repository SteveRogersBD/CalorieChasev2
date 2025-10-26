package com.example.caloriechase.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a draft session configuration before it becomes active
 */
@Entity(tableName = "session_drafts")
@TypeConverters({Converters.class})
public class SessionDraft {
    @PrimaryKey
    @NonNull
    public String sessionId;
    
    public double startLatitude;
    public double startLongitude;
    public float distanceGoal; // in kilometers
    public ActivityType activityType;
    public long createdTimestamp;

    public SessionDraft() {
        // Default constructor required by Room
        this.sessionId = UUID.randomUUID().toString();
        this.createdTimestamp = System.currentTimeMillis();
        this.activityType = ActivityType.WALK;
    }

    @Ignore
    public SessionDraft(double startLatitude, double startLongitude, float distanceGoal, ActivityType activityType) {
        this();
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.distanceGoal = distanceGoal;
        this.activityType = activityType;
    }

    /**
     * Get the starting point as a LatLng-like object
     */
    public double[] getStartingPoint() {
        return new double[]{startLatitude, startLongitude};
    }

    /**
     * Set the starting point from a LatLng-like object
     */
    public void setStartingPoint(double latitude, double longitude) {
        this.startLatitude = latitude;
        this.startLongitude = longitude;
    }

    /**
     * Validate that the session draft has all required fields
     */
    public boolean isValid() {
        return sessionId != null && 
               !sessionId.isEmpty() &&
               distanceGoal > 0 &&
               distanceGoal <= 5.0f && // Max 5km as per requirements
               activityType != null;
    }
}