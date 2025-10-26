package com.example.caloriechase.utils;

import android.content.Context;

import com.example.caloriechase.data.DailyStats;
import com.example.caloriechase.data.TreasureHuntDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class for managing daily statistics
 */
public class DailyStatsHelper {
    
    /**
     * Generate sample data for the last 7 days (for testing/demo purposes)
     */
    public static void generateSampleData(Context context) {
        TreasureHuntDatabase database = TreasureHuntDatabase.getInstance(context);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        executor.execute(() -> {
            Random random = new Random();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            
            for (int i = 6; i >= 0; i--) {
                calendar.add(Calendar.DAY_OF_YEAR, -i);
                String date = dateFormat.format(calendar.getTime());
                
                // Generate random but realistic data
                int steps = 3000 + random.nextInt(7000); // 3000-10000 steps
                float distance = steps * 0.0008f; // ~0.8m per step
                int calories = (int) (steps * 0.04f); // ~0.04 cal per step
                
                DailyStats stats = new DailyStats(date, steps, distance, calories);
                database.dailyStatsDao().insert(stats);
                
                // Reset calendar for next iteration
                calendar = Calendar.getInstance();
            }
            
            executor.shutdown();
        });
    }
    
    /**
     * Update today's stats in the database
     */
    public static void updateTodayStats(Context context, int steps, float distance, int calories) {
        TreasureHuntDatabase database = TreasureHuntDatabase.getInstance(context);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        executor.execute(() -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dateFormat.format(Calendar.getInstance().getTime());
            
            DailyStats stats = new DailyStats(today, steps, distance, calories);
            database.dailyStatsDao().insert(stats);
            
            executor.shutdown();
        });
    }
}
