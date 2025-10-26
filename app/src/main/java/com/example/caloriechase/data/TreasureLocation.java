package com.example.caloriechase.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * Entity representing a treasure location in the treasure hunt system
 */
@Entity(tableName = "treasure_locations")
@TypeConverters({Converters.class})
public class TreasureLocation {
    @PrimaryKey
    @NonNull
    public String treasureId;
    
    public String sessionId; // Foreign key to session
    public double latitude;
    public double longitude;
    public float radius; // Geofence radius in meters
    public TreasureType type;
    public boolean isCollected;
    public long collectionTimestamp;

    public TreasureLocation() {
        // Default constructor required by Room
    }

    @Ignore
    public TreasureLocation(String treasureId, String sessionId, double latitude, double longitude, TreasureType type) {
        this.treasureId = treasureId;
        this.sessionId = sessionId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.radius = type.getRadiusMeters();
        this.isCollected = false;
        this.collectionTimestamp = 0;
    }

    /**
     * Mark this treasure as collected
     */
    public void markCollected() {
        this.isCollected = true;
        this.collectionTimestamp = System.currentTimeMillis();
    }

    /**
     * Get the position as a LatLng-like object
     */
    public double[] getPosition() {
        return new double[]{latitude, longitude};
    }
}