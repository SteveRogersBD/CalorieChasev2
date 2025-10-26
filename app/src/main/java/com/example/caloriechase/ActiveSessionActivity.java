package com.example.caloriechase;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.widget.Button;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.example.caloriechase.data.ActiveSession;
import com.example.caloriechase.data.SessionManager;
import com.example.caloriechase.data.SessionMetrics;
import com.example.caloriechase.data.SessionRecord;
import com.example.caloriechase.error.ErrorHandler;
import com.example.caloriechase.error.ServiceRecoveryManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Locale;

/**
 * ActiveSessionActivity provides full-screen map interface for treasure hunt sessions
 * with real-time tracking, GPS warm-up, countdown, and progress overlay
 */
public class ActiveSessionActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {
    
    private static final String TAG = "ActiveSessionActivity";
    
    // Intent extras
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_DISTANCE_GOAL = "distance_goal";
    public static final String EXTRA_START_LATITUDE = "start_latitude";
    public static final String EXTRA_START_LONGITUDE = "start_longitude";
    public static final String EXTRA_START_ADDRESS = "start_address";
    
    // Location tracking constants
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long LOCATION_UPDATE_INTERVAL = 5000; // 5 seconds
    private static final long LOCATION_FASTEST_INTERVAL = 2000; // 2 seconds
    private static final float MIN_DISTANCE_FOR_UPDATE = 5.0f; // 5 meters
    private static final float STARTING_POINT_RADIUS = 50.0f; // 50 meters
    
    // UI Components
    private LinearLayout llGpsWarmup;
    private LinearLayout llCountdown;
    private LinearLayout llProgressOverlay;
    private LinearLayout llStartingPointBanner;
    private LinearLayout llControlButtons;
    private LinearLayout llGpsStatus;
    
    private TextView tvWarmupStatus;
    private ProgressBar pbGpsWarmup;
    private TextView tvCountdownNumber;
    private TextView tvCountdownText;
    
    private TextView tvSessionTime;
    private TextView tvDistanceValue;
    private TextView tvStepsValue;
    private TextView tvCaloriesValue;
    private TextView tvTreasuresValue;
    private ProgressBar pbDistanceProgress;
    private TextView tvProgressText;
    
    private TextView tvGpsStatus;
    private View viewGpsIndicator;
    
    private Button btnPauseResume;
    private Button btnEndSession;
    
    // Map and Location
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Marker userMarker;
    private Marker startingPointMarker;
    
    // Session Data
    private String sessionId;
    private float distanceGoal;
    private double startLatitude;
    private double startLongitude;
    private String startAddress;
    private ActiveSession activeSession;
    private SessionManager sessionManager;
    
    // Tracking Service
    private TrackingServiceManager trackingServiceManager;
    private BroadcastReceiver sessionUpdateReceiver;
    private BroadcastReceiver locationUpdateReceiver;
    private BroadcastReceiver startingPointStatusReceiver;
    private BroadcastReceiver gpsStatusReceiver;
    
    // Tracking State
    private boolean isGpsWarmedUp = false;
    private boolean isCountdownComplete = false;
    private boolean isSessionActive = false;
    private boolean isSessionPaused = false;
    private boolean isAtStartingPoint = false;
    
    // Location tracking
    private Location lastLocation;
    private float totalDistance = 0.0f;
    private long sessionStartTime;
    private long pausedDuration = 0;
    private long lastPauseTime = 0;
    
    // Step counting
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor stepDetectorSensor;
    private int initialStepCount = -1;
    private int currentStepCount = 0;
    private boolean useStepCounter = false;
    
    // UI Update Handler
    private Handler uiUpdateHandler;
    private Runnable uiUpdateRunnable;
    
    // Auto-pause detection
    private static final long AUTO_PAUSE_THRESHOLD = 5 * 60 * 1000; // 5 minutes of inactivity
    private static final int MIN_STEPS_FOR_ACTIVITY = 10; // Minimum steps in threshold period to be considered active
    private long lastActivityTime;
    private int lastStepCountForActivity;
    private Handler autoPauseHandler;
    private Runnable autoPauseRunnable;
    
    // Error handling and recovery
    private ErrorHandler errorHandler;
    private ServiceRecoveryManager recoveryManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Keep screen on during active session
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_active_session);
        
        // Get session data from intent
        getSessionDataFromIntent();
        
        // Initialize components
        initializeViews();
        initializeLocationServices();
        initializeSensorServices();
        initializeSessionManager();
        initializeTrackingService();
        initializeErrorHandling();
        
        // Setup map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        // Check for service recovery first
        checkServiceRecovery();
        
        // Start GPS warm-up
        startGpsWarmup();
    }
    
    private void initializeErrorHandling() {
        errorHandler = new ErrorHandler(this);
        recoveryManager = new ServiceRecoveryManager(this);
    }
    
    private void checkServiceRecovery() {
        if (recoveryManager.isRecoveryNeeded()) {
            recoveryManager.attemptServiceRecovery(new ServiceRecoveryManager.ServiceRecoveryCallback() {
                @Override
                public void onRecoverySuccessful(ActiveSession recoveredSession, ErrorHandler.ServiceRecoveryType recoveryType) {
                    Log.i(TAG, "Service recovery successful: " + recoveryType);
                    activeSession = recoveredSession;
                    // Skip GPS warmup and countdown since service is already running
                    isGpsWarmedUp = true;
                    isCountdownComplete = true;
                    startActiveSession();
                }
                
                @Override
                public void onRecoveryFailed(String reason) {
                    Log.w(TAG, "Service recovery failed: " + reason);
                    // Continue with normal startup
                }
                
                @Override
                public void onRecoveryNotNeeded() {
                    Log.d(TAG, "No service recovery needed");
                    // Continue with normal startup
                }
            });
        }
    }
    
    private void getSessionDataFromIntent() {
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        distanceGoal = getIntent().getFloatExtra(EXTRA_DISTANCE_GOAL, 1.0f);
        startLatitude = getIntent().getDoubleExtra(EXTRA_START_LATITUDE, 0.0);
        startLongitude = getIntent().getDoubleExtra(EXTRA_START_LONGITUDE, 0.0);
        startAddress = getIntent().getStringExtra(EXTRA_START_ADDRESS);
        
        if (sessionId == null || sessionId.isEmpty()) {
            Toast.makeText(this, "Invalid session data", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }
    
    private void initializeViews() {
        // Overlay views
        llGpsWarmup = findViewById(R.id.ll_gps_warmup);
        llCountdown = findViewById(R.id.ll_countdown);
        llProgressOverlay = findViewById(R.id.ll_progress_overlay);
        llStartingPointBanner = findViewById(R.id.ll_starting_point_banner);
        llControlButtons = findViewById(R.id.ll_control_buttons);
        llGpsStatus = findViewById(R.id.ll_gps_status);
        
        // Warmup views
        tvWarmupStatus = findViewById(R.id.tv_warmup_status);
        pbGpsWarmup = findViewById(R.id.pb_gps_warmup);
        
        // Countdown views
        tvCountdownNumber = findViewById(R.id.tv_countdown_number);
        tvCountdownText = findViewById(R.id.tv_countdown_text);
        
        // Progress views
        tvSessionTime = findViewById(R.id.tv_session_time);
        tvDistanceValue = findViewById(R.id.tv_distance_value);
        tvStepsValue = findViewById(R.id.tv_steps_value);
        tvCaloriesValue = findViewById(R.id.tv_calories_value);
        tvTreasuresValue = findViewById(R.id.tv_treasures_value);
        pbDistanceProgress = findViewById(R.id.pb_distance_progress);
        tvProgressText = findViewById(R.id.tv_progress_text);
        
        // GPS status views
        tvGpsStatus = findViewById(R.id.tv_gps_status);
        viewGpsIndicator = findViewById(R.id.view_gps_indicator);
        
        // Control buttons
        btnPauseResume = findViewById(R.id.btn_pause_resume);
        btnEndSession = findViewById(R.id.btn_end_session);
        
        // Setup button listeners
        btnPauseResume.setOnClickListener(v -> togglePauseResume());
        btnEndSession.setOnClickListener(v -> showEndSessionDialog());
    }
    
    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Create location request for high accuracy
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
                .setMinUpdateDistanceMeters(MIN_DISTANCE_FOR_UPDATE)
                .build();
        
        // Create location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                handleLocationUpdate(locationResult.getLastLocation());
            }
        };
    }
    
    private void initializeSensorServices() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            
            // Prefer step counter over step detector for accuracy
            useStepCounter = stepCounterSensor != null;
        }
    }
    
    private void initializeSessionManager() {
        sessionManager = SessionManager.getInstance(this);
        
        // Load active session data
        sessionManager.getCurrentActiveSession(new SessionManager.SessionCallback<ActiveSession>() {
            @Override
            public void onSuccess(ActiveSession session) {
                if (session != null && session.sessionId.equals(sessionId)) {
                    activeSession = session;
                    runOnUiThread(() -> updateProgressDisplay());
                }
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    Toast.makeText(ActiveSessionActivity.this, 
                                 "Failed to load session: " + error.getMessage(), 
                                 Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }
    
    private void initializeTrackingService() {
        trackingServiceManager = new TrackingServiceManager(this);
        
        // Initialize broadcast receivers
        sessionUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleSessionUpdate(intent);
            }
        };
        
        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleLocationUpdate(intent);
            }
        };
        
        startingPointStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleStartingPointStatus(intent);
            }
        };
        
        gpsStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleGpsStatus(intent);
            }
        };
        
        // Register broadcast receivers
        LocalBroadcastManager.getInstance(this).registerReceiver(
                sessionUpdateReceiver, 
                new IntentFilter(TrackingService.BROADCAST_SESSION_UPDATE)
        );
        
        LocalBroadcastManager.getInstance(this).registerReceiver(
                locationUpdateReceiver, 
                new IntentFilter(TrackingService.BROADCAST_LOCATION_UPDATE)
        );
        
        LocalBroadcastManager.getInstance(this).registerReceiver(
                startingPointStatusReceiver, 
                new IntentFilter(TrackingService.BROADCAST_STARTING_POINT_STATUS)
        );
        
        LocalBroadcastManager.getInstance(this).registerReceiver(
                gpsStatusReceiver, 
                new IntentFilter(TrackingService.BROADCAST_GPS_STATUS)
        );
    }
    
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        
        // Configure map settings
        googleMap.getUiSettings().setAllGesturesEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        
        // Set map type to normal
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        
        // Add starting point marker
        LatLng startingPoint = new LatLng(startLatitude, startLongitude);
        startingPointMarker = googleMap.addMarker(new MarkerOptions()
                .position(startingPoint)
                .title("Starting Point")
                .snippet(startAddress != null ? startAddress : "Your starting location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        
        // Move camera to starting point
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startingPoint, 16.0f));
        
        // Enable location layer if permission granted
        enableLocationLayer();
    }
    
    private void enableLocationLayer() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(false); // We'll use custom marker
        }
    }
    
    private void startGpsWarmup() {
        tvWarmupStatus.setText("Preparing GPS...");
        llGpsWarmup.setVisibility(View.VISIBLE);
        
        // Check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        // Request high accuracy location to warm up GPS
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        handleInitialLocation(location);
                    } else {
                        // Fallback: start location updates to get a fix
                        startLocationUpdates();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (!isGpsWarmedUp) {
                                completeGpsWarmup();
                            }
                        }, 10000); // 10 second timeout
                    }
                })
                .addOnFailureListener(e -> {
                    tvWarmupStatus.setText("GPS unavailable - using step tracking");
                    new Handler(Looper.getMainLooper()).postDelayed(this::completeGpsWarmup, 2000);
                });
    }
    
    private void handleInitialLocation(Location location) {
        lastLocation = location;
        isGpsWarmedUp = true;
        
        tvWarmupStatus.setText("GPS ready!");
        
        // Update user marker on map
        updateUserMarker(location);
        
        // Check if user is at starting point
        checkStartingPointProximity(location);
        
        // Complete warmup after short delay
        new Handler(Looper.getMainLooper()).postDelayed(this::completeGpsWarmup, 1000);
    }
    
    private void completeGpsWarmup() {
        llGpsWarmup.setVisibility(View.GONE);
        startCountdown();
    }
    
    private void startCountdown() {
        llCountdown.setVisibility(View.VISIBLE);
        
        // Initialize step sensors during countdown
        initializeStepSensors();
        
        // 3-second countdown
        performCountdown(3);
    }
    
    private void performCountdown(int count) {
        if (count <= 0) {
            completeCountdown();
            return;
        }
        
        tvCountdownNumber.setText(String.valueOf(count));
        tvCountdownText.setText(count == 1 ? "Go!" : "Get ready...");
        
        // Animate countdown number
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvCountdownNumber, "scaleX", 0.5f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvCountdownNumber, "scaleY", 0.5f, 1.2f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(tvCountdownNumber, "alpha", 0.0f, 1.0f);
        
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(800);
        animatorSet.start();
        
        // Continue countdown
        new Handler(Looper.getMainLooper()).postDelayed(() -> performCountdown(count - 1), 1000);
    }
    
    private void completeCountdown() {
        llCountdown.setVisibility(View.GONE);
        isCountdownComplete = true;
        
        // Start active session
        startActiveSession();
    }
    
    private void startActiveSession() {
        isSessionActive = true;
        sessionStartTime = System.currentTimeMillis();
        
        // Show UI elements
        llProgressOverlay.setVisibility(View.VISIBLE);
        llControlButtons.setVisibility(View.VISIBLE);
        llGpsStatus.setVisibility(View.VISIBLE);
        
        // Start tracking service
        trackingServiceManager.startTracking(sessionId);
        
        // Start UI updates (for elements not handled by service broadcasts)
        startUIUpdates();
        
        // Initialize auto-pause detection
        initializeAutoPauseDetection();
        
        // Update session state in database
        if (activeSession != null) {
            activeSession.startTimestamp = sessionStartTime;
            sessionManager.updateActiveSession(activeSession, new SessionManager.VoidCallback() {
                @Override
                public void onSuccess() {
                    // Session updated successfully
                }
                
                @Override
                public void onError(Exception error) {
                    // Log error but continue session
                }
            });
        }
    }
    
    private void handleSessionUpdate(Intent intent) {
        String receivedSessionId = intent.getStringExtra(TrackingService.EXTRA_SESSION_ID);
        if (!sessionId.equals(receivedSessionId)) {
            return;
        }
        
        int steps = intent.getIntExtra(TrackingService.EXTRA_STEPS, 0);
        float distance = intent.getFloatExtra(TrackingService.EXTRA_DISTANCE, 0.0f);
        int calories = intent.getIntExtra(TrackingService.EXTRA_CALORIES, 0);
        long duration = intent.getLongExtra(TrackingService.EXTRA_DURATION, 0);
        
        // Update active session data
        if (activeSession != null) {
            activeSession.currentSteps = steps;
            activeSession.currentDistance = distance;
            activeSession.caloriesBurned = calories;
        }
        
        // Update UI with service data
        runOnUiThread(() -> {
            tvStepsValue.setText(String.valueOf(steps));
            tvDistanceValue.setText(String.format(Locale.getDefault(), "%.2f", distance));
            tvCaloriesValue.setText(String.valueOf(calories));
            tvSessionTime.setText(formatDuration(duration));
            
            // Update progress bar
            float progressPercentage = (distance / distanceGoal) * 100.0f;
            pbDistanceProgress.setProgress((int) Math.min(100, progressPercentage));
            tvProgressText.setText(String.format(Locale.getDefault(), 
                    "%.0f%% of %.1f km goal", progressPercentage, distanceGoal));
            
            // Check if distance goal has been reached
            if (distance >= distanceGoal && isSessionActive && !isSessionPaused) {
                checkDistanceGoalReached();
            }
        });
    }
    
    private void handleLocationUpdate(Intent intent) {
        double latitude = intent.getDoubleExtra(TrackingService.EXTRA_LATITUDE, 0.0);
        double longitude = intent.getDoubleExtra(TrackingService.EXTRA_LONGITUDE, 0.0);
        
        if (latitude != 0.0 && longitude != 0.0) {
            Location location = new Location("TrackingService");
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            
            runOnUiThread(() -> {
                updateUserMarker(location);
                checkStartingPointProximity(location);
                updateGpsStatus(location);
            });
        }
    }
    
    private void initializeStepSensors() {
        if (sensorManager == null) return;
        
        if (useStepCounter && stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        } else if (stepDetectorSensor != null) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }
    
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }
    
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
    
    private void handleLocationUpdate(Location location) {
        if (location == null || !isSessionActive || isSessionPaused) return;
        
        // Update user marker
        updateUserMarker(location);
        
        // Calculate distance if we have a previous location
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(location) / 1000.0f; // Convert to km
            
            // Only add distance if movement is significant (reduces GPS noise)
            if (distance > 0.005f && distance < 0.1f) { // Between 5m and 100m
                totalDistance += distance;
                updateProgressDisplay();
                
                // Detect movement activity for auto-pause
                updateActivityTime();
            }
        }
        
        // Check starting point proximity
        checkStartingPointProximity(location);
        
        // Update GPS status
        updateGpsStatus(location);
        
        lastLocation = location;
    }
    
    private void updateUserMarker(Location location) {
        if (googleMap == null) return;
        
        LatLng userPosition = new LatLng(location.getLatitude(), location.getLongitude());
        
        if (userMarker == null) {
            userMarker = googleMap.addMarker(new MarkerOptions()
                    .position(userPosition)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        } else {
            userMarker.setPosition(userPosition);
        }
        
        // Move camera to follow user (with some smoothing)
        if (isSessionActive) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(userPosition));
        }
    }
    
    private void checkStartingPointProximity(Location location) {
        LatLng startingPoint = new LatLng(startLatitude, startLongitude);
        LatLng userPosition = new LatLng(location.getLatitude(), location.getLongitude());
        
        float[] results = new float[1];
        Location.distanceBetween(startingPoint.latitude, startingPoint.longitude,
                userPosition.latitude, userPosition.longitude, results);
        
        boolean wasAtStartingPoint = isAtStartingPoint;
        isAtStartingPoint = results[0] <= STARTING_POINT_RADIUS;
        
        // Show/hide starting point banner
        if (isAtStartingPoint != wasAtStartingPoint) {
            llStartingPointBanner.setVisibility(isAtStartingPoint ? View.GONE : View.VISIBLE);
        }
    }
    
    private void updateGpsStatus(Location location) {
        if (location.hasAccuracy()) {
            float accuracy = location.getAccuracy();
            
            if (accuracy <= 10) {
                // Excellent GPS
                viewGpsIndicator.setBackgroundTintList(getColorStateList(R.color.success));
                tvGpsStatus.setText("GPS");
            } else if (accuracy <= 25) {
                // Good GPS
                viewGpsIndicator.setBackgroundTintList(getColorStateList(R.color.warning));
                tvGpsStatus.setText("GPS");
            } else {
                // Poor GPS
                viewGpsIndicator.setBackgroundTintList(getColorStateList(R.color.error));
                tvGpsStatus.setText("GPS");
            }
        }
    }
    
    private void startUIUpdates() {
        uiUpdateHandler = new Handler(Looper.getMainLooper());
        uiUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isSessionActive && !isSessionPaused) {
                    updateProgressDisplay();
                    uiUpdateHandler.postDelayed(this, 1000); // Update every second
                }
            }
        };
        uiUpdateHandler.post(uiUpdateRunnable);
    }
    
    private void stopUIUpdates() {
        if (uiUpdateHandler != null && uiUpdateRunnable != null) {
            uiUpdateHandler.removeCallbacks(uiUpdateRunnable);
        }
    }
    
    private void updateProgressDisplay() {
        if (activeSession == null) return;
        
        // Calculate current session duration
        long currentTime = System.currentTimeMillis();
        long effectiveDuration = currentTime - sessionStartTime - pausedDuration;
        if (isSessionPaused && lastPauseTime > 0) {
            effectiveDuration -= (currentTime - lastPauseTime);
        }
        
        // Update time display
        tvSessionTime.setText(formatDuration(effectiveDuration));
        
        // Update distance (use GPS distance or step-based fallback)
        float displayDistance = totalDistance;
        if (totalDistance < 0.01f && currentStepCount > 0) {
            // Fallback to step-based distance if GPS distance is minimal
            displayDistance = FitnessTracker.calculateDistanceFromSteps(this, currentStepCount);
        }
        tvDistanceValue.setText(String.format(Locale.getDefault(), "%.2f", displayDistance));
        
        // Update steps
        tvStepsValue.setText(String.valueOf(currentStepCount));
        
        // Update calories
        int calories = FitnessTracker.calculateCaloriesFromSteps(this, currentStepCount);
        tvCaloriesValue.setText(String.valueOf(calories));
        
        // Update treasures (placeholder - will be updated by geofence events)
        tvTreasuresValue.setText("0");
        
        // Update progress bar
        float progressPercentage = (displayDistance / distanceGoal) * 100.0f;
        pbDistanceProgress.setProgress((int) Math.min(100, progressPercentage));
        tvProgressText.setText(String.format(Locale.getDefault(), 
                "%.0f%% of %.1f km goal", progressPercentage, distanceGoal));
        
        // Update active session data
        activeSession.currentDistance = displayDistance;
        activeSession.currentSteps = currentStepCount;
        activeSession.caloriesBurned = calories;
        
        // Check if distance goal has been reached
        if (displayDistance >= distanceGoal && isSessionActive && !isSessionPaused) {
            checkDistanceGoalReached();
        }
    }
    
    private String formatDuration(long durationMillis) {
        long seconds = durationMillis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    private void togglePauseResume() {
        if (isSessionPaused) {
            resumeSession();
        } else {
            pauseSession();
        }
    }
    
    private void pauseSession() {
        isSessionPaused = true;
        lastPauseTime = System.currentTimeMillis();
        
        // Update UI
        btnPauseResume.setText("Resume");
        
        // Pause tracking service
        trackingServiceManager.pauseTracking();
        
        // Update session in database
        if (activeSession != null) {
            sessionManager.pauseSession(sessionId, new SessionManager.VoidCallback() {
                @Override
                public void onSuccess() {
                    // Session paused successfully
                }
                
                @Override
                public void onError(Exception error) {
                    // Log error but continue
                }
            });
        }
        
        // Stop UI updates
        stopUIUpdates();
        
        // Stop location updates to save battery
        stopLocationUpdates();
        
        // Stop auto-pause detection while paused
        stopAutoPauseDetection();
        
        // Show pause overlay or update UI to indicate paused state
        showPauseIndicator();
    }
    
    private void resumeSession() {
        isSessionPaused = false;
        lastPauseTime = 0;
        
        // Update UI
        btnPauseResume.setText("Pause");
        hidePauseIndicator();
        
        // Resume tracking service
        trackingServiceManager.resumeTracking();
        
        // Update session in database
        if (activeSession != null) {
            sessionManager.resumeSession(sessionId, new SessionManager.VoidCallback() {
                @Override
                public void onSuccess() {
                    // Session resumed successfully
                }
                
                @Override
                public void onError(Exception error) {
                    // Log error but continue
                }
            });
        }
        
        // Restart UI updates
        startUIUpdates();
        
        // Resume location updates if needed
        startLocationUpdates();
        
        // Restart auto-pause detection
        initializeAutoPauseDetection();
    }
    
    private void showEndSessionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("End Session")
                .setMessage("Are you sure you want to end your treasure hunt session? Your progress will be saved.")
                .setPositiveButton("End Session", (dialog, which) -> endSession())
                .setNegativeButton("Continue", null)
                .show();
    }
    
    private void endSession() {
        if (!isSessionActive) return;
        
        // Stop tracking service
        trackingServiceManager.stopTracking();
        
        // Stop all updates and cleanup
        stopUIUpdates();
        stopLocationUpdates();
        stopAutoPauseDetection();
        
        // Mark session as inactive
        isSessionActive = false;
        
        // Finalize session in database
        finalizeSession();
    }
    
    /**
     * Finalize the session and navigate to summary
     */
    private void finalizeSession() {
        if (sessionManager == null || sessionId == null) {
            Toast.makeText(this, "Error finalizing session", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        sessionManager.finalizeSession(sessionId, new SessionManager.SessionCallback<SessionRecord>() {
            @Override
            public void onSuccess(SessionRecord sessionRecord) {
                runOnUiThread(() -> {
                    // Clean up geofences
                    cleanupGeofences();
                    
                    // Navigate to summary activity
                    Intent intent = new Intent(ActiveSessionActivity.this, SessionSummaryActivity.class);
                    intent.putExtra(SessionSummaryActivity.EXTRA_SESSION_ID, sessionId);
                    startActivity(intent);
                    finish();
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    Toast.makeText(ActiveSessionActivity.this, 
                                 "Error saving session: " + error.getMessage(), 
                                 Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }
    
    /**
     * Clean up geofences for the completed session
     */
    private void cleanupGeofences() {
        // Use GeofenceManager to remove geofences
        com.example.caloriechase.location.GeofenceManager geofenceManager = 
            new com.example.caloriechase.location.GeofenceManager(this);
        
        geofenceManager.removeGeofences(sessionId, new com.example.caloriechase.location.GeofenceManager.GeofenceCallback() {
            @Override
            public void onSuccess(int geofenceCount) {
                // Geofences cleaned up successfully
            }
            
            @Override
            public void onFailure(String error) {
                // Log error but don't block completion
            }
        });
    }
    
    /**
     * Check if distance goal has been reached and auto-end if so
     */
    private void checkDistanceGoalReached() {
        if (activeSession != null && activeSession.currentDistance >= distanceGoal) {
            // Goal reached - show completion dialog
            showGoalReachedDialog();
        }
    }
    
    /**
     * Show goal reached dialog
     */
    private void showGoalReachedDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Goal Achieved! 🎉")
                .setMessage("Congratulations! You've reached your distance goal. Would you like to end your session now?")
                .setPositiveButton("End Session", (dialog, which) -> endSession())
                .setNegativeButton("Continue", null)
                .setCancelable(false)
                .show();
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isSessionActive || isSessionPaused) return;
        
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // Step counter gives total steps since boot
            if (initialStepCount == -1) {
                initialStepCount = (int) event.values[0];
                currentStepCount = 0;
            } else {
                int previousStepCount = currentStepCount;
                currentStepCount = (int) event.values[0] - initialStepCount;
                
                // Detect activity for auto-pause
                if (currentStepCount > previousStepCount) {
                    updateActivityTime();
                }
            }
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            // Step detector gives individual step events
            currentStepCount++;
            
            // Detect activity for auto-pause
            updateActivityTime();
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle sensor accuracy changes if needed
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, continue with GPS warmup
                startGpsWarmup();
            } else {
                // Permission denied, show message and continue with step-only tracking
                tvWarmupStatus.setText("Location permission denied - using step tracking only");
                new Handler(Looper.getMainLooper()).postDelayed(this::completeGpsWarmup, 2000);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup tracking service manager
        if (trackingServiceManager != null) {
            trackingServiceManager.cleanup();
        }
        
        // Unregister broadcast receivers
        if (sessionUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(sessionUpdateReceiver);
        }
        if (locationUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(locationUpdateReceiver);
        }
        if (startingPointStatusReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(startingPointStatusReceiver);
        }
        if (gpsStatusReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsStatusReceiver);
        }
        
        // Stop location updates
        stopLocationUpdates();
        
        // Stop UI updates
        stopUIUpdates();
        
        // Stop auto-pause detection
        stopAutoPauseDetection();
        
        // Unregister sensors
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        
        // Clear screen on flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Keep tracking active in background via service - don't pause the session
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Resume UI updates if session is active
        if (isSessionActive && !isSessionPaused) {
            startUIUpdates();
        }
    }
    
    /**
     * Handle starting point proximity status updates
     */
    private void handleStartingPointStatus(Intent intent) {
        boolean atStartingPoint = intent.getBooleanExtra(TrackingService.EXTRA_AT_STARTING_POINT, false);
        float distanceToStart = intent.getFloatExtra(TrackingService.EXTRA_DISTANCE_TO_START, 0.0f);
        
        runOnUiThread(() -> {
            if (atStartingPoint) {
                // User is at starting point - hide banner
                llStartingPointBanner.setVisibility(View.GONE);
            } else {
                // User is away from starting point - show guidance banner
                llStartingPointBanner.setVisibility(View.VISIBLE);
                // You could add a TextView to show distance to starting point
            }
        });
    }
    
    /**
     * Handle GPS status updates
     */
    private void handleGpsStatus(Intent intent) {
        float accuracy = intent.getFloatExtra(TrackingService.EXTRA_GPS_ACCURACY, -1.0f);
        boolean usingStepFallback = intent.getBooleanExtra(TrackingService.EXTRA_USING_STEP_FALLBACK, false);
        
        runOnUiThread(() -> {
            if (usingStepFallback) {
                // Using step-based distance fallback
                viewGpsIndicator.setBackgroundTintList(getColorStateList(R.color.warning));
                tvGpsStatus.setText("STEPS");
            } else if (accuracy > 0) {
                if (accuracy <= 10) {
                    // Excellent GPS
                    viewGpsIndicator.setBackgroundTintList(getColorStateList(R.color.success));
                    tvGpsStatus.setText("GPS");
                } else if (accuracy <= 25) {
                    // Good GPS
                    viewGpsIndicator.setBackgroundTintList(getColorStateList(R.color.warning));
                    tvGpsStatus.setText("GPS");
                } else {
                    // Poor GPS
                    viewGpsIndicator.setBackgroundTintList(getColorStateList(R.color.error));
                    tvGpsStatus.setText("GPS");
                }
            }
        });
    }
    
    @Override
    public void onBackPressed() {
        // Prevent accidental back press during active session
        if (isSessionActive) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Exit Session")
                    .setMessage("Your session is still active. Do you want to pause it and return to the home screen?")
                    .setPositiveButton("Pause & Exit", (dialog, which) -> {
                        if (!isSessionPaused) {
                            pauseSession();
                        }
                        super.onBackPressed();
                    })
                    .setNegativeButton("Continue Session", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
    

    
    /**
     * Show pause indicator in UI
     */
    private void showPauseIndicator() {
        runOnUiThread(() -> {
            if (tvSessionTime != null) {
                String currentText = tvSessionTime.getText().toString();
                if (!currentText.contains(" (PAUSED)")) {
                    tvSessionTime.setText(currentText + " (PAUSED)");
                }
            }
        });
    }
    
    /**
     * Hide pause indicator from UI
     */
    private void hidePauseIndicator() {
        runOnUiThread(() -> {
            if (tvSessionTime != null) {
                String currentText = tvSessionTime.getText().toString();
                if (currentText.contains(" (PAUSED)")) {
                    tvSessionTime.setText(currentText.replace(" (PAUSED)", ""));
                }
            }
        });
    }
    
    /**
     * Initialize auto-pause detection
     */
    private void initializeAutoPauseDetection() {
        lastActivityTime = System.currentTimeMillis();
        lastStepCountForActivity = currentStepCount;
        
        autoPauseHandler = new Handler(Looper.getMainLooper());
        autoPauseRunnable = new Runnable() {
            @Override
            public void run() {
                if (isSessionActive && !isSessionPaused) {
                    checkForInactivity();
                    // Check every minute
                    autoPauseHandler.postDelayed(this, 60000);
                }
            }
        };
        
        // Start checking after the first minute
        autoPauseHandler.postDelayed(autoPauseRunnable, 60000);
    }
    
    /**
     * Check for user inactivity and auto-pause if needed
     */
    private void checkForInactivity() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastActivity = currentTime - lastActivityTime;
        
        // Check if user has been inactive for the threshold period
        if (timeSinceLastActivity >= AUTO_PAUSE_THRESHOLD) {
            // Check if there's been minimal step activity
            int stepsSinceLastCheck = currentStepCount - lastStepCountForActivity;
            
            if (stepsSinceLastCheck < MIN_STEPS_FOR_ACTIVITY) {
                // User appears to be inactive - auto-pause
                showAutoPauseDialog();
            } else {
                // User is still active - update activity time
                updateActivityTime();
            }
        }
    }
    
    /**
     * Update the last activity time (called when user shows activity)
     */
    private void updateActivityTime() {
        lastActivityTime = System.currentTimeMillis();
        lastStepCountForActivity = currentStepCount;
    }
    
    /**
     * Show auto-pause dialog to user
     */
    private void showAutoPauseDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Auto-Pause Detected")
                .setMessage("You seem to have been inactive for a while. Would you like to pause your session?")
                .setPositiveButton("Pause Session", (dialog, which) -> {
                    pauseSession();
                })
                .setNegativeButton("Continue", (dialog, which) -> {
                    // User wants to continue - reset activity timer
                    updateActivityTime();
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * Stop auto-pause detection
     */
    private void stopAutoPauseDetection() {
        if (autoPauseHandler != null && autoPauseRunnable != null) {
            autoPauseHandler.removeCallbacks(autoPauseRunnable);
        }
    }
    

    
    private boolean hasShownGpsFallbackNotification = false;
    
    /**
     * Update GPS status indicator based on accuracy
     */
    private void updateGpsStatusIndicator(float accuracy) {
        if (accuracy <= 10) {
            // Excellent GPS
            viewGpsIndicator.setBackgroundTintList(getColorStateList(R.color.success));
            tvGpsStatus.setText("GPS");
        } else if (accuracy <= 25) {
            // Good GPS
            viewGpsIndicator.setBackgroundTintList(getColorStateList(R.color.warning));
            tvGpsStatus.setText("GPS");
        } else {
            // Poor GPS
            viewGpsIndicator.setBackgroundTintList(getColorStateList(R.color.error));
            tvGpsStatus.setText("GPS");
        }
    }
    
    /**
     * Handle session errors with recovery options
     */
    private void handleSessionError(Exception error, String context) {
        Log.e(TAG, "Session error in " + context, error);
        
        ErrorHandler.SessionErrorType errorType;
        if (context.contains("load")) {
            errorType = ErrorHandler.SessionErrorType.SESSION_LOAD_FAILED;
        } else if (context.contains("sensor")) {
            errorType = ErrorHandler.SessionErrorType.SENSOR_ERROR;
        } else if (context.contains("database")) {
            errorType = ErrorHandler.SessionErrorType.DATABASE_ERROR;
        } else {
            errorType = ErrorHandler.SessionErrorType.SESSION_LOAD_FAILED;
        }
        
        errorHandler.handleSessionError(errorType, error.getMessage(), 
                new ErrorHandler.SessionErrorCallback() {
            @Override
            public void onRetry() {
                // Attempt to restart the session
                restartSession();
            }
            
            @Override
            public void onCancelSession() {
                // End the session and return to dashboard
                forceEndSession();
            }
            
            @Override
            public void onContinueAnyway() {
                // Continue with limited functionality
                Toast.makeText(ActiveSessionActivity.this, 
                             "Continuing with limited functionality", 
                             Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * Restart the session after an error
     */
    private void restartSession() {
        try {
            // Stop current tracking
            if (trackingServiceManager != null) {
                trackingServiceManager.stopTracking();
            }
            
            // Wait a moment then restart
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (sessionId != null) {
                    trackingServiceManager.startTracking(sessionId);
                }
            }, 2000);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart session", e);
            Toast.makeText(this, "Failed to restart session", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Force end the session due to unrecoverable error
     */
    private void forceEndSession() {
        try {
            // Stop tracking service
            if (trackingServiceManager != null) {
                trackingServiceManager.stopTracking();
            }
            
            // Clear recovery state
            if (recoveryManager != null) {
                recoveryManager.clearServiceState();
            }
            
            // Return to dashboard
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error during force end session", e);
            // Force close the activity
            finish();
        }
    }

}