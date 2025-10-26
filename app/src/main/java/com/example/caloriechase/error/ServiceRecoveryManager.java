package com.example.caloriechase.error;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.caloriechase.data.ActiveSession;
import com.example.caloriechase.data.SessionManager;
import com.example.caloriechase.TrackingService;
import com.example.caloriechase.TrackingServiceManager;

/**
 * Manages service recovery logic for app kills and system restarts
 * Ensures session continuity and data integrity
 */
public class ServiceRecoveryManager {
    
    private static final String TAG = "ServiceRecoveryManager";
    private static final String PREFS_NAME = "service_recovery";
    private static final String KEY_LAST_ACTIVE_SESSION = "last_active_session";
    private static final String KEY_LAST_SERVICE_STATE = "last_service_state";
    private static final String KEY_RECOVERY_TIMESTAMP = "recovery_timestamp";
    private static final String KEY_RECOVERY_ATTEMPT_COUNT = "recovery_attempt_count";
    
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    private static final long RECOVERY_TIMEOUT_MS = 30000; // 30 seconds
    
    private final Context context;
    private final SharedPreferences prefs;
    private final SessionManager sessionManager;
    private final TrackingServiceManager trackingServiceManager;
    private final ErrorHandler errorHandler;
    
    public ServiceRecoveryManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.sessionManager = SessionManager.getInstance(context);
        this.trackingServiceManager = new TrackingServiceManager(context);
        this.errorHandler = new ErrorHandler(context);
    }
    
    /**
     * Save current service state for recovery purposes
     */
    public void saveServiceState(String sessionId, ServiceState state) {
        Log.d(TAG, "Saving service state: " + state + " for session: " + sessionId);
        
        prefs.edit()
                .putString(KEY_LAST_ACTIVE_SESSION, sessionId)
                .putString(KEY_LAST_SERVICE_STATE, state.name())
                .putLong(KEY_RECOVERY_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }
    
    /**
     * Clear saved service state (called when session ends normally)
     */
    public void clearServiceState() {
        Log.d(TAG, "Clearing saved service state");
        
        prefs.edit()
                .remove(KEY_LAST_ACTIVE_SESSION)
                .remove(KEY_LAST_SERVICE_STATE)
                .remove(KEY_RECOVERY_TIMESTAMP)
                .remove(KEY_RECOVERY_ATTEMPT_COUNT)
                .apply();
    }
    
    /**
     * Attempt to recover service after app restart or system restart
     */
    public void attemptServiceRecovery(ServiceRecoveryCallback callback) {
        String lastSessionId = prefs.getString(KEY_LAST_ACTIVE_SESSION, null);
        String lastStateStr = prefs.getString(KEY_LAST_SERVICE_STATE, null);
        long recoveryTimestamp = prefs.getLong(KEY_RECOVERY_TIMESTAMP, 0);
        int attemptCount = prefs.getInt(KEY_RECOVERY_ATTEMPT_COUNT, 0);
        
        if (lastSessionId == null || lastStateStr == null) {
            Log.d(TAG, "No service state to recover");
            callback.onRecoveryNotNeeded();
            return;
        }
        
        // Check if recovery has timed out
        long timeSinceLastSave = System.currentTimeMillis() - recoveryTimestamp;
        if (timeSinceLastSave > RECOVERY_TIMEOUT_MS) {
            Log.w(TAG, "Service recovery timed out, clearing state");
            clearServiceState();
            callback.onRecoveryFailed("Recovery timeout exceeded");
            return;
        }
        
        // Check if we've exceeded max recovery attempts
        if (attemptCount >= MAX_RECOVERY_ATTEMPTS) {
            Log.w(TAG, "Max recovery attempts exceeded, clearing state");
            clearServiceState();
            callback.onRecoveryFailed("Maximum recovery attempts exceeded");
            return;
        }
        
        // Increment attempt count
        prefs.edit().putInt(KEY_RECOVERY_ATTEMPT_COUNT, attemptCount + 1).apply();
        
        ServiceState lastState;
        try {
            lastState = ServiceState.valueOf(lastStateStr);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid service state: " + lastStateStr, e);
            clearServiceState();
            callback.onRecoveryFailed("Invalid service state");
            return;
        }
        
        Log.i(TAG, "Attempting service recovery for session: " + lastSessionId + 
              ", state: " + lastState + ", attempt: " + (attemptCount + 1));
        
        // Verify the session still exists and is active
        sessionManager.getCurrentActiveSession(new SessionManager.SessionCallback<ActiveSession>() {
            @Override
            public void onSuccess(ActiveSession activeSession) {
                if (activeSession != null && activeSession.sessionId.equals(lastSessionId)) {
                    // Session exists, attempt to recover service
                    recoverTrackingService(activeSession, lastState, callback);
                } else {
                    Log.w(TAG, "Active session not found or session ID mismatch during recovery");
                    clearServiceState();
                    callback.onRecoveryFailed("Active session not found");
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error checking active session during recovery", error);
                callback.onRecoveryFailed("Database error: " + error.getMessage());
            }
        });
    }
    
    /**
     * Recover the tracking service for an active session
     */
    private void recoverTrackingService(ActiveSession activeSession, ServiceState lastState, 
                                      ServiceRecoveryCallback callback) {
        Log.d(TAG, "Recovering tracking service for session: " + activeSession.sessionId);
        
        try {
            // Determine recovery type based on how much time has passed
            long timeSinceLastSave = System.currentTimeMillis() - 
                                   prefs.getLong(KEY_RECOVERY_TIMESTAMP, 0);
            
            ErrorHandler.ServiceRecoveryType recoveryType;
            if (timeSinceLastSave > 60000) { // More than 1 minute
                recoveryType = ErrorHandler.ServiceRecoveryType.SYSTEM_RESTART;
            } else {
                recoveryType = ErrorHandler.ServiceRecoveryType.APP_KILLED;
            }
            
            // Start the tracking service
            trackingServiceManager.startTracking(activeSession.sessionId);
            
            // If the session was paused, restore paused state
            if (lastState == ServiceState.PAUSED) {
                // Give the service a moment to start before pausing
                android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.postDelayed(() -> {
                    trackingServiceManager.pauseTracking();
                }, 1000);
            }
            
            // Show recovery notification to user
            errorHandler.handleServiceRecovery(recoveryType, new ErrorHandler.ServiceRecoveryCallback() {
                @Override
                public void onServiceRecovered() {
                    // User acknowledged recovery
                }
            });
            
            // Clear recovery attempt count on successful recovery
            prefs.edit().remove(KEY_RECOVERY_ATTEMPT_COUNT).apply();
            
            Log.i(TAG, "Service recovery successful for session: " + activeSession.sessionId);
            callback.onRecoverySuccessful(activeSession, recoveryType);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during service recovery", e);
            callback.onRecoveryFailed("Service restart failed: " + e.getMessage());
        }
    }
    
    /**
     * Handle service crash recovery
     */
    public void handleServiceCrash(String sessionId, ServiceCrashCallback callback) {
        Log.w(TAG, "Handling service crash for session: " + sessionId);
        
        // Verify session is still active
        sessionManager.getCurrentActiveSession(new SessionManager.SessionCallback<ActiveSession>() {
            @Override
            public void onSuccess(ActiveSession activeSession) {
                if (activeSession != null && activeSession.sessionId.equals(sessionId)) {
                    // Attempt to restart the service
                    try {
                        trackingServiceManager.startTracking(sessionId);
                        
                        // Show crash recovery notification
                        errorHandler.handleServiceRecovery(
                            ErrorHandler.ServiceRecoveryType.SERVICE_CRASHED,
                            new ErrorHandler.ServiceRecoveryCallback() {
                                @Override
                                public void onServiceRecovered() {
                                    // User acknowledged recovery
                                }
                            }
                        );
                        
                        callback.onCrashRecoverySuccessful();
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to restart service after crash", e);
                        callback.onCrashRecoveryFailed("Service restart failed: " + e.getMessage());
                    }
                } else {
                    Log.w(TAG, "No active session found for crash recovery");
                    callback.onCrashRecoveryFailed("No active session found");
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error checking session during crash recovery", error);
                callback.onCrashRecoveryFailed("Database error: " + error.getMessage());
            }
        });
    }
    
    /**
     * Handle low memory conditions
     */
    public void handleLowMemoryRecovery(String sessionId, LowMemoryCallback callback) {
        Log.w(TAG, "Handling low memory recovery for session: " + sessionId);
        
        // In low memory conditions, we should be more conservative
        // and only restart if the session is very recent
        long timeSinceLastSave = System.currentTimeMillis() - 
                               prefs.getLong(KEY_RECOVERY_TIMESTAMP, 0);
        
        if (timeSinceLastSave > 300000) { // More than 5 minutes
            Log.w(TAG, "Session too old for low memory recovery, abandoning");
            clearServiceState();
            callback.onLowMemoryRecoveryFailed("Session too old");
            return;
        }
        
        sessionManager.getCurrentActiveSession(new SessionManager.SessionCallback<ActiveSession>() {
            @Override
            public void onSuccess(ActiveSession activeSession) {
                if (activeSession != null && activeSession.sessionId.equals(sessionId)) {
                    try {
                        // Restart with reduced resource usage
                        trackingServiceManager.startTracking(sessionId);
                        
                        // Show low memory recovery notification
                        errorHandler.handleServiceRecovery(
                            ErrorHandler.ServiceRecoveryType.LOW_MEMORY,
                            new ErrorHandler.ServiceRecoveryCallback() {
                                @Override
                                public void onServiceRecovered() {
                                    // User acknowledged recovery
                                }
                            }
                        );
                        
                        callback.onLowMemoryRecoverySuccessful();
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to restart service after low memory", e);
                        callback.onLowMemoryRecoveryFailed("Service restart failed: " + e.getMessage());
                    }
                } else {
                    Log.w(TAG, "No active session found for low memory recovery");
                    callback.onLowMemoryRecoveryFailed("No active session found");
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error checking session during low memory recovery", error);
                callback.onLowMemoryRecoveryFailed("Database error: " + error.getMessage());
            }
        });
    }
    
    /**
     * Check if service recovery is needed on app startup
     */
    public boolean isRecoveryNeeded() {
        String lastSessionId = prefs.getString(KEY_LAST_ACTIVE_SESSION, null);
        String lastStateStr = prefs.getString(KEY_LAST_SERVICE_STATE, null);
        long recoveryTimestamp = prefs.getLong(KEY_RECOVERY_TIMESTAMP, 0);
        
        if (lastSessionId == null || lastStateStr == null) {
            return false;
        }
        
        // Check if recovery has timed out
        long timeSinceLastSave = System.currentTimeMillis() - recoveryTimestamp;
        return timeSinceLastSave <= RECOVERY_TIMEOUT_MS;
    }
    
    /**
     * Get the last active session ID for recovery
     */
    public String getLastActiveSessionId() {
        return prefs.getString(KEY_LAST_ACTIVE_SESSION, null);
    }
    
    /**
     * Service state enum
     */
    public enum ServiceState {
        ACTIVE, PAUSED, STOPPED
    }
    
    /**
     * Callback interface for service recovery
     */
    public interface ServiceRecoveryCallback {
        void onRecoverySuccessful(ActiveSession recoveredSession, ErrorHandler.ServiceRecoveryType recoveryType);
        void onRecoveryFailed(String reason);
        void onRecoveryNotNeeded();
    }
    
    /**
     * Callback interface for service crash recovery
     */
    public interface ServiceCrashCallback {
        void onCrashRecoverySuccessful();
        void onCrashRecoveryFailed(String reason);
    }
    
    /**
     * Callback interface for low memory recovery
     */
    public interface LowMemoryCallback {
        void onLowMemoryRecoverySuccessful();
        void onLowMemoryRecoveryFailed(String reason);
    }
}