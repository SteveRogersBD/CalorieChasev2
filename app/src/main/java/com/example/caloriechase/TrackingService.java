package com.example.caloriechase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.caloriechase.data.ActiveSession;
import com.example.caloriechase.data.SessionManager;
import com.example.caloriechase.data.TreasureLocation;
import com.example.caloriechase.location.TreasureHuntLocationManager;
import com.example.caloriechase.error.ErrorHandler;
import com.example.caloriechase.error.ServiceRecoveryManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Foreground service for continuous background tracking during active sessions
 */
public class TrackingService extends Service implements SensorEventListener {
    
    private static final String TAG = "TrackingService";
    private static final String CHANNEL_ID = "tracking_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    // Intent actions
    public static final String ACTION_START_TRACKING = "com.example.caloriechase.START_TRACKING";
    public static final String ACTION_STOP_TRACKING = "com.example.caloriechase.STOP_TRACKING";
    public static final String ACTION_PAUSE_TRACKING = "com.example.caloriechase.PAUSE_TRACKING";
    public static final String ACTION_RESUME_TRACKING = "com.example.caloriechase.RESUME_TRACKING";
    
    // Broadcast actions
    public static final String BROADCAST_SESSION_UPDATE = "com.example.caloriechase.SESSION_UPDATE";
    public static final String BROADCAST_LOCATION_UPDATE = "com.example.caloriechase.LOCATION_UPDATE";
    public static final String BROADCAST_STARTING_POINT_STATUS = "com.example.caloriechase.STARTING_POINT_STATUS";
    public static final String BROADCAST_GPS_STATUS = "com.example.caloriechase.GPS_STATUS";
    
    // Intent extras
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_STEPS = "steps";
    public static final String EXTRA_DISTANCE = "distance";
    public static final String EXTRA_CALORIES = "calories";
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_AT_STARTING_POINT = "at_starting_point";
    public static final String EXTRA_DISTANCE_TO_START = "distance_to_start";
    public static final String EXTRA_GPS_ACCURACY = "gps_accuracy";
    public static final String EXTRA_USING_STEP_FALLBACK = "using_step_fallback";
    
    // Location update configuration
    private static final long LOCATION_UPDATE_INTERVAL = 5000; // 5 seconds
    private static final long LOCATION_FASTEST_INTERVAL = 2000; // 2 seconds
    private static final float LOCATION_SMALLEST_DISPLACEMENT = 2.0f; // 2 meters
    
    // Service components
    private SessionManager sessionManager;
    private TreasureHuntLocationManager treasureHuntManager;
    private SensorManager sensorManager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private NotificationManager notificationManager;
    private Handler mainHandler;
    
    // Error handling and recovery
    private ErrorHandler errorHandler;
    private ServiceRecoveryManager recoveryManager;
    
    // Tracking state
    private String currentSessionId;
    private ActiveSession currentSession;
    private boolean isTracking = false;
    private boolean isPaused = false;
    
    // Sensor data
    private Sensor stepCounterSensor;
    private Sensor stepDetectorSensor;
    private int initialStepCount = -1;
    private int currentStepCount = 0;
    private int sessionSteps = 0;
    
    // Location data
    private Location lastLocation;
    private Location startingPointLocation;
    private boolean isAtStartingPoint = false;
    private boolean useStepBasedDistance = false;
    
    // Update timing
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 2000; // 2 seconds
    private static final float GPS_ACCURACY_THRESHOLD = 20.0f; // 20 meters
    private static final float STARTING_POINT_RADIUS = 50.0f; // 50 meters
    
    // Session tracking
    private float totalDistance = 0.0f;

    
    // Binder for local service binding
    private final IBinder binder = new TrackingBinder();
    
    public class TrackingBinder extends Binder {
        public TrackingService getService() {
            return TrackingService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TrackingService created");
        
        // Initialize components
        sessionManager = SessionManager.getInstance(this);
        treasureHuntManager = new TreasureHuntLocationManager(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize error handling and recovery
        errorHandler = new ErrorHandler(this);
        recoveryManager = new ServiceRecoveryManager(this);
        
        // Initialize sensors
        initializeSensors();
        
        // Create notification channel
        createNotificationChannel();
        
        // Initialize location callback
        initializeLocationCallback();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + (intent != null ? intent.getAction() : "null"));
        
        if (intent != null) {
            String action = intent.getAction();
            String sessionId = intent.getStringExtra(EXTRA_SESSION_ID);
            
            switch (action != null ? action : "") {
                case ACTION_START_TRACKING:
                    startTracking(sessionId);
                    break;
                case ACTION_STOP_TRACKING:
                    stopTracking();
                    break;
                case ACTION_PAUSE_TRACKING:
                    pauseTracking();
                    break;
                case ACTION_RESUME_TRACKING:
                    resumeTracking();
                    break;
                default:
                    // Service restart - try to restore session
                    restoreActiveSession();
                    break;
            }
        } else {
            // Service restart - try to restore session
            restoreActiveSession();
        }
        
        // Return START_STICKY for automatic restart
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "TrackingService destroyed");
        stopTracking();
        if (sessionManager != null) {
            sessionManager.shutdown();
        }
        super.onDestroy();
    }
    
    /**
     * Initialize step counting sensors
     */
    private void initializeSensors() {
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            
            if (stepCounterSensor == null && stepDetectorSensor == null) {
                Log.w(TAG, "No step sensors available on this device");
            }
        }
    }
    
    /**
     * Initialize location callback for GPS updates
     */
    private void initializeLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || !isTracking || isPaused) {
                    return;
                }
                
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    handleLocationUpdate(location);
                }
            }
        };
    }
    
    /**
     * Start tracking for the specified session
     */
    private void startTracking(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            Log.e(TAG, "Cannot start tracking: invalid session ID");
            return;
        }
        
        Log.d(TAG, "Starting tracking for session: " + sessionId);
        currentSessionId = sessionId;
        
        // Get the active session
        sessionManager.getCurrentActiveSession(new SessionManager.SessionCallback<ActiveSession>() {
            @Override
            public void onSuccess(ActiveSession session) {
                if (session != null && session.sessionId.equals(sessionId)) {
                    currentSession = session;
                    isTracking = true;
                    isPaused = false;
                    
                    // Set starting point location
                    if (session.startLatitude != 0.0 && session.startLongitude != 0.0) {
                        startingPointLocation = new Location("StartingPoint");
                        startingPointLocation.setLatitude(session.startLatitude);
                        startingPointLocation.setLongitude(session.startLongitude);
                    }
                    
                    // Reset counters
                    resetCounters();
                    
                    // Start foreground service
                    startForeground(NOTIFICATION_ID, createNotification());
                    
                    // Start location updates
                    startLocationUpdates();
                    
                    // Start step counting
                    startStepCounting();
                    
                    // Start periodic updates
                    startPeriodicUpdates();
                    
                    // Initialize treasure hunt for this session
                    initializeTreasureHunt(sessionId);
                    
                    // Save service state for recovery
                    recoveryManager.saveServiceState(sessionId, ServiceRecoveryManager.ServiceState.ACTIVE);
                    
                    Log.d(TAG, "Tracking started successfully");
                } else {
                    Log.e(TAG, "Active session not found or session ID mismatch");
                    stopSelf();
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error getting active session", error);
                stopSelf();
            }
        });
    }
    
    /**
     * Stop tracking and clean up
     */
    private void stopTracking() {
        Log.d(TAG, "Stopping tracking");
        
        isTracking = false;
        isPaused = false;
        
        // Stop location updates
        stopLocationUpdates();
        
        // Stop step counting
        stopStepCounting();
        
        // Stop periodic updates
        stopPeriodicUpdates();
        
        // Cleanup treasure hunt
        cleanupTreasureHunt();
        
        // Clear recovery state
        recoveryManager.clearServiceState();
        
        // Clear session data
        currentSession = null;
        currentSessionId = null;
        
        // Stop foreground service
        stopForeground(true);
        stopSelf();
    }
    
    /**
     * Pause tracking
     */
    private void pauseTracking() {
        if (!isTracking || isPaused) {
            return;
        }
        
        Log.d(TAG, "Pausing tracking");
        isPaused = true;
        
        if (currentSession != null) {
            currentSession.pause();
            updateSessionInDatabase();
        }
        
        // Save paused state for recovery
        if (currentSessionId != null) {
            recoveryManager.saveServiceState(currentSessionId, ServiceRecoveryManager.ServiceState.PAUSED);
        }
        
        // Update notification
        updateNotification();
        
        // Broadcast pause state
        broadcastSessionUpdate();
    }
    
    /**
     * Resume tracking
     */
    private void resumeTracking() {
        if (!isTracking || !isPaused) {
            return;
        }
        
        Log.d(TAG, "Resuming tracking");
        isPaused = false;
        
        if (currentSession != null) {
            currentSession.resume();
            updateSessionInDatabase();
        }
        
        // Save active state for recovery
        if (currentSessionId != null) {
            recoveryManager.saveServiceState(currentSessionId, ServiceRecoveryManager.ServiceState.ACTIVE);
        }
        
        // Update notification
        updateNotification();
        
        // Broadcast resume state
        broadcastSessionUpdate();
    }
    
    /**
     * Restore active session after service restart
     */
    private void restoreActiveSession() {
        Log.d(TAG, "Attempting to restore active session");
        
        sessionManager.getCurrentActiveSession(new SessionManager.SessionCallback<ActiveSession>() {
            @Override
            public void onSuccess(ActiveSession session) {
                if (session != null) {
                    Log.d(TAG, "Restored active session: " + session.sessionId);
                    currentSession = session;
                    currentSessionId = session.sessionId;
                    isTracking = true;
                    isPaused = session.isPaused;
                    
                    // Restore counters from session
                    sessionSteps = session.currentSteps;
                    totalDistance = session.currentDistance;
                    
                    // Start foreground service
                    startForeground(NOTIFICATION_ID, createNotification());
                    
                    if (!isPaused) {
                        // Start location updates
                        startLocationUpdates();
                        
                        // Start step counting
                        startStepCounting();
                        
                        // Start periodic updates
                        startPeriodicUpdates();
                    }
                } else {
                    Log.d(TAG, "No active session to restore");
                    stopSelf();
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error restoring active session", error);
                stopSelf();
            }
        });
    }
    
    /**
     * Start location updates with error handling
     */
    private void startLocationUpdates() {
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
                    .setMinUpdateDistanceMeters(LOCATION_SMALLEST_DISPLACEMENT)
                    .build();
            
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d(TAG, "Location updates started");
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
            handleGpsFailure(ErrorHandler.GpsFailureType.GPS_PERMISSION_DENIED);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start location updates", e);
            handleGpsFailure(ErrorHandler.GpsFailureType.GPS_WEAK_SIGNAL);
        }
    }
    
    /**
     * Handle GPS failures with appropriate fallbacks
     */
    private void handleGpsFailure(ErrorHandler.GpsFailureType failureType) {
        Log.w(TAG, "GPS failure detected: " + failureType);
        useStepBasedDistance = true;
        
        // Show user notification about GPS fallback
        errorHandler.handleGpsFailure(failureType, new ErrorHandler.GpsFailureCallback() {
            @Override
            public void onRetryGps() {
                // Retry location updates after a delay
                mainHandler.postDelayed(() -> {
                    if (isTracking && !isPaused) {
                        startLocationUpdates();
                    }
                }, 5000); // 5 second delay
            }
            
            @Override
            public void onContinueWithSteps() {
                // Continue with step-based tracking
                Log.i(TAG, "Continuing with step-based distance tracking");
            }
        });
    }
    
    /**
     * Stop location updates
     */
    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped");
        }
    }
    
    /**
     * Start step counting
     */
    private void startStepCounting() {
        if (sensorManager == null) {
            return;
        }
        
        // Try step counter first (more battery efficient)
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
            Log.d(TAG, "Step counter sensor registered");
        } else if (stepDetectorSensor != null) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
            Log.d(TAG, "Step detector sensor registered");
        } else {
            Log.w(TAG, "No step sensors available");
        }
    }
    
    /**
     * Stop step counting
     */
    private void stopStepCounting() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.d(TAG, "Step sensors unregistered");
        }
    }
    
    /**
     * Start periodic updates
     */
    private void startPeriodicUpdates() {
        mainHandler.post(updateRunnable);
    }
    
    /**
     * Stop periodic updates
     */
    private void stopPeriodicUpdates() {
        mainHandler.removeCallbacks(updateRunnable);
    }
    
    /**
     * Periodic update runnable
     */
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTracking && !isPaused) {
                updateSessionMetrics();
                updateNotification();
                broadcastSessionUpdate();
            }
            
            if (isTracking) {
                mainHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };
    
    /**
     * Handle location updates
     */
    private void handleLocationUpdate(Location location) {
        if (location == null || !isTracking || isPaused) {
            return;
        }
        
        // Check GPS accuracy and decide on fallback
        boolean hasGoodGps = location.hasAccuracy() && location.getAccuracy() <= GPS_ACCURACY_THRESHOLD;
        
        // Calculate distance if we have a previous location and good GPS
        if (lastLocation != null && hasGoodGps) {
            float distance = lastLocation.distanceTo(location) / 1000.0f; // Convert to km
            
            // Only add distance if it's reasonable (not a GPS jump)
            if (distance > 0.005f && distance < 0.1f) { // Between 5m and 100m per update
                totalDistance += distance;
                useStepBasedDistance = false; // GPS is working well
            }
        } else if (!hasGoodGps && !useStepBasedDistance) {
            // Switch to step-based distance if GPS is poor
            Log.d(TAG, "GPS accuracy poor (" + (location.hasAccuracy() ? location.getAccuracy() : "unknown") + "m), switching to step-based distance");
            useStepBasedDistance = true;
        }
        
        // Check starting point proximity
        checkStartingPointProximity(location);
        
        lastLocation = location;
        
        // Check for treasure collection (proximity checking if geofences failed)
        if (currentSessionId != null) {
            treasureHuntManager.updateLocation(location, currentSessionId);
        }
        
        // Broadcast location update
        broadcastLocationUpdate(location);
        
        // Broadcast GPS status
        broadcastGpsStatus(location);
    }
    
    /**
     * Check if user is at starting point
     */
    private void checkStartingPointProximity(Location location) {
        if (startingPointLocation == null || location == null) {
            return;
        }
        
        float distanceToStart = location.distanceTo(startingPointLocation);
        boolean wasAtStartingPoint = isAtStartingPoint;
        isAtStartingPoint = distanceToStart <= STARTING_POINT_RADIUS;
        
        // Broadcast starting point status if changed
        if (isAtStartingPoint != wasAtStartingPoint) {
            broadcastStartingPointStatus(isAtStartingPoint, distanceToStart);
        }
    }
    
    /**
     * Handle sensor events (step counting)
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isTracking || isPaused) {
            return;
        }
        
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // Step counter gives total steps since boot
            int totalSteps = (int) event.values[0];
            
            if (initialStepCount == -1) {
                initialStepCount = totalSteps;
                currentStepCount = totalSteps;
            } else {
                currentStepCount = totalSteps;
                sessionSteps = currentStepCount - initialStepCount;
            }
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            // Step detector gives individual steps
            sessionSteps++;
        }
        
        // If GPS is weak, use step-based distance
        if (useStepBasedDistance && sessionSteps > 0) {
            float stepBasedDistance = FitnessTracker.calculateDistanceFromSteps(this, sessionSteps);
            // Use the greater of GPS distance or step-based distance for better accuracy
            totalDistance = Math.max(totalDistance, stepBasedDistance);
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle sensor accuracy changes if needed
    }
    
    /**
     * Reset tracking counters
     */
    private void resetCounters() {
        initialStepCount = -1;
        currentStepCount = 0;
        sessionSteps = 0;
        totalDistance = 0.0f;
        lastLocation = null;
        useStepBasedDistance = false;
    }
    
    /**
     * Update session metrics
     */
    private void updateSessionMetrics() {
        if (currentSession == null) {
            return;
        }
        
        // Update session data
        currentSession.currentSteps = sessionSteps;
        currentSession.currentDistance = totalDistance;
        
        // Calculate calories
        long durationMinutes = currentSession.getEffectiveDuration() / (1000 * 60);
        if (durationMinutes > 0 && totalDistance > 0) {
            double averageSpeedKmh = (totalDistance / durationMinutes) * 60.0;
            currentSession.caloriesBurned = FitnessTracker.calculateCalories(this, durationMinutes, averageSpeedKmh);
        } else {
            // Fallback to step-based calories
            currentSession.caloriesBurned = FitnessTracker.calculateCaloriesFromSteps(this, sessionSteps);
        }
        
        // Update in database periodically (every 10 updates to avoid excessive writes)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > 10000) { // 10 seconds
            updateSessionInDatabase();
            lastUpdateTime = currentTime;
        }
    }
    
    /**
     * Update session in database
     */
    private void updateSessionInDatabase() {
        if (currentSession != null && sessionManager != null) {
            sessionManager.updateActiveSession(currentSession, new SessionManager.VoidCallback() {
                @Override
                public void onSuccess() {
                    // Session updated successfully
                }
                
                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Error updating session in database", error);
                }
            });
        }
    }
    
    /**
     * Broadcast session update to UI components
     */
    private void broadcastSessionUpdate() {
        if (currentSession == null) {
            return;
        }
        
        Intent intent = new Intent(BROADCAST_SESSION_UPDATE);
        intent.putExtra(EXTRA_SESSION_ID, currentSession.sessionId);
        intent.putExtra(EXTRA_STEPS, sessionSteps);
        intent.putExtra(EXTRA_DISTANCE, totalDistance);
        intent.putExtra(EXTRA_CALORIES, currentSession.caloriesBurned);
        intent.putExtra(EXTRA_DURATION, currentSession.getEffectiveDuration());
        
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    /**
     * Broadcast location update
     */
    private void broadcastLocationUpdate(Location location) {
        Intent intent = new Intent(BROADCAST_LOCATION_UPDATE);
        intent.putExtra(EXTRA_LATITUDE, location.getLatitude());
        intent.putExtra(EXTRA_LONGITUDE, location.getLongitude());
        
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    /**
     * Broadcast starting point proximity status
     */
    private void broadcastStartingPointStatus(boolean atStartingPoint, float distanceToStart) {
        Intent intent = new Intent(BROADCAST_STARTING_POINT_STATUS);
        intent.putExtra(EXTRA_AT_STARTING_POINT, atStartingPoint);
        intent.putExtra(EXTRA_DISTANCE_TO_START, distanceToStart);
        
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        
        Log.d(TAG, "Starting point status: " + (atStartingPoint ? "AT" : "AWAY") + " (" + distanceToStart + "m)");
    }
    
    /**
     * Broadcast GPS status and accuracy
     */
    private void broadcastGpsStatus(Location location) {
        Intent intent = new Intent(BROADCAST_GPS_STATUS);
        
        if (location.hasAccuracy()) {
            intent.putExtra(EXTRA_GPS_ACCURACY, location.getAccuracy());
        }
        intent.putExtra(EXTRA_USING_STEP_FALLBACK, useStepBasedDistance);
        
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    /**
     * Create notification channel for Android O+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Treasure Hunt Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows progress during active treasure hunt sessions");
            channel.setShowBadge(false);
            
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Create notification for foreground service
     */
    private Notification createNotification() {
        // Intent to open ActiveSessionActivity when notification is tapped
        Intent notificationIntent = new Intent(this, ActiveSessionActivity.class);
        if (currentSessionId != null) {
            notificationIntent.putExtra(EXTRA_SESSION_ID, currentSessionId);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Treasure Hunt Active")
                .setSmallIcon(R.drawable.ic_treasure) // You'll need to add this icon
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        
        // Add content text based on current state
        if (isPaused) {
            builder.setContentText("Session paused - Tap to resume");
        } else {
            String content = String.format("Steps: %d • Distance: %.2f km • Calories: %d",
                    sessionSteps, totalDistance, 
                    currentSession != null ? currentSession.caloriesBurned : 0);
            builder.setContentText(content);
        }
        
        return builder.build();
    }
    
    /**
     * Update the existing notification
     */
    private void updateNotification() {
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification());
        }
    }
    
    // Public methods for service interaction
    
    public boolean isTracking() {
        return isTracking;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public String getCurrentSessionId() {
        return currentSessionId;
    }
    
    public ActiveSession getCurrentSession() {
        return currentSession;
    }
    
    public int getCurrentSteps() {
        return sessionSteps;
    }
    
    public float getCurrentDistance() {
        return totalDistance;
    }
    
    public Location getLastLocation() {
        return lastLocation;
    }
    
    /**
     * Retry location updates (called from TrackingServiceManager)
     */
    public void retryLocationUpdates() {
        Log.d(TAG, "Retrying location updates");
        
        if (isTracking && !isPaused) {
            // Stop current location updates
            stopLocationUpdates();
            
            // Wait a moment then restart
            mainHandler.postDelayed(() -> {
                if (isTracking && !isPaused) {
                    startLocationUpdates();
                }
            }, 2000); // 2 second delay
        }
    }
    
    /**
     * Initialize treasure hunt for the session
     */
    private void initializeTreasureHunt(String sessionId) {
        Log.d(TAG, "Initializing treasure hunt for session: " + sessionId);
        
        // Get treasures for this session
        sessionManager.getTreasuresForSession(sessionId, new SessionManager.SessionCallback<java.util.List<TreasureLocation>>() {
            @Override
            public void onSuccess(java.util.List<TreasureLocation> treasures) {
                if (treasures != null && !treasures.isEmpty()) {
                    Log.d(TAG, "Found " + treasures.size() + " treasures for session");
                    
                    // Start treasure hunt with geofences
                    treasureHuntManager.startTreasureHunt(treasures, sessionId, new TreasureHuntLocationManager.TreasureHuntCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "Treasure hunt started: " + message);
                        }
                        
                        @Override
                        public void onFailure(String error) {
                            Log.w(TAG, "Treasure hunt setup failed: " + error);
                        }
                    });
                } else {
                    Log.d(TAG, "No treasures found for session: " + sessionId);
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error getting treasures for session", error);
            }
        });
    }
    
    /**
     * Cleanup treasure hunt when stopping tracking
     */
    private void cleanupTreasureHunt() {
        if (currentSessionId != null) {
            Log.d(TAG, "Cleaning up treasure hunt for session: " + currentSessionId);
            
            treasureHuntManager.stopTreasureHunt(currentSessionId, new TreasureHuntLocationManager.TreasureHuntCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Treasure hunt stopped: " + message);
                }
                
                @Override
                public void onFailure(String error) {
                    Log.w(TAG, "Treasure hunt cleanup failed: " + error);
                }
            });
        }
        
        // Cleanup treasure hunt manager
        treasureHuntManager.cleanup();
    }
}