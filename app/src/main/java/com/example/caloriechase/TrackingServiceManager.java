package com.example.caloriechase;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Helper class to manage TrackingService interactions
 */
public class TrackingServiceManager {
    
    private static final String TAG = "TrackingServiceManager";
    
    private Context context;
    private TrackingService trackingService;
    private boolean isServiceBound = false;
    private ServiceConnection serviceConnection;
    private ServiceConnectionListener connectionListener;
    
    public interface ServiceConnectionListener {
        void onServiceConnected(TrackingService service);
        void onServiceDisconnected();
    }
    
    public TrackingServiceManager(Context context) {
        this.context = context.getApplicationContext();
        initializeServiceConnection();
    }
    
    /**
     * Initialize service connection
     */
    private void initializeServiceConnection() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Service connected");
                TrackingService.TrackingBinder binder = (TrackingService.TrackingBinder) service;
                trackingService = binder.getService();
                isServiceBound = true;
                
                if (connectionListener != null) {
                    connectionListener.onServiceConnected(trackingService);
                }
            }
            
            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Service disconnected");
                trackingService = null;
                isServiceBound = false;
                
                if (connectionListener != null) {
                    connectionListener.onServiceDisconnected();
                }
            }
        };
    }
    
    /**
     * Start tracking for a session
     */
    public void startTracking(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            Log.e(TAG, "Cannot start tracking: invalid session ID");
            return;
        }
        
        Log.d(TAG, "Starting tracking for session: " + sessionId);
        
        Intent intent = new Intent(context, TrackingService.class);
        intent.setAction(TrackingService.ACTION_START_TRACKING);
        intent.putExtra(TrackingService.EXTRA_SESSION_ID, sessionId);
        
        // Start the service
        context.startForegroundService(intent);
        
        // Bind to the service for direct communication
        bindToService();
    }
    
    /**
     * Stop tracking
     */
    public void stopTracking() {
        Log.d(TAG, "Stopping tracking");
        
        Intent intent = new Intent(context, TrackingService.class);
        intent.setAction(TrackingService.ACTION_STOP_TRACKING);
        context.startService(intent);
        
        // Unbind from service
        unbindFromService();
    }
    
    /**
     * Pause tracking
     */
    public void pauseTracking() {
        Log.d(TAG, "Pausing tracking");
        
        Intent intent = new Intent(context, TrackingService.class);
        intent.setAction(TrackingService.ACTION_PAUSE_TRACKING);
        context.startService(intent);
    }
    
    /**
     * Resume tracking
     */
    public void resumeTracking() {
        Log.d(TAG, "Resuming tracking");
        
        Intent intent = new Intent(context, TrackingService.class);
        intent.setAction(TrackingService.ACTION_RESUME_TRACKING);
        context.startService(intent);
    }
    
    /**
     * Retry GPS location updates
     */
    public void retryGps() {
        Log.d(TAG, "Retrying GPS");
        
        if (trackingService != null) {
            // Call service method to retry GPS
            trackingService.retryLocationUpdates();
        } else {
            Log.w(TAG, "Cannot retry GPS: service not bound");
        }
    }
    
    /**
     * Bind to the tracking service
     */
    public void bindToService() {
        if (!isServiceBound) {
            Intent intent = new Intent(context, TrackingService.class);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }
    
    /**
     * Unbind from the tracking service
     */
    public void unbindFromService() {
        if (isServiceBound) {
            context.unbindService(serviceConnection);
            isServiceBound = false;
            trackingService = null;
        }
    }
    
    /**
     * Check if service is bound
     */
    public boolean isServiceBound() {
        return isServiceBound;
    }
    
    /**
     * Get the tracking service instance (only available when bound)
     */
    public TrackingService getTrackingService() {
        return trackingService;
    }
    
    /**
     * Check if tracking is active
     */
    public boolean isTracking() {
        return trackingService != null && trackingService.isTracking();
    }
    
    /**
     * Check if the tracking service is running
     */
    public boolean isServiceRunning() {
        android.app.ActivityManager manager = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (TrackingService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if tracking is paused
     */
    public boolean isPaused() {
        return trackingService != null && trackingService.isPaused();
    }
    
    /**
     * Get current session ID
     */
    public String getCurrentSessionId() {
        return trackingService != null ? trackingService.getCurrentSessionId() : null;
    }
    
    /**
     * Get current steps
     */
    public int getCurrentSteps() {
        return trackingService != null ? trackingService.getCurrentSteps() : 0;
    }
    
    /**
     * Get current distance
     */
    public float getCurrentDistance() {
        return trackingService != null ? trackingService.getCurrentDistance() : 0.0f;
    }
    
    /**
     * Set service connection listener
     */
    public void setServiceConnectionListener(ServiceConnectionListener listener) {
        this.connectionListener = listener;
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        unbindFromService();
        connectionListener = null;
    }
    
    /**
     * Static helper methods for quick service control
     */
    
    public static void startTrackingSession(Context context, String sessionId) {
        TrackingServiceManager manager = new TrackingServiceManager(context);
        manager.startTracking(sessionId);
    }
    
    public static void stopTrackingSession(Context context) {
        TrackingServiceManager manager = new TrackingServiceManager(context);
        manager.stopTracking();
    }
    
    public static void pauseTrackingSession(Context context) {
        TrackingServiceManager manager = new TrackingServiceManager(context);
        manager.pauseTracking();
    }
    
    public static void resumeTrackingSession(Context context) {
        TrackingServiceManager manager = new TrackingServiceManager(context);
        manager.resumeTracking();
    }
}