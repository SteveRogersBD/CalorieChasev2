package com.example.caloriechase.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing daily fitness statistics
 */
@Entity(tableName = "daily_stats", indices = {@Index(value = "date", unique = true)})
public class DailyStats {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @NonNull
    private String date; // Format: yyyy-MM-dd
    private int steps;
    private float distance; // in kilometers
    private int calories;
    
    public DailyStats(String date, int steps, float distance, int calories) {
        this.date = date;
        this.steps = steps;
        this.distance = distance;
        this.calories = calories;
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public int getSteps() {
        return steps;
    }
    
    public void setSteps(int steps) {
        this.steps = steps;
    }
    
    public float getDistance() {
        return distance;
    }
    
    public void setDistance(float distance) {
        this.distance = distance;
    }
    
    public int getCalories() {
        return calories;
    }
    
    public void setCalories(int calories) {
        this.calories = calories;
    }
}
