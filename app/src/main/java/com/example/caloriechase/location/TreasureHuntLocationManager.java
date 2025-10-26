package com.example.caloriechase.location;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import com.example.caloriechase.data.TreasureLocation;
import com.example.caloriechase.data.SessionManager;
import java.util.List;

/**
 * High-level manager that coordinates geofencing and treasure collection
 * This class provides a simple interface for activities to use the treasure collection system
 */
public class TreasureHuntLocationManager {
    private static final String TAG = "TreasureHuntLocationManager";
    
    private final Context context;
    private final GeofenceManager geofenceManager;
    private final TreasureCollectionManager treasureCollectionManager;
    private final SessionManager sessionManager;
    
    public TreasureHuntLocationManager(Context context) {
        this.context = context.getApplicationContext();
        this.geofenceManager = new GeofenceManager(this.context);
        this.treasureCollectionManager = new TreasureCollectionManager(this.context);
        this.sessionManager = SessionManager.getInstance(this.context);
    }
    
    /**
     * Start treasure hunt tracking for a session
     * @param treasures List of treasures to track
     * @param sessionId Session ID
     * @param callback Callback for success/failure
     */
    public void startTreasureHunt(List<TreasureLocation> treasures, String sessionId, TreasureHuntCallback callback) {
        Log.d(TAG, "Starting treasure hunt for session: " + sessionId + " with " + treasures.size() + " treasures");
        
        // Set up geofences first
        geofenceManager.setupGeofences(treasures, sessionId, new GeofenceManager.GeofenceCallback() {
            @Override
            public void onSuccess(int geofenceCount) {
                Log.d(TAG, "Geofences set up successfully: " + geofenceCount);
                callback.onSuccess("Treasure hunt started with " + geofenceCount + " geofences");
            }
            
            @Override
            public void onFailure(String error) {
                Log.w(TAG, "Geofence setup failed, enabling proximity checking: " + error);
                // Enable fallback proximity checking when geofences fail
                treasureCollectionManager.setProximityCheckingEnabled(true);
                callback.onSuccess("Treasure hunt started with proximity checking (geofences unavailable)");
            }
        });
    }
    
    /**
     * Stop treasure hunt tracking for a session
     * @param sessionId Session ID
     * @param callback Callback for success/failure
     */
    public void stopTreasureHunt(String sessionId, TreasureHuntCallback callback) {
        Log.d(TAG, "Stopping treasure hunt for session: " + sessionId);
        
        // Remove geofences
        geofenceManager.removeGeofences(sessionId, new GeofenceManager.GeofenceCallback() {
            @Override
            public void onSuccess(int geofenceCount) {
                Log.d(TAG, "Geofences removed successfully");
                // Disable proximity checking
                treasureCollectionManager.setProximityCheckingEnabled(false);
                callback.onSuccess("Treasure hunt stopped");
            }
            
            @Override
            public void onFailure(String error) {
                Log.w(TAG, "Failed to remove geofences: " + error);
                // Still disable proximity checking
                treasureCollectionManager.setProximityCheckingEnabled(false);
                callback.onSuccess("Treasure hunt stopped (with warnings)");
            }
        });
    }
    
    /**
     * Update location for proximity checking (call this from location updates)
     * @param location Current location
     * @param sessionId Active session ID
     */
    public void updateLocation(Location location, String sessionId) {
        // This will only do proximity checking if geofences failed and it's enabled
        treasureCollectionManager.checkProximityForTreasures(location, sessionId);
    }
    
    /**
     * Manually collect a treasure (for testing or special cases)
     * @param treasure Treasure to collect
     */
    public void collectTreasure(TreasureLocation treasure) {
        Log.d(TAG, "Manually collecting treasure: " + treasure.treasureId);
        treasureCollectionManager.collectTreasure(treasure);
    }
    
    /**
     * Get nearby treasures for UI feedback
     * @param location Current location
     * @param sessionId Session ID
     * @param maxDistance Maximum distance in meters
     * @param callback Callback with nearby treasures
     */
    public void getNearbyTreasures(Location location, String sessionId, float maxDistance, 
                                 TreasureCollectionManager.NearbyTreasuresCallback callback) {
        treasureCollectionManager.getNearbyTreasures(location, sessionId, maxDistance, callback);
    }
    
    /**
     * Enable or disable proximity checking (fallback when geofences fail)
     * @param enabled Whether to enable proximity checking
     */
    public void setProximityCheckingEnabled(boolean enabled) {
        treasureCollectionManager.setProximityCheckingEnabled(enabled);
        Log.d(TAG, "Proximity checking " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if proximity checking is enabled
     * @return True if proximity checking is enabled
     */
    public boolean isProximityCheckingEnabled() {
        return treasureCollectionManager.isProximityCheckingEnabled();
    }
    
    /**
     * Trigger proximity animation for a treasure (for UI feedback)
     * @param treasure Treasure to animate
     */
    public void triggerProximityAnimation(TreasureLocation treasure) {
        treasureCollectionManager.triggerProximityAnimation(treasure);
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up TreasureHuntLocationManager");
        treasureCollectionManager.cleanup();
    }
    
    /**
     * Callback interface for treasure hunt operations
     */
    public interface TreasureHuntCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
}