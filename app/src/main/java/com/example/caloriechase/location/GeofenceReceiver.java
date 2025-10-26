package com.example.caloriechase.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.example.caloriechase.data.TreasureHuntDatabase;
import com.example.caloriechase.data.TreasureDao;
import com.example.caloriechase.data.TreasureLocation;
import com.example.caloriechase.data.SessionManager;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BroadcastReceiver that handles geofence transition events for treasure collection
 */
public class GeofenceReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Geofence event received");
        
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null");
            return;
        }
        
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: " + geofencingEvent.getErrorCode());
            return;
        }
        
        // Get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        
        // Only handle ENTER transitions (treasure collection)
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            if (triggeringGeofences != null && !triggeringGeofences.isEmpty()) {
                handleTreasureCollection(context, triggeringGeofences);
            }
        } else {
            Log.d(TAG, "Ignoring geofence transition: " + geofenceTransition);
        }
    }
    
    /**
     * Handle treasure collection when geofences are triggered
     */
    private void handleTreasureCollection(Context context, List<Geofence> triggeringGeofences) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        executor.execute(() -> {
            try {
                TreasureHuntDatabase database = TreasureHuntDatabase.getInstance(context);
                TreasureDao treasureDao = database.treasureDao();
                SessionManager sessionManager = SessionManager.getInstance(context);
                
                for (Geofence geofence : triggeringGeofences) {
                    String treasureId = geofence.getRequestId();
                    Log.d(TAG, "Processing treasure collection for: " + treasureId);
                    
                    // Get the treasure from database
                    TreasureLocation treasure = treasureDao.getTreasureById(treasureId);
                    if (treasure != null && !treasure.isCollected) {
                        // Mark treasure as collected
                        treasure.markCollected();
                        treasureDao.updateTreasure(treasure);
                        
                        Log.i(TAG, "Treasure collected: " + treasureId + " at " + treasure.collectionTimestamp);
                        
                        // Notify session manager about treasure collection
                        sessionManager.onTreasureCollected(treasure);
                        
                        // Trigger collection animation and feedback
                        triggerCollectionFeedback(context, treasure);
                        
                        // Update session progress
                        updateSessionProgress(context, treasure.sessionId);
                    } else if (treasure == null) {
                        Log.w(TAG, "Treasure not found in database: " + treasureId);
                    } else {
                        Log.d(TAG, "Treasure already collected: " + treasureId);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling treasure collection", e);
            }
        });
    }
    
    /**
     * Trigger collection animation and user feedback
     */
    private void triggerCollectionFeedback(Context context, TreasureLocation treasure) {
        // Send broadcast to active session activity for animation
        Intent feedbackIntent = new Intent(TreasureCollectionManager.ACTION_TREASURE_COLLECTED);
        feedbackIntent.putExtra(TreasureCollectionManager.EXTRA_TREASURE_ID, treasure.treasureId);
        feedbackIntent.putExtra(TreasureCollectionManager.EXTRA_TREASURE_TYPE, treasure.type.name());
        feedbackIntent.putExtra(TreasureCollectionManager.EXTRA_TREASURE_LAT, treasure.latitude);
        feedbackIntent.putExtra(TreasureCollectionManager.EXTRA_TREASURE_LNG, treasure.longitude);
        
        context.sendBroadcast(feedbackIntent);
        
        Log.d(TAG, "Treasure collection feedback triggered for: " + treasure.treasureId);
    }
    
    /**
     * Update session progress after treasure collection
     */
    private void updateSessionProgress(Context context, String sessionId) {
        try {
            SessionManager sessionManager = SessionManager.getInstance(context);
            sessionManager.updateSessionProgress();
            
            // Send broadcast to update UI
            Intent progressIntent = new Intent(TreasureCollectionManager.ACTION_SESSION_PROGRESS_UPDATED);
            progressIntent.putExtra(TreasureCollectionManager.EXTRA_SESSION_ID, sessionId);
            context.sendBroadcast(progressIntent);
            
            Log.d(TAG, "Session progress updated for session: " + sessionId);
        } catch (Exception e) {
            Log.e(TAG, "Error updating session progress", e);
        }
    }
}