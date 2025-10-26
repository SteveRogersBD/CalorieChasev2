package com.example.caloriechase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import java.util.List;

/**
 * Broadcast receiver for handling geofence transitions
 */
public class GeofenceReceiver extends BroadcastReceiver {
    
    private static final String TAG = "GeofenceReceiver";
    private static final String GEOFENCE_ACTION = "com.example.caloriechase.GEOFENCE_TRIGGERED";
    
    @Override
    public void onReceive(Context context, Intent intent) {
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
        
        // Check if the transition is of interest
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Get the geofences that were triggered
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            
            if (triggeringGeofences != null && !triggeringGeofences.isEmpty()) {
                for (Geofence geofence : triggeringGeofences) {
                    String geofenceId = geofence.getRequestId();
                    Log.d(TAG, "Geofence entered: " + geofenceId);
                    
                    // Send broadcast to GameplayActivity
                    Intent broadcastIntent = new Intent(GEOFENCE_ACTION);
                    broadcastIntent.putExtra("geofence_id", geofenceId);
                    context.sendBroadcast(broadcastIntent);
                }
            }
        } else {
            Log.d(TAG, "Geofence transition not of interest: " + geofenceTransition);
        }
    }
}