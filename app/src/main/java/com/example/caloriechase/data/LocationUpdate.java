package com.example.caloriechase.data;

/**
 * Data class representing a location update during tracking
 */
public class LocationUpdate {
    public double latitude;
    public double longitude;
    public float accuracy;
    public long timestamp;
    public float speed;
    public float bearing;

    public LocationUpdate() {
        // Default constructor
    }

    public LocationUpdate(double latitude, double longitude, float accuracy, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
        this.speed = 0.0f;
        this.bearing = 0.0f;
    }

    public LocationUpdate(double latitude, double longitude, float accuracy, long timestamp, float speed, float bearing) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
        this.speed = speed;
        this.bearing = bearing;
    }

    /**
     * Get the position as a LatLng-like object
     */
    public double[] getPosition() {
        return new double[]{latitude, longitude};
    }

    /**
     * Check if this location update has valid GPS data
     */
    public boolean isValid() {
        return accuracy > 0 && accuracy < 100; // Consider locations with accuracy better than 100m as valid
    }
}