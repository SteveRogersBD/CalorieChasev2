package com.example.caloriechase;

import android.content.Context;
import android.content.SharedPreferences;

public class TrackingSession {
    
    private static final String PREFS_NAME = "TrackingSession";
    private static final String KEY_IS_ACTIVE = "is_active";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_TARGET_DISTANCE = "target_distance";
    private static final String KEY_START_STEPS = "start_steps";
    
    private Context context;
    private SharedPreferences prefs;
    
    public TrackingSession(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Start a new tracking session
     */
    public void startSession(float targetDistanceKm, int currentSteps) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_ACTIVE, true);
        editor.putLong(KEY_START_TIME, System.currentTimeMillis());
        editor.putFloat(KEY_TARGET_DISTANCE, targetDistanceKm);
        editor.putInt(KEY_START_STEPS, currentSteps);
        editor.apply();
    }
    
    /**
     * Stop the current tracking session
     */
    public void stopSession() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_ACTIVE, false);
        editor.apply();
    }
    
    /**
     * Check if a session is currently active
     */
    public boolean isSessionActive() {
        return prefs.getBoolean(KEY_IS_ACTIVE, false);
    }
    
    /**
     * Get session duration in milliseconds
     */
    public long getSessionDuration() {
        if (!isSessionActive()) return 0;
        
        long startTime = prefs.getLong(KEY_START_TIME, 0);
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Get target distance for current session
     */
    public float getTargetDistance() {
        return prefs.getFloat(KEY_TARGET_DISTANCE, 0.0f);
    }
    
    /**
     * Get steps taken during current session
     */
    public int getSessionSteps(int currentSteps) {
        if (!isSessionActive()) return 0;
        
        int startSteps = prefs.getInt(KEY_START_STEPS, 0);
        return Math.max(0, currentSteps - startSteps);
    }
    
    /**
     * Get session progress as percentage
     */
    public float getSessionProgress(float currentDistance) {
        float targetDistance = getTargetDistance();
        if (targetDistance <= 0) return 0;
        
        return Math.min(100, (currentDistance / targetDistance) * 100);
    }
}