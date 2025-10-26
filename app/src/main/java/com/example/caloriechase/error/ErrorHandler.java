package com.example.caloriechase.error;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Centralized error handling utility for treasure hunt sessions
 * Provides consistent error messaging and recovery options
 */
public class ErrorHandler {
    
    private final Context context;
    
    public ErrorHandler(Context context) {
        this.context = context;
    }
    
    /**
     * Handle permission denial with clear messaging and settings navigation
     */
    public void handlePermissionDenied(PermissionType permissionType, PermissionDenialCallback callback) {
        String title;
        String message;
        String settingsAction;
        
        switch (permissionType) {
            case LOCATION:
                title = "Location Permission Required";
                message = "CalorieChase needs location access to track your treasure hunt sessions and detect treasure locations.\n\n" +
                         "Without location permission, you won't be able to:\n" +
                         "• Track your movement and distance\n" +
                         "• Collect treasures automatically\n" +
                         "• See your position on the map";
                settingsAction = "Grant Location Permission";
                break;
                
            case ACTIVITY_RECOGNITION:
                title = "Activity Recognition Permission Required";
                message = "CalorieChase needs activity recognition to count your steps and detect movement patterns.\n\n" +
                         "Without this permission, you won't be able to:\n" +
                         "• Get accurate step counts\n" +
                         "• Use step-based distance fallback when GPS is weak\n" +
                         "• Detect periods of inactivity for auto-pause";
                settingsAction = "Grant Activity Permission";
                break;
                
            case BACKGROUND_LOCATION:
                title = "Background Location Permission Required";
                message = "CalorieChase needs background location access to continue tracking when the app is minimized.\n\n" +
                         "Without this permission:\n" +
                         "• Tracking will stop when you switch apps\n" +
                         "• You may miss treasure collections\n" +
                         "• Session data may be incomplete";
                settingsAction = "Grant Background Location";
                break;
                
            default:
                title = "Permission Required";
                message = "CalorieChase needs additional permissions to function properly.";
                settingsAction = "Open Settings";
                break;
        }
        
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(settingsAction, (dialog, which) -> {
                    openAppSettings();
                    if (callback != null) {
                        callback.onSettingsOpened();
                    }
                })
                .setNegativeButton("Continue Without", (dialog, which) -> {
                    if (callback != null) {
                        callback.onContinueWithoutPermission();
                    }
                })
                .setNeutralButton("Cancel", (dialog, which) -> {
                    if (callback != null) {
                        callback.onCancel();
                    }
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * Handle GPS failure with step-based distance fallback
     */
    public void handleGpsFailure(GpsFailureType failureType, GpsFailureCallback callback) {
        String title;
        String message;
        String primaryAction;
        String secondaryAction;
        
        switch (failureType) {
            case GPS_DISABLED:
                title = "GPS Disabled";
                message = "GPS is currently disabled on your device. Your treasure hunt session will use step counting for distance tracking, which may be less accurate.\n\n" +
                         "For the best experience, we recommend enabling GPS.";
                primaryAction = "Enable GPS";
                secondaryAction = "Continue with Steps";
                break;
                
            case GPS_WEAK_SIGNAL:
                title = "Weak GPS Signal";
                message = "GPS signal is weak in your current location. The app will automatically switch to step-based distance tracking.\n\n" +
                         "This may happen indoors or in areas with poor satellite visibility.";
                primaryAction = "Try Again";
                secondaryAction = "Continue with Steps";
                break;
                
            case GPS_TIMEOUT:
                title = "GPS Timeout";
                message = "Unable to get a GPS fix within the expected time. The session will continue using step counting for distance measurement.\n\n" +
                         "GPS may become available during your session.";
                primaryAction = "Retry GPS";
                secondaryAction = "Continue with Steps";
                break;
                
            case GPS_PERMISSION_DENIED:
                title = "GPS Permission Denied";
                message = "Location permission was denied. The session will use step counting only.\n\n" +
                         "Distance and treasure collection accuracy will be reduced.";
                primaryAction = "Grant Permission";
                secondaryAction = "Continue with Steps";
                break;
                
            default:
                title = "GPS Unavailable";
                message = "GPS is currently unavailable. Using step-based tracking instead.";
                primaryAction = "Retry";
                secondaryAction = "Continue";
                break;
        }
        
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(primaryAction, (dialog, which) -> {
                    if (callback != null) {
                        callback.onRetryGps();
                    }
                })
                .setNegativeButton(secondaryAction, (dialog, which) -> {
                    if (callback != null) {
                        callback.onContinueWithSteps();
                    }
                })
                .show();
    }
    
    /**
     * Handle geofence failure with manual proximity checking
     */
    public void handleGeofenceFailure(GeofenceFailureType failureType, GeofenceFailureCallback callback) {
        String title;
        String message;
        
        switch (failureType) {
            case GEOFENCE_LIMIT_EXCEEDED:
                title = "Geofence Limit Exceeded";
                message = "Your device has reached the maximum number of geofences. The app will use manual proximity checking to detect treasure collection.\n\n" +
                         "This may slightly reduce battery life but won't affect functionality.";
                break;
                
            case GEOFENCE_SERVICE_UNAVAILABLE:
                title = "Geofence Service Unavailable";
                message = "Google Play Services geofencing is temporarily unavailable. The app will manually check your proximity to treasures.\n\n" +
                         "Treasure collection will still work normally.";
                break;
                
            case GEOFENCE_PERMISSION_DENIED:
                title = "Geofence Permission Denied";
                message = "Location permission is required for automatic treasure detection. The app will check your proximity manually.\n\n" +
                         "You may need to get closer to treasures for collection.";
                break;
                
            case GEOFENCE_REGISTRATION_FAILED:
                title = "Geofence Registration Failed";
                message = "Failed to register treasure locations for automatic detection. The app will use manual proximity checking instead.\n\n" +
                         "All treasures can still be collected normally.";
                break;
                
            default:
                title = "Geofence Error";
                message = "Automatic treasure detection is unavailable. Using manual proximity checking.";
                break;
        }
        
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Continue", (dialog, which) -> {
                    if (callback != null) {
                        callback.onContinueWithManualChecking();
                    }
                })
                .setNegativeButton("Retry", (dialog, which) -> {
                    if (callback != null) {
                        callback.onRetryGeofences();
                    }
                })
                .show();
    }
    
    /**
     * Handle service recovery after app kills and system restarts
     */
    public void handleServiceRecovery(ServiceRecoveryType recoveryType, ServiceRecoveryCallback callback) {
        String title;
        String message;
        
        switch (recoveryType) {
            case APP_KILLED:
                title = "Session Restored";
                message = "Your treasure hunt session was automatically restored after the app was closed.\n\n" +
                         "All progress has been preserved and tracking will continue.";
                break;
                
            case SYSTEM_RESTART:
                title = "Session Recovered";
                message = "Your treasure hunt session was recovered after a system restart.\n\n" +
                         "Some recent progress may need to be recalculated.";
                break;
                
            case SERVICE_CRASHED:
                title = "Tracking Restored";
                message = "The tracking service was automatically restarted after an unexpected error.\n\n" +
                         "Your session progress has been preserved.";
                break;
                
            case LOW_MEMORY:
                title = "Service Restarted";
                message = "The tracking service was restarted due to low memory conditions.\n\n" +
                         "Session tracking will continue normally.";
                break;
                
            default:
                title = "Session Restored";
                message = "Your treasure hunt session has been automatically restored.";
                break;
        }
        
        // Show a brief toast for service recovery (less intrusive than dialog)
        Toast.makeText(context, title + ": " + message, Toast.LENGTH_LONG).show();
        
        if (callback != null) {
            callback.onServiceRecovered();
        }
    }
    
    /**
     * Handle network failure for Maps and Places API calls
     */
    public void handleNetworkFailure(NetworkFailureType failureType, NetworkFailureCallback callback) {
        String title;
        String message;
        String primaryAction;
        String secondaryAction;
        
        switch (failureType) {
            case MAPS_API_FAILED:
                title = "Maps Loading Failed";
                message = "Unable to load map data due to network issues. You can still continue with your session, but map features may be limited.\n\n" +
                         "Check your internet connection and try again.";
                primaryAction = "Retry";
                secondaryAction = "Continue Offline";
                break;
                
            case PLACES_API_FAILED:
                title = "Location Search Failed";
                message = "Unable to search for locations due to network issues. You can still select a starting point by tapping on the map.\n\n" +
                         "Check your internet connection to use location search.";
                primaryAction = "Retry";
                secondaryAction = "Use Map Selection";
                break;
                
            case GEOCODING_FAILED:
                title = "Address Lookup Failed";
                message = "Unable to get address information for the selected location due to network issues.\n\n" +
                         "You can still use this location for your treasure hunt.";
                primaryAction = "Retry";
                secondaryAction = "Continue Without Address";
                break;
                
            case NETWORK_TIMEOUT:
                title = "Network Timeout";
                message = "The network request timed out. Please check your internet connection and try again.\n\n" +
                         "You can continue with limited functionality.";
                primaryAction = "Retry";
                secondaryAction = "Continue Offline";
                break;
                
            case NO_INTERNET:
                title = "No Internet Connection";
                message = "No internet connection detected. Map features and location search will be limited.\n\n" +
                         "You can still start a treasure hunt session using cached data.";
                primaryAction = "Check Connection";
                secondaryAction = "Continue Offline";
                break;
                
            default:
                title = "Network Error";
                message = "A network error occurred. Some features may be limited.";
                primaryAction = "Retry";
                secondaryAction = "Continue";
                break;
        }
        
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(primaryAction, (dialog, which) -> {
                    if (callback != null) {
                        callback.onRetryNetwork();
                    }
                })
                .setNegativeButton(secondaryAction, (dialog, which) -> {
                    if (callback != null) {
                        callback.onContinueOffline();
                    }
                })
                .show();
    }
    
    /**
     * Handle general session errors with recovery options
     */
    public void handleSessionError(SessionErrorType errorType, String details, SessionErrorCallback callback) {
        String title;
        String message;
        
        switch (errorType) {
            case SESSION_CREATION_FAILED:
                title = "Session Creation Failed";
                message = "Unable to create a new treasure hunt session.\n\n" +
                         "Error: " + (details != null ? details : "Unknown error") + "\n\n" +
                         "Please try again or restart the app.";
                break;
                
            case SESSION_LOAD_FAILED:
                title = "Session Load Failed";
                message = "Unable to load your active session.\n\n" +
                         "Error: " + (details != null ? details : "Unknown error") + "\n\n" +
                         "Your session data may be corrupted.";
                break;
                
            case TREASURE_GENERATION_FAILED:
                title = "Treasure Generation Failed";
                message = "Unable to generate treasures for your session.\n\n" +
                         "Error: " + (details != null ? details : "Unknown error") + "\n\n" +
                         "Please try selecting a different starting point.";
                break;
                
            case DATABASE_ERROR:
                title = "Database Error";
                message = "A database error occurred while saving your session data.\n\n" +
                         "Error: " + (details != null ? details : "Unknown error") + "\n\n" +
                         "Your progress may not be saved properly.";
                break;
                
            case SENSOR_ERROR:
                title = "Sensor Error";
                message = "Unable to access device sensors for step counting.\n\n" +
                         "Error: " + (details != null ? details : "Unknown error") + "\n\n" +
                         "Distance tracking may be less accurate.";
                break;
                
            default:
                title = "Session Error";
                message = "An error occurred with your treasure hunt session.\n\n" +
                         "Error: " + (details != null ? details : "Unknown error");
                break;
        }
        
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Retry", (dialog, which) -> {
                    if (callback != null) {
                        callback.onRetry();
                    }
                })
                .setNegativeButton("Cancel Session", (dialog, which) -> {
                    if (callback != null) {
                        callback.onCancelSession();
                    }
                })
                .setNeutralButton("Continue Anyway", (dialog, which) -> {
                    if (callback != null) {
                        callback.onContinueAnyway();
                    }
                })
                .show();
    }
    
    /**
     * Open app settings for permission management
     */
    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // Fallback to general settings
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Toast.makeText(context, "Please find app permissions manually in settings", Toast.LENGTH_LONG).show();
            } catch (Exception fallbackException) {
                Toast.makeText(context, "Unable to open settings. Please navigate to app permissions manually.", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    // Enums for error types
    public enum PermissionType {
        LOCATION, ACTIVITY_RECOGNITION, BACKGROUND_LOCATION
    }
    
    public enum GpsFailureType {
        GPS_DISABLED, GPS_WEAK_SIGNAL, GPS_TIMEOUT, GPS_PERMISSION_DENIED
    }
    
    public enum GeofenceFailureType {
        GEOFENCE_LIMIT_EXCEEDED, GEOFENCE_SERVICE_UNAVAILABLE, 
        GEOFENCE_PERMISSION_DENIED, GEOFENCE_REGISTRATION_FAILED
    }
    
    public enum ServiceRecoveryType {
        APP_KILLED, SYSTEM_RESTART, SERVICE_CRASHED, LOW_MEMORY
    }
    
    public enum NetworkFailureType {
        MAPS_API_FAILED, PLACES_API_FAILED, GEOCODING_FAILED, 
        NETWORK_TIMEOUT, NO_INTERNET
    }
    
    public enum SessionErrorType {
        SESSION_CREATION_FAILED, SESSION_LOAD_FAILED, TREASURE_GENERATION_FAILED,
        DATABASE_ERROR, SENSOR_ERROR
    }
    
    // Callback interfaces
    public interface PermissionDenialCallback {
        void onSettingsOpened();
        void onContinueWithoutPermission();
        void onCancel();
    }
    
    public interface GpsFailureCallback {
        void onRetryGps();
        void onContinueWithSteps();
    }
    
    public interface GeofenceFailureCallback {
        void onContinueWithManualChecking();
        void onRetryGeofences();
    }
    
    public interface ServiceRecoveryCallback {
        void onServiceRecovered();
    }
    
    public interface NetworkFailureCallback {
        void onRetryNetwork();
        void onContinueOffline();
    }
    
    public interface SessionErrorCallback {
        void onRetry();
        void onCancelSession();
        void onContinueAnyway();
    }
}