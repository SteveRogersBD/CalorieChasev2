package com.example.caloriechase.location;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import com.example.caloriechase.data.TreasureHuntDatabase;
import com.example.caloriechase.data.TreasureDao;
import com.example.caloriechase.data.TreasureLocation;
import com.example.caloriechase.data.SessionManager;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages treasure collection system including animations, feedback, and fallback proximity checking
 */
public class TreasureCollectionManager {
    private static final String TAG = "TreasureCollectionManager";
    
    // Broadcast action constants
    public static final String ACTION_TREASURE_COLLECTED = "com.example.caloriechase.TREASURE_COLLECTED";
    public static final String ACTION_SESSION_PROGRESS_UPDATED = "com.example.caloriechase.SESSION_PROGRESS_UPDATED";
    public static final String ACTION_TREASURE_ANIMATION = "com.example.caloriechase.TREASURE_ANIMATION";
    
    // Intent extra constants
    public static final String EXTRA_TREASURE_ID = "treasure_id";
    public static final String EXTRA_TREASURE_TYPE = "treasure_type";
    public static final String EXTRA_TREASURE_LAT = "treasure_lat";
    public static final String EXTRA_TREASURE_LNG = "treasure_lng";
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_ANIMATION_TYPE = "animation_type";
    
    // Animation types
    public static final String ANIMATION_COLLECT = "collect";
    public static final String ANIMATION_PULSE = "pulse";
    public static final String ANIMATION_SPARKLE = "sparkle";
    
    // Proximity checking constants
    private static final long PROXIMITY_CHECK_INTERVAL = 5000; // 5 seconds
    private static final float PROXIMITY_BUFFER = 5.0f; // 5 meter buffer for proximity checking
    
    private final Context context;
    private final ExecutorService executor;
    private boolean isProximityCheckingEnabled = false;
    private long lastProximityCheck = 0;
    
    public TreasureCollectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Enable fallback proximity checking when geofences fail
     * @param enabled Whether to enable proximity checking
     */
    public void setProximityCheckingEnabled(boolean enabled) {
        this.isProximityCheckingEnabled = enabled;
        Log.d(TAG, "Proximity checking " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if proximity checking is enabled
     * @return True if proximity checking is enabled
     */
    public boolean isProximityCheckingEnabled() {
        return isProximityCheckingEnabled;
    }
    
    /**
     * Check for treasure collection using manual proximity checking
     * This is used as a fallback when geofences fail
     * @param currentLocation Current user location
     * @param sessionId Active session ID
     */
    public void checkProximityForTreasures(Location currentLocation, String sessionId) {
        if (!isProximityCheckingEnabled) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProximityCheck < PROXIMITY_CHECK_INTERVAL) {
            return; // Don't check too frequently
        }
        
        lastProximityCheck = currentTime;
        
        executor.execute(() -> {
            try {
                TreasureHuntDatabase database = TreasureHuntDatabase.getInstance(context);
                TreasureDao treasureDao = database.treasureDao();
                
                // Get uncollected treasures for the session
                List<TreasureLocation> uncollectedTreasures = treasureDao.getUncollectedTreasuresForSession(sessionId);
                
                for (TreasureLocation treasure : uncollectedTreasures) {
                    float distance = calculateDistance(
                        currentLocation.getLatitude(), 
                        currentLocation.getLongitude(),
                        treasure.latitude, 
                        treasure.longitude
                    );
                    
                    // Check if user is within treasure radius plus buffer
                    if (distance <= (treasure.radius + PROXIMITY_BUFFER)) {
                        Log.d(TAG, "Proximity treasure collection triggered for: " + treasure.treasureId + 
                              " (distance: " + distance + "m, radius: " + treasure.radius + "m)");
                        
                        // Collect the treasure
                        collectTreasure(treasure);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during proximity checking", e);
            }
        });
    }
    
    /**
     * Manually collect a treasure (used by both geofence and proximity checking)
     * @param treasure The treasure to collect
     */
    public void collectTreasure(TreasureLocation treasure) {
        executor.execute(() -> {
            try {
                if (treasure.isCollected) {
                    Log.d(TAG, "Treasure already collected: " + treasure.treasureId);
                    return;
                }
                
                TreasureHuntDatabase database = TreasureHuntDatabase.getInstance(context);
                TreasureDao treasureDao = database.treasureDao();
                SessionManager sessionManager = SessionManager.getInstance(context);
                
                // Mark treasure as collected
                treasure.markCollected();
                treasureDao.updateTreasure(treasure);
                
                Log.i(TAG, "Treasure collected: " + treasure.treasureId + " (type: " + treasure.type + ")");
                
                // Notify session manager
                sessionManager.onTreasureCollected(treasure);
                
                // Trigger collection animation and feedback
                triggerCollectionAnimation(treasure);
                
                // Update session progress
                sessionManager.updateSessionProgress();
                
            } catch (Exception e) {
                Log.e(TAG, "Error collecting treasure: " + treasure.treasureId, e);
            }
        });
    }
    
    /**
     * Trigger treasure collection animation and user feedback
     * @param treasure The collected treasure
     */
    private void triggerCollectionAnimation(TreasureLocation treasure) {
        try {
            // Send broadcast for treasure collection animation
            android.content.Intent animationIntent = new android.content.Intent(ACTION_TREASURE_ANIMATION);
            animationIntent.putExtra(EXTRA_TREASURE_ID, treasure.treasureId);
            animationIntent.putExtra(EXTRA_TREASURE_TYPE, treasure.type.name());
            animationIntent.putExtra(EXTRA_TREASURE_LAT, treasure.latitude);
            animationIntent.putExtra(EXTRA_TREASURE_LNG, treasure.longitude);
            animationIntent.putExtra(EXTRA_ANIMATION_TYPE, ANIMATION_COLLECT);
            
            context.sendBroadcast(animationIntent);
            
            // Send broadcast for general treasure collection
            android.content.Intent collectionIntent = new android.content.Intent(ACTION_TREASURE_COLLECTED);
            collectionIntent.putExtra(EXTRA_TREASURE_ID, treasure.treasureId);
            collectionIntent.putExtra(EXTRA_TREASURE_TYPE, treasure.type.name());
            collectionIntent.putExtra(EXTRA_TREASURE_LAT, treasure.latitude);
            collectionIntent.putExtra(EXTRA_TREASURE_LNG, treasure.longitude);
            
            context.sendBroadcast(collectionIntent);
            
            Log.d(TAG, "Collection animation triggered for treasure: " + treasure.treasureId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error triggering collection animation", e);
        }
    }
    
    /**
     * Trigger proximity animation for nearby treasures (visual feedback)
     * @param treasure The nearby treasure
     */
    public void triggerProximityAnimation(TreasureLocation treasure) {
        try {
            android.content.Intent animationIntent = new android.content.Intent(ACTION_TREASURE_ANIMATION);
            animationIntent.putExtra(EXTRA_TREASURE_ID, treasure.treasureId);
            animationIntent.putExtra(EXTRA_TREASURE_TYPE, treasure.type.name());
            animationIntent.putExtra(EXTRA_TREASURE_LAT, treasure.latitude);
            animationIntent.putExtra(EXTRA_TREASURE_LNG, treasure.longitude);
            animationIntent.putExtra(EXTRA_ANIMATION_TYPE, ANIMATION_PULSE);
            
            context.sendBroadcast(animationIntent);
            
            Log.d(TAG, "Proximity animation triggered for treasure: " + treasure.treasureId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error triggering proximity animation", e);
        }
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     * @param lat1 Latitude of first point
     * @param lng1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lng2 Longitude of second point
     * @return Distance in meters
     */
    private float calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371000; // Earth's radius in meters
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return (float) (R * c);
    }
    
    /**
     * Get treasures within a certain distance of a location
     * @param location Current location
     * @param sessionId Session ID
     * @param maxDistance Maximum distance in meters
     * @param callback Callback with nearby treasures
     */
    public void getNearbyTreasures(Location location, String sessionId, float maxDistance, NearbyTreasuresCallback callback) {
        executor.execute(() -> {
            try {
                TreasureHuntDatabase database = TreasureHuntDatabase.getInstance(context);
                TreasureDao treasureDao = database.treasureDao();
                
                List<TreasureLocation> allTreasures = treasureDao.getTreasuresForSession(sessionId);
                List<TreasureLocation> nearbyTreasures = new java.util.ArrayList<>();
                
                for (TreasureLocation treasure : allTreasures) {
                    float distance = calculateDistance(
                        location.getLatitude(), 
                        location.getLongitude(),
                        treasure.latitude, 
                        treasure.longitude
                    );
                    
                    if (distance <= maxDistance) {
                        nearbyTreasures.add(treasure);
                    }
                }
                
                callback.onNearbyTreasures(nearbyTreasures);
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting nearby treasures", e);
                callback.onError("Failed to get nearby treasures: " + e.getMessage());
            }
        });
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
    
    /**
     * Callback interface for nearby treasures
     */
    public interface NearbyTreasuresCallback {
        void onNearbyTreasures(List<TreasureLocation> treasures);
        void onError(String error);
    }
}