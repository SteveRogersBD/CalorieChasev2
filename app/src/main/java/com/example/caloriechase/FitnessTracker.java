package com.example.caloriechase;

import android.content.Context;
import android.content.SharedPreferences;

public class FitnessTracker {
    
    private static final String PREFS_NAME = "FitnessTracker";
    
    // MET (Metabolic Equivalent of Task) values for different activities
    private static final double WALKING_SLOW_MET = 2.5;    // < 3.2 km/h
    private static final double WALKING_NORMAL_MET = 3.5;   // 3.2-5.1 km/h
    private static final double WALKING_FAST_MET = 4.3;     // 5.1-6.4 km/h
    private static final double RUNNING_SLOW_MET = 6.0;     // 6.4-8.0 km/h
    private static final double RUNNING_NORMAL_MET = 8.3;   // 8.0-9.7 km/h
    private static final double RUNNING_FAST_MET = 11.0;    // > 9.7 km/h
    
    /**
     * Calculate calories burned based on MET formula
     * Calories = MET × weight(kg) × time(hours)
     * Enhanced with UserDataManager integration for personalized calculations
     */
    public static int calculateCalories(Context context, double durationMinutes, double averageSpeedKmh) {
        UserDataManager.UserData userData = UserDataManager.getUserData(context);
        
        float weight = userData.weight;
        if (userData.isImperial) {
            weight = weight * 0.453592f; // Convert lb to kg
        }
        
        // Use BMR for more accurate calorie calculation
        double bmr = UserDataManager.calculateBMR(userData);
        double met = getMETValue(averageSpeedKmh);
        double durationHours = durationMinutes / 60.0;
        
        // Enhanced formula: (MET × weight × time) + (BMR adjustment)
        double baseCalories = met * weight * durationHours;
        double bmrAdjustment = (bmr / 24.0) * durationHours * 0.1; // 10% BMR adjustment
        
        return (int) Math.round(baseCalories + bmrAdjustment);
    }
    
    /**
     * Calculate calories from steps (enhanced with UserDataManager integration)
     */
    public static int calculateCaloriesFromSteps(Context context, int steps) {
        UserDataManager.UserData userData = UserDataManager.getUserData(context);
        
        float weight = userData.weight;
        if (userData.isImperial) {
            weight = weight * 0.453592f; // Convert lb to kg
        }
        
        // Enhanced calculation based on user data
        // Base calories per step varies by weight and gender
        float baseCaloriesPerStep = 0.04f;
        
        // Adjust for gender (men typically burn slightly more calories)
        if ("Male".equals(userData.sex)) {
            baseCaloriesPerStep *= 1.1f;
        }
        
        // Adjust for age (metabolism slows with age)
        if (userData.age > 30) {
            float ageAdjustment = 1.0f - ((userData.age - 30) * 0.005f); // 0.5% reduction per year after 30
            baseCaloriesPerStep *= Math.max(ageAdjustment, 0.8f); // Minimum 80% of base rate
        }
        
        return Math.round(steps * baseCaloriesPerStep * weight);
    }
    
    /**
     * Estimate distance from steps (enhanced with UserDataManager integration)
     */
    public static float calculateDistanceFromSteps(Context context, int steps) {
        UserDataManager.UserData userData = UserDataManager.getUserData(context);
        
        // Average step length based on height and gender
        float height = userData.height;
        if (userData.isImperial) {
            height = height * 2.54f; // Convert inches to cm
        }
        
        // Enhanced step length calculation
        // Base step length ≈ height × 0.43 (in cm)
        float stepLengthCm = height * 0.43f;
        
        // Adjust for gender (men typically have slightly longer strides)
        if ("Male".equals(userData.sex)) {
            stepLengthCm *= 1.05f; // 5% longer stride
        }
        
        // Adjust for age (stride may shorten with age)
        if (userData.age > 60) {
            float ageAdjustment = 1.0f - ((userData.age - 60) * 0.01f); // 1% reduction per year after 60
            stepLengthCm *= Math.max(ageAdjustment, 0.85f); // Minimum 85% of base stride
        }
        
        float stepLengthKm = stepLengthCm / 100000f; // Convert to km
        
        return steps * stepLengthKm;
    }
    
    /**
     * Calculate average pace (minutes per km)
     */
    public static String calculatePace(float distanceKm, long durationMinutes) {
        if (distanceKm <= 0) return "0:00 /km";
        
        double paceMinutes = durationMinutes / distanceKm;
        int minutes = (int) paceMinutes;
        int seconds = (int) ((paceMinutes - minutes) * 60);
        
        return String.format("%d:%02d /km", minutes, seconds);
    }
    
    /**
     * Format duration from milliseconds to HH:MM:SS
     */
    public static String formatDuration(long durationMillis) {
        long seconds = durationMillis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * Get MET value based on speed
     */
    private static double getMETValue(double speedKmh) {
        if (speedKmh < 3.2) return WALKING_SLOW_MET;
        else if (speedKmh < 5.1) return WALKING_NORMAL_MET;
        else if (speedKmh < 6.4) return WALKING_FAST_MET;
        else if (speedKmh < 8.0) return RUNNING_SLOW_MET;
        else if (speedKmh < 9.7) return RUNNING_NORMAL_MET;
        else return RUNNING_FAST_MET;
    }
    
    /**
     * Save daily progress
     */
    public static void saveDailyProgress(Context context, int steps, float distance, int calories, int treasures) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putInt("daily_steps", steps);
        editor.putFloat("daily_distance", distance);
        editor.putInt("daily_calories", calories);
        editor.putInt("daily_treasures", treasures);
        editor.putLong("last_update", System.currentTimeMillis());
        
        editor.apply();
    }
    
    /**
     * Load daily progress
     */
    public static DailyProgress loadDailyProgress(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        return new DailyProgress(
            prefs.getInt("daily_steps", 0),
            prefs.getFloat("daily_distance", 0.0f),
            prefs.getInt("daily_calories", 0),
            prefs.getInt("daily_treasures", 0),
            prefs.getLong("last_update", 0)
        );
    }
    
    /**
     * Update daily progress by adding session data
     * Used when a treasure hunt session is completed
     */
    public static void updateDailyProgressFromSession(Context context, int sessionSteps, 
                                                     float sessionDistance, int sessionCalories, 
                                                     int sessionTreasures) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Add session data to existing daily totals
        int currentSteps = prefs.getInt("daily_steps", 0);
        float currentDistance = prefs.getFloat("daily_distance", 0.0f);
        int currentCalories = prefs.getInt("daily_calories", 0);
        int currentTreasures = prefs.getInt("daily_treasures", 0);
        
        editor.putInt("daily_steps", currentSteps + sessionSteps);
        editor.putFloat("daily_distance", currentDistance + sessionDistance);
        editor.putInt("daily_calories", currentCalories + sessionCalories);
        editor.putInt("daily_treasures", currentTreasures + sessionTreasures);
        editor.putLong("last_update", System.currentTimeMillis());
        
        editor.apply();
    }
    
    /**
     * Data class for daily progress
     */
    public static class DailyProgress {
        public final int steps;
        public final float distance;
        public final int calories;
        public final int treasures;
        public final long lastUpdate;
        
        public DailyProgress(int steps, float distance, int calories, int treasures, long lastUpdate) {
            this.steps = steps;
            this.distance = distance;
            this.calories = calories;
            this.treasures = treasures;
            this.lastUpdate = lastUpdate;
        }
    }
}