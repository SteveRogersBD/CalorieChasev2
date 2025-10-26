package com.example.caloriechase.location;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.example.caloriechase.data.TreasureLocation;
import com.example.caloriechase.error.ErrorHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages geofences for treasure collection in treasure hunt sessions
 */
public class GeofenceManager {
    private static final String TAG = "GeofenceManager";
    private static final int GEOFENCE_EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    
    private final Context context;
    private final GeofencingClient geofencingClient;
    private final ErrorHandler errorHandler;
    private PendingIntent geofencePendingIntent;
    private boolean manualProximityMode = false;
    
    public GeofenceManager(Context context) {
        this.context = context.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(this.context);
        this.errorHandler = new ErrorHandler(this.context);
    }
    
    /**
     * Set up geofences for a list of treasure locations
     * @param treasures List of treasure locations to create geofences for
     * @param sessionId Session ID for tracking
     * @param callback Callback for success/failure
     */
    public void setupGeofences(List<TreasureLocation> treasures, String sessionId, GeofenceCallback callback) {
        if (treasures == null || treasures.isEmpty()) {
            Log.w(TAG, "No treasures provided for geofence setup");
            callback.onFailure("No treasures to set up geofences for");
            return;
        }
        
        // Check location permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted for geofence setup");
            callback.onFailure("Location permission required for geofence setup");
            return;
        }
        
        List<Geofence> geofenceList = createGeofenceList(treasures);
        GeofencingRequest geofencingRequest = createGeofencingRequest(geofenceList);
        
        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Geofences added successfully for session: " + sessionId);
                manualProximityMode = false;
                callback.onSuccess(geofenceList.size());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to add geofences for session: " + sessionId, e);
                handleGeofenceFailure(e, treasures, sessionId, callback);
            });
    }
    
    /**
     * Remove all geofences for a session
     * @param sessionId Session ID to remove geofences for
     * @param callback Callback for success/failure
     */
    public void removeGeofences(String sessionId, GeofenceCallback callback) {
        if (geofencePendingIntent != null) {
            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofences removed successfully for session: " + sessionId);
                    callback.onSuccess(0);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove geofences for session: " + sessionId, e);
                    callback.onFailure("Failed to remove geofences: " + e.getMessage());
                });
        } else {
            Log.d(TAG, "No geofences to remove for session: " + sessionId);
            callback.onSuccess(0);
        }
    }
    
    /**
     * Remove specific geofences by their IDs
     * @param geofenceIds List of geofence IDs to remove
     * @param callback Callback for success/failure
     */
    public void removeGeofencesByIds(List<String> geofenceIds, GeofenceCallback callback) {
        if (geofenceIds == null || geofenceIds.isEmpty()) {
            callback.onSuccess(0);
            return;
        }
        
        geofencingClient.removeGeofences(geofenceIds)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Specific geofences removed successfully: " + geofenceIds.size());
                callback.onSuccess(geofenceIds.size());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to remove specific geofences", e);
                callback.onFailure("Failed to remove specific geofences: " + e.getMessage());
            });
    }
    
    /**
     * Create a list of Geofence objects from treasure locations
     */
    private List<Geofence> createGeofenceList(List<TreasureLocation> treasures) {
        List<Geofence> geofenceList = new ArrayList<>();
        
        for (TreasureLocation treasure : treasures) {
            if (!treasure.isCollected) {
                Geofence geofence = new Geofence.Builder()
                    .setRequestId(treasure.treasureId)
                    .setCircularRegion(
                        treasure.latitude,
                        treasure.longitude,
                        treasure.radius
                    )
                    .setExpirationDuration(GEOFENCE_EXPIRATION_TIME)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build();
                
                geofenceList.add(geofence);
                Log.d(TAG, "Created geofence for treasure: " + treasure.treasureId + 
                      " at (" + treasure.latitude + ", " + treasure.longitude + 
                      ") with radius: " + treasure.radius + "m");
            }
        }
        
        return geofenceList;
    }
    
    /**
     * Create a GeofencingRequest from a list of geofences
     */
    private GeofencingRequest createGeofencingRequest(List<Geofence> geofenceList) {
        return new GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build();
    }
    
    /**
     * Get the PendingIntent for geofence transitions
     */
    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        
        Intent intent = new Intent(context, GeofenceReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return geofencePendingIntent;
    }
    
    /**
     * Handle geofence setup failures with appropriate fallbacks
     */
    private void handleGeofenceFailure(Exception error, List<TreasureLocation> treasures, 
                                     String sessionId, GeofenceCallback callback) {
        ErrorHandler.GeofenceFailureType failureType = determineGeofenceFailureType(error);
        
        errorHandler.handleGeofenceFailure(failureType, new ErrorHandler.GeofenceFailureCallback() {
            @Override
            public void onContinueWithManualChecking() {
                Log.i(TAG, "Switching to manual proximity checking for session: " + sessionId);
                manualProximityMode = true;
                callback.onSuccess(treasures.size()); // Report success with manual mode
            }
            
            @Override
            public void onRetryGeofences() {
                // Retry geofence setup after a brief delay
                android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.postDelayed(() -> {
                    setupGeofences(treasures, sessionId, callback);
                }, 2000); // 2 second delay
            }
        });
    }
    
    /**
     * Determine the type of geofence failure from the exception
     */
    private ErrorHandler.GeofenceFailureType determineGeofenceFailureType(Exception error) {
        if (error instanceof ApiException) {
            ApiException apiException = (ApiException) error;
            switch (apiException.getStatusCode()) {
                case 1000: // GEOFENCE_NOT_AVAILABLE
                    return ErrorHandler.GeofenceFailureType.GEOFENCE_SERVICE_UNAVAILABLE;
                case 1001: // GEOFENCE_TOO_MANY_GEOFENCES
                    return ErrorHandler.GeofenceFailureType.GEOFENCE_LIMIT_EXCEEDED;
                case 1002: // GEOFENCE_TOO_MANY_PENDING_INTENTS
                    return ErrorHandler.GeofenceFailureType.GEOFENCE_LIMIT_EXCEEDED;
                default:
                    return ErrorHandler.GeofenceFailureType.GEOFENCE_REGISTRATION_FAILED;
            }
        } else if (error instanceof SecurityException) {
            return ErrorHandler.GeofenceFailureType.GEOFENCE_PERMISSION_DENIED;
        } else {
            return ErrorHandler.GeofenceFailureType.GEOFENCE_REGISTRATION_FAILED;
        }
    }
    
    /**
     * Check if geofence manager is in manual proximity mode
     */
    public boolean isManualProximityMode() {
        return manualProximityMode;
    }
    
    /**
     * Manually check proximity to treasures (fallback when geofences fail)
     */
    public List<TreasureLocation> checkProximityToTreasures(android.location.Location currentLocation, 
                                                           List<TreasureLocation> treasures) {
        List<TreasureLocation> nearbyTreasures = new ArrayList<>();
        
        if (currentLocation == null || treasures == null) {
            return nearbyTreasures;
        }
        
        for (TreasureLocation treasure : treasures) {
            if (!treasure.isCollected) {
                android.location.Location treasureLocation = new android.location.Location("treasure");
                treasureLocation.setLatitude(treasure.latitude);
                treasureLocation.setLongitude(treasure.longitude);
                
                float distance = currentLocation.distanceTo(treasureLocation);
                
                // Use a slightly larger radius for manual checking to compensate for less frequent updates
                float checkRadius = treasure.radius * 1.2f;
                
                if (distance <= checkRadius) {
                    nearbyTreasures.add(treasure);
                    Log.d(TAG, "Manual proximity check: treasure " + treasure.treasureId + 
                          " is within range (" + distance + "m <= " + checkRadius + "m)");
                }
            }
        }
        
        return nearbyTreasures;
    }
    
    /**
     * Force cleanup of all geofences (for error recovery)
     */
    public void forceCleanupGeofences(GeofenceCallback callback) {
        Log.w(TAG, "Force cleaning up all geofences");
        
        geofencingClient.removeGeofences(getGeofencePendingIntent())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Force cleanup successful");
                manualProximityMode = false;
                if (callback != null) {
                    callback.onSuccess(0);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Force cleanup failed", e);
                // Even if cleanup fails, reset to manual mode
                manualProximityMode = true;
                if (callback != null) {
                    callback.onFailure("Cleanup failed, switched to manual mode");
                }
            });
    }
    
    /**
     * Get geofence status information for debugging
     */
    public String getGeofenceStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("Geofence Manager Status:\n");
        status.append("- Manual proximity mode: ").append(manualProximityMode).append("\n");
        status.append("- Pending intent active: ").append(geofencePendingIntent != null).append("\n");
        
        // Check location permission
        boolean hasLocationPermission = ActivityCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        status.append("- Location permission: ").append(hasLocationPermission).append("\n");
        
        return status.toString();
    }
    
    /**
     * Callback interface for geofence operations
     */
    public interface GeofenceCallback {
        void onSuccess(int geofenceCount);
        void onFailure(String error);
    }
}