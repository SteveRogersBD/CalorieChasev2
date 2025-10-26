package com.example.caloriechase.data;

/**
 * Data class representing current session metrics
 */
public class SessionMetrics {
    public final int steps;
    public final float distance; // in kilometers
    public final int calories;
    public final long duration; // in milliseconds
    public final int treasuresCollected;
    
    public SessionMetrics(int steps, float distance, int calories, long duration, int treasuresCollected) {
        this.steps = steps;
        this.distance = distance;
        this.calories = calories;
        this.duration = duration;
        this.treasuresCollected = treasuresCollected;
    }
    
    /**
     * Get formatted duration string (HH:MM:SS)
     */
    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * Get average pace in minutes per kilometer
     */
    public String getAveragePace() {
        if (distance <= 0 || duration <= 0) {
            return "0:00 /km";
        }
        
        double durationMinutes = duration / (1000.0 * 60.0);
        double paceMinutes = durationMinutes / distance;
        
        int minutes = (int) paceMinutes;
        int seconds = (int) ((paceMinutes - minutes) * 60);
        
        return String.format("%d:%02d /km", minutes, seconds);
    }
    
    /**
     * Get current speed in km/h
     */
    public double getCurrentSpeed() {
        if (distance <= 0 || duration <= 0) {
            return 0.0;
        }
        
        double durationHours = duration / (1000.0 * 60.0 * 60.0);
        return distance / durationHours;
    }
    
    /**
     * Get formatted distance string
     */
    public String getFormattedDistance() {
        if (distance < 1.0f) {
            return String.format("%.0f m", distance * 1000);
        } else {
            return String.format("%.2f km", distance);
        }
    }
    
    /**
     * Get steps per minute
     */
    public int getStepsPerMinute() {
        if (duration <= 0) {
            return 0;
        }
        
        double durationMinutes = duration / (1000.0 * 60.0);
        return (int) (steps / durationMinutes);
    }
    
    @Override
    public String toString() {
        return String.format("SessionMetrics{steps=%d, distance=%.2f km, calories=%d, duration=%s, treasures=%d}",
                steps, distance, calories, getFormattedDuration(), treasuresCollected);
    }
}