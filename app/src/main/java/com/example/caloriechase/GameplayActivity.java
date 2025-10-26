package com.example.caloriechase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

/**
 * Pokemon Go-style gameplay activity with live tracking and geofencing
 */
public class GameplayActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {
    
    private static final String TAG = "GameplayActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final String GEOFENCE_ACTION = "com.example.caloriechase.GEOFENCE_TRIGGERED";
    
    // UI Components
    private GoogleMap mMap;
    private TextView tvScore;
    private TextView tvTreasuresCollected;
    private TextView tvTotalTreasures;
    private TextView tvDistance;
    private TextView tvTimer;
    private MaterialCardView scoreCard;
    private FloatingActionButton fabEndSession;
    
    // Location and Geofencing
    private FusedLocationProviderClient fusedLocationClient;
    private GeofencingClient geofencingClient;
    private LocationCallback locationCallback;
    private List<Geofence> geofenceList;
    private PendingIntent geofencePendingIntent;
    
    // Game Data
    private List<LatLng> treasureLocations;
    private List<Marker> treasureMarkers;
    private List<Boolean> treasuresCollected;
    private Marker playerMarker;
    private Circle playerRadius;
    private int score = 0;
    private int treasuresFound = 0;
    private float totalDistance = 0f;
    private Location lastLocation;
    private List<LatLng> trackPath;
    
    // Step tracking with real sensor
    private int totalSteps = 0;
    private int initialStepCount = 0;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private boolean sensorAvailable = false;
    
    // User profile data for calorie calculation
    private float userWeight = 70f; // kg
    private float userHeight = 170f; // cm
    private String userGender = "male";
    private int userAge = 30;
    
    // Calorie tracking
    private int caloriesBurned = 0;
    private List<Location> locationHistory;
    
    // Timer
    private long sessionStartTime;
    private android.os.Handler timerHandler;
    private Runnable timerRunnable;
    
    // Session Data
    private double startLatitude;
    private double startLongitude;
    private float distanceGoal;
    private String startAddress;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "GameplayActivity onCreate started");
            setContentView(R.layout.activity_gameplay);
            
            // Enable edge-to-edge display
            enableEdgeToEdge();
            
            // Get session data from intent
            Intent intent = getIntent();
            startLatitude = intent.getDoubleExtra("start_latitude", 0);
            startLongitude = intent.getDoubleExtra("start_longitude", 0);
            distanceGoal = intent.getFloatExtra("distance_goal", 1.0f);
            startAddress = intent.getStringExtra("start_address");
            
            Log.d(TAG, "Session data: lat=" + startLatitude + ", lng=" + startLongitude + ", distance=" + distanceGoal);
            
            // Get treasure locations from intent (passed from SessionMapActivity)
            double[] latitudes = intent.getDoubleArrayExtra("treasure_latitudes");
            double[] longitudes = intent.getDoubleArrayExtra("treasure_longitudes");
            
            // Get track path from intent
            double[] trackLats = intent.getDoubleArrayExtra("track_latitudes");
            double[] trackLngs = intent.getDoubleArrayExtra("track_longitudes");
            
            Log.d(TAG, "Received treasures: " + (latitudes != null ? latitudes.length : 0));
            Log.d(TAG, "Received track points: " + (trackLats != null ? trackLats.length : 0));
            
            // Load user profile data
            loadUserProfile();
            
            // Initialize location history
            locationHistory = new ArrayList<>();
            
            // Initialize step counter sensor
            initStepCounter();
            
            initViews();
            initLocationServices();
            initTreasureData(latitudes, longitudes);
            initTrackPath(trackLats, trackLngs);
            setupMap();
            registerGeofenceReceiver();
            
            Log.d(TAG, "GameplayActivity onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error starting gameplay: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    /**
     * Load user profile data from SharedPreferences
     */
    private void loadUserProfile() {
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        userWeight = prefs.getFloat("weight", 70f); // Default 70kg
        userHeight = prefs.getFloat("height", 170f); // Default 170cm
        userGender = prefs.getString("gender", "male"); // Default male
        userAge = prefs.getInt("age", 30); // Default 30 years
        
        Log.d(TAG, "User profile loaded: weight=" + userWeight + "kg, height=" + userHeight + 
              "cm, gender=" + userGender + ", age=" + userAge);
    }
    
    /**
     * Initialize step counter sensor
     */
    private void initStepCounter() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            
            if (stepCounterSensor != null) {
                sensorAvailable = true;
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
                Log.d(TAG, "Step counter sensor registered");
            } else {
                Log.w(TAG, "Step counter sensor not available, will estimate from GPS");
                sensorAvailable = false;
            }
        }
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount == 0) {
                // First reading - set as baseline
                initialStepCount = (int) event.values[0];
                Log.d(TAG, "Initial step count: " + initialStepCount);
            } else {
                // Calculate steps since session started
                totalSteps = (int) event.values[0] - initialStepCount;
                
                // Calculate calories based on real steps
                calculateCalories();
                
                Log.d(TAG, "Steps: " + totalSteps + ", Calories: " + caloriesBurned);
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for step counter
    }
    
    /**
     * Calculate calories burned based on user profile, steps, distance, and pace
     */
    private void calculateCalories() {
        if (totalSteps == 0 && totalDistance == 0) {
            caloriesBurned = 0;
            return;
        }
        
        // Calculate MET (Metabolic Equivalent of Task) based on pace
        float met = calculateMET();
        
        // Calculate time in hours
        long sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000; // seconds
        float hours = sessionDuration / 3600f;
        
        // Calorie formula: Calories = MET × weight(kg) × time(hours)
        // Adjusted for gender and age
        float baseCalories = met * userWeight * hours;
        
        // Gender adjustment (women typically burn ~5% fewer calories)
        float genderMultiplier = userGender.equalsIgnoreCase("female") ? 0.95f : 1.0f;
        
        // Age adjustment (metabolism decreases ~2% per decade after 30)
        float ageMultiplier = 1.0f - Math.max(0, (userAge - 30) / 10) * 0.02f;
        
        caloriesBurned = (int) (baseCalories * genderMultiplier * ageMultiplier);
        
        Log.d(TAG, "Calories calculated: MET=" + met + ", base=" + baseCalories + 
              ", final=" + caloriesBurned);
    }
    
    /**
     * Calculate MET value based on pace and activity
     */
    private float calculateMET() {
        if (totalDistance == 0 || totalSteps == 0) {
            return 3.0f; // Slow walking
        }
        
        // Calculate pace (km/h)
        long sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000; // seconds
        if (sessionDuration == 0) return 3.0f;
        
        float distanceKm = totalDistance / 1000f;
        float hours = sessionDuration / 3600f;
        float pace = distanceKm / hours; // km/h
        
        // MET values based on pace
        // Walking: 2.0 mph (3.2 km/h) = 2.5 MET
        // Walking: 3.0 mph (4.8 km/h) = 3.5 MET
        // Walking: 4.0 mph (6.4 km/h) = 5.0 MET
        // Jogging: 5.0 mph (8.0 km/h) = 8.0 MET
        // Running: 6.0 mph (9.7 km/h) = 10.0 MET
        
        if (pace < 3.2f) {
            return 2.5f; // Slow walking
        } else if (pace < 4.8f) {
            return 3.5f; // Normal walking
        } else if (pace < 6.4f) {
            return 5.0f; // Brisk walking
        } else if (pace < 8.0f) {
            return 8.0f; // Jogging
        } else {
            return 10.0f; // Running
        }
    }
    
    private void initViews() {
        Log.d(TAG, "Initializing views");
        
        tvScore = findViewById(R.id.tv_score);
        tvTreasuresCollected = findViewById(R.id.tv_treasures_collected);
        tvTotalTreasures = findViewById(R.id.tv_total_treasures);
        tvDistance = findViewById(R.id.tv_distance);
        tvTimer = findViewById(R.id.tv_timer);
        scoreCard = findViewById(R.id.score_card);
        fabEndSession = findViewById(R.id.fab_end_session);
        
        // Check for null views
        if (tvScore == null || tvTreasuresCollected == null || tvTotalTreasures == null || 
            tvDistance == null || tvTimer == null || fabEndSession == null) {
            Log.e(TAG, "One or more views are null!");
            throw new RuntimeException("Failed to initialize views - check layout file");
        }
        
        // Apply system bar insets to instructions card
        MaterialCardView instructionsCard = findViewById(R.id.instructions_card);
        if (instructionsCard != null) {
            applySystemBarInsets(instructionsCard);
        }
        
        fabEndSession.setOnClickListener(v -> endSession());
        
        updateScoreDisplay();
        startTimer();
        
        Log.d(TAG, "Views initialized successfully");
    }
    
    private void startTimer() {
        sessionStartTime = System.currentTimeMillis();
        timerHandler = new android.os.Handler();
        
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedMillis = System.currentTimeMillis() - sessionStartTime;
                int seconds = (int) (elapsedMillis / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;
                seconds = seconds % 60;
                minutes = minutes % 60;
                
                tvTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                
                timerHandler.postDelayed(this, 1000); // Update every second
            }
        };
        
        timerHandler.post(timerRunnable);
        Log.d(TAG, "Session timer started");
    }
    
    private void initLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceList = new ArrayList<>();
        
        // Setup location callback for live tracking
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updatePlayerLocation(location);
                }
            }
        };
    }
    
    private void initTreasureData(double[] latitudes, double[] longitudes) {
        treasureLocations = new ArrayList<>();
        treasureMarkers = new ArrayList<>();
        treasuresCollected = new ArrayList<>();
        
        if (latitudes != null && longitudes != null) {
            for (int i = 0; i < Math.min(latitudes.length, longitudes.length); i++) {
                treasureLocations.add(new LatLng(latitudes[i], longitudes[i]));
                treasuresCollected.add(false);
            }
        }
        
        tvTotalTreasures.setText(String.valueOf(treasureLocations.size()));
    }
    
    private void initTrackPath(double[] trackLats, double[] trackLngs) {
        trackPath = new ArrayList<>();
        
        if (trackLats != null && trackLngs != null) {
            for (int i = 0; i < Math.min(trackLats.length, trackLngs.length); i++) {
                trackPath.add(new LatLng(trackLats[i], trackLngs[i]));
            }
        }
        
        Log.d(TAG, "Track path initialized with " + trackPath.size() + " points");
    }
    
    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        // Configure map for gameplay
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true); // Enable zoom controls
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true); // Enable my location button
        mMap.getUiSettings().setZoomGesturesEnabled(true); // Enable pinch to zoom
        mMap.getUiSettings().setScrollGesturesEnabled(true); // Enable panning
        
        drawTrackPath();
        setupTreasureMarkers();
        setupGeofences();
        
        // Wait for map to be laid out before fitting camera
        mMap.setOnMapLoadedCallback(() -> {
            Log.d(TAG, "Map loaded, now fitting camera");
            
            // Enable my location if permission granted
            if (checkLocationPermission()) {
                enableMyLocation();
            } else {
                requestLocationPermission();
            }
            
            startLocationUpdates();
        });
    }
    
    private void drawTrackPath() {
        if (trackPath == null || trackPath.isEmpty()) {
            Log.w(TAG, "No track path to draw");
            return;
        }
        
        // Draw the track path as a polyline
        com.google.android.gms.maps.model.PolylineOptions polylineOptions = 
            new com.google.android.gms.maps.model.PolylineOptions()
                .addAll(trackPath)
                .color(ContextCompat.getColor(this, R.color.primary_orange))
                .width(8f)
                .clickable(false);
        
        mMap.addPolyline(polylineOptions);
        
        Log.d(TAG, "Track path drawn with " + trackPath.size() + " points");
    }
    
    private void setupTreasureMarkers() {
        BitmapDescriptor goldIcon = createScaledGoldIcon();
        
        for (int i = 0; i < treasureLocations.size(); i++) {
            LatLng location = treasureLocations.get(i);
            
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Treasure " + (i + 1))
                    .snippet("Walk here to collect!")
                    .icon(goldIcon));
            
            treasureMarkers.add(marker);
        }
    }

    private void setupGeofences() {
        List<Geofence> geofenceList = buildGeofencesFromExtras();
        if (geofenceList.isEmpty()) {
            Log.e("Gameplay", "No geofences built — skipping.");
            return;
        }
        addGeofences(geofenceList);
    }

    private List<Geofence> buildGeofencesFromExtras() {
        List<Geofence> geofenceList = new ArrayList<>();

        Intent intent = getIntent();
        double[] latitudes = intent.getDoubleArrayExtra("treasure_latitudes");
        double[] longitudes = intent.getDoubleArrayExtra("treasure_longitudes");

        // safety check
        if (latitudes == null || longitudes == null || latitudes.length == 0 || longitudes.length == 0) {
            Log.e("Gameplay", "No treasure locations found in intent extras");
            return geofenceList;
        }

        // you can tune the radius as per your gameplay distance
        float radiusMeters = 30f; // 30 m radius for each treasure spot

        for (int i = 0; i < latitudes.length; i++) {
            LatLng pos = new LatLng(latitudes[i], longitudes[i]);

            geofenceList.add(
                    new Geofence.Builder()
                            .setRequestId("treasure_" + i)
                            .setCircularRegion(
                                    pos.latitude,
                                    pos.longitude,
                                    radiusMeters
                            )
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                            .build()
            );
        }

        Log.d("Gameplay", "Built " + geofenceList.size() + " geofences.");
        return geofenceList;
    }



    private void addGeofences(List<Geofence> geofenceList) {
        if (geofenceList == null || geofenceList.isEmpty()) {
            Log.e("Gameplay", "addGeofences called with empty list");
            return;
        }

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build();

        PendingIntent pi = getGeofencePendingIntent(); // your receiver PI

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // request FINE (and BACKGROUND on Android 10+) before adding
            return;
        }
        geofencingClient.addGeofences(request, pi)
                .addOnSuccessListener(v -> Log.d("Gameplay", "Geofences added"))
                .addOnFailureListener(e -> Log.e("Gameplay", "Failed to add geofences", e));
    }


    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        
        // Make Intent explicit by specifying the component class (required for Android 14+)
        Intent intent = new Intent(this, GeofenceReceiver.class);
        intent.setAction(GEOFENCE_ACTION);
        
        // Android 12+ (API 31+) requires FLAG_IMMUTABLE for security
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);
        return geofencePendingIntent;
    }
    
    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        mMap.setMyLocationEnabled(true);
        
        // Get current location and fit camera to show all treasures + player
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        updatePlayerLocation(location);
                        fitCameraToShowAllTreasuresAndPlayer(currentLocation);
                    }
                });
    }
    
    private void fitCameraToShowAllTreasuresAndPlayer(LatLng playerLocation) {
        if (mMap == null) return;
        
        if (treasureLocations == null || treasureLocations.isEmpty()) {
            // No treasures, just zoom to player
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(playerLocation, 16f));
            return;
        }
        
        // Build bounds that include player position and all treasures
        com.google.android.gms.maps.model.LatLngBounds.Builder boundsBuilder = 
            new com.google.android.gms.maps.model.LatLngBounds.Builder();
        
        // Include player location
        boundsBuilder.include(playerLocation);
        
        // Include all treasure locations
        for (LatLng treasure : treasureLocations) {
            boundsBuilder.include(treasure);
        }
        
        // Also include track path points for better framing
        if (trackPath != null && !trackPath.isEmpty()) {
            for (LatLng point : trackPath) {
                boundsBuilder.include(point);
            }
        }
        
        com.google.android.gms.maps.model.LatLngBounds bounds = boundsBuilder.build();
        
        // Add padding (150 pixels) to ensure all markers are visible
        int padding = 150;
        
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding), 2000, null);
            Log.d(TAG, "Camera fitted to show all " + treasureLocations.size() + " treasures and player");
        } catch (IllegalStateException e) {
            // Map not ready yet, try with moveCamera instead
            Log.w(TAG, "Map not ready for animateCamera, using moveCamera");
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }
    }
    
    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!checkLocationPermission()) return;
        
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000) // Update every 2 seconds
                .setFastestInterval(1000); // Fastest update every 1 second
        
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }
    
    private void updatePlayerLocation(Location location) {
        if (mMap == null) return;
        
        LatLng playerLocation = new LatLng(location.getLatitude(), location.getLongitude());
        
        // Add to location history for accurate distance tracking
        locationHistory.add(location);
        
        // Update player marker
        if (playerMarker != null) {
            playerMarker.remove();
        }
        
        playerMarker = mMap.addMarker(new MarkerOptions()
                .position(playerLocation)
                .title("You are here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        
        // Update player radius circle (collection radius)
        if (playerRadius != null) {
            playerRadius.remove();
        }
        
        playerRadius = mMap.addCircle(new CircleOptions()
                .center(playerLocation)
                .radius(20f) // 20 meter collection radius
                .strokeColor(ContextCompat.getColor(this, R.color.accent_blue))
                .fillColor(ContextCompat.getColor(this, R.color.accent_blue) & 0x30FFFFFF)
                .strokeWidth(2f));
        
        // Calculate distance traveled from GPS
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(location);
            
            // Only count if movement is significant (> 1 meter) to avoid GPS jitter
            if (distance > 1.0f && distance < 100f) { // Also filter out GPS jumps
                totalDistance += distance;
                
                // If sensor not available, estimate steps from distance
                if (!sensorAvailable) {
                    // Average stride length is ~0.75 meters
                    int estimatedSteps = (int) (distance / 0.75f);
                    totalSteps += estimatedSteps;
                }
                
                // Recalculate calories with new distance
                calculateCalories();
                
                updateDistanceDisplay();
            }
        }
        
        lastLocation = location;
        
        // Check for nearby treasures within collection radius
        checkNearbyTreasures(playerLocation);
        
        // Smoothly pan camera to keep player in view (don't re-fit bounds on every update)
        mMap.animateCamera(CameraUpdateFactory.newLatLng(playerLocation));
    }
    
    /**
     * Check if any treasures are within collection radius and collect them
     */
    private void checkNearbyTreasures(LatLng playerLocation) {
        if (treasureLocations == null || treasureLocations.isEmpty()) return;
        
        float collectionRadius = 20f; // 20 meters collection radius
        
        for (int i = 0; i < treasureLocations.size(); i++) {
            // Skip already collected treasures
            if (treasuresCollected.get(i)) continue;
            
            LatLng treasureLocation = treasureLocations.get(i);
            
            // Calculate distance between player and treasure
            float[] results = new float[1];
            Location.distanceBetween(
                playerLocation.latitude, playerLocation.longitude,
                treasureLocation.latitude, treasureLocation.longitude,
                results
            );
            
            float distanceToTreasure = results[0];
            
            // Check if treasure is within collection radius
            if (distanceToTreasure <= collectionRadius) {
                collectTreasure(i);
            }
        }
    }
    
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerGeofenceReceiver() {
        IntentFilter filter = new IntentFilter(GEOFENCE_ACTION);
        // Android 13+ requires specifying receiver export flag
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(geofenceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(geofenceReceiver, filter);
        }
        
        Log.d(TAG, "Geofence receiver registered");
    }
    private final BroadcastReceiver geofenceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GEOFENCE_ACTION.equals(intent.getAction())) {
                String geofenceId = intent.getStringExtra("geofence_id");
                if (geofenceId != null && geofenceId.startsWith("treasure_")) {
                    int treasureIndex = Integer.parseInt(geofenceId.replace("treasure_", ""));
                    collectTreasure(treasureIndex);
                }
            }
        }
    };
    
    private void collectTreasure(int treasureIndex) {
        if (treasureIndex >= 0 && treasureIndex < treasuresCollected.size() && 
            !treasuresCollected.get(treasureIndex)) {
            
            // Mark treasure as collected
            treasuresCollected.set(treasureIndex, true);
            treasuresFound++;
            
            // Update score based on treasure type
            int points = calculateTreasurePoints(treasureIndex);
            score += points;
            
            // Remove the marker from the map (make coin vanish)
            if (treasureMarkers != null && treasureIndex < treasureMarkers.size()) {
                Marker marker = treasureMarkers.get(treasureIndex);
                if (marker != null) {
                    marker.remove();
                    Log.d(TAG, "Treasure " + treasureIndex + " marker removed from map");
                }
            }
            
            // Update UI (change scoreboard)
            updateScoreDisplay();
            
            // Show collection feedback dialog
            showTreasureCollectedDialog(points);
            
            Log.d(TAG, "Treasure collected! Index: " + treasureIndex + ", Points: " + points + 
                      ", Total Score: " + score + ", Treasures: " + treasuresFound + "/" + treasureLocations.size());
            
            // Check if all treasures collected
            if (treasuresFound >= treasureLocations.size()) {
                showGameComplete();
            }
        }
    }
    
    /**
     * Show dialog when treasure is collected
     */
    private void showTreasureCollectedDialog(int points) {
        runOnUiThread(() -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("🎉 Treasure Collected!")
                    .setMessage("You earned +" + points + " points!\n\nKeep walking to find more treasures!")
                    .setPositiveButton("Continue", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        });
    }
    
    private int calculateTreasurePoints(int treasureIndex) {
        // Different point values based on treasure type
        if (treasureIndex % 8 == 0) {
            return 100; // Epic treasure
        } else if (treasureIndex % 4 == 0) {
            return 50;  // Rare treasure
        } else {
            return 25;  // Common treasure
        }
    }
    
    private void updateScoreDisplay() {
        tvScore.setText(String.valueOf(score));
        tvTreasuresCollected.setText(String.valueOf(treasuresFound));
    }
    
    private void updateDistanceDisplay() {
        // Show steps instead of distance
        tvDistance.setText(String.format("%,d", totalSteps));
    }
    
    private void showGameComplete() {
        Toast.makeText(this, "Congratulations! All treasures collected!", Toast.LENGTH_LONG).show();
        // Could show a completion dialog or automatically end session
    }
    
    private BitmapDescriptor createScaledGoldIcon() {
        // Same implementation as SessionMapActivity
        android.graphics.Bitmap originalBitmap = android.graphics.BitmapFactory.decodeResource(
                getResources(), R.drawable.gold);
        android.graphics.Bitmap scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                originalBitmap, 48, 48, false);
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap);
    }
    
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            }
        }
    }
    
    private void endSession() {
        // Stop timer
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        
        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback);
        
        // Remove geofences
        geofencingClient.removeGeofences(getGeofencePendingIntent());
        
        // Unregister sensor
        if (sensorManager != null && stepCounterSensor != null) {
            sensorManager.unregisterListener(this);
        }
        
        // Calculate session metrics
        long sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000; // in seconds
        float distanceKm = totalDistance / 1000f; // Convert to km
        
        // Final calorie calculation (already calculated in real-time)
        calculateCalories();
        
        // Calculate average velocity (km/h)
        float averageVelocity = 0f;
        if (sessionDuration > 0) {
            averageVelocity = (distanceKm / sessionDuration) * 3600; // Convert to km/h
        }
        
        Log.d(TAG, "Session ended - Distance: " + distanceKm + "km, Steps: " + totalSteps + 
              ", Calories: " + caloriesBurned + ", Velocity: " + averageVelocity + "km/h");
        
        // Show session summary with direct stats (no session ID needed)
        Intent intent = new Intent(this, SessionSummaryDirectActivity.class);
        intent.putExtra("distance_traveled", distanceKm);
        intent.putExtra("distance_goal", distanceGoal);
        intent.putExtra("session_duration", sessionDuration);
        intent.putExtra("total_steps", totalSteps);
        intent.putExtra("calories_burned", caloriesBurned); // Real calories from calculation
        intent.putExtra("average_velocity", averageVelocity);
        intent.putExtra("treasures_collected", treasuresFound);
        intent.putExtra("total_treasures", treasureLocations != null ? treasureLocations.size() : 0);
        intent.putExtra("final_score", score);
        startActivity(intent);
        
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Stop timer
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        
        // Unregister sensor
        if (sensorManager != null && stepCounterSensor != null) {
            sensorManager.unregisterListener(this);
        }
        
        if (geofenceReceiver != null) {
            unregisterReceiver(geofenceReceiver);
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
    
    /**
     * Enable edge-to-edge display to prevent system navigation bar overlap
     */
    private void enableEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }
    
    /**
     * Apply system bar insets to a view to prevent overlap with navigation bar
     */
    private void applySystemBarInsets(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            view.setOnApplyWindowInsetsListener((v, insets) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.graphics.Insets systemBars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
                    // Get current margin
                    android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    params.bottomMargin = systemBars.bottom + 16; // Add system bar height + extra margin
                    v.setLayoutParams(params);
                    return insets;
                } else {
                    android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    params.bottomMargin = insets.getSystemWindowInsetBottom() + 16;
                    v.setLayoutParams(params);
                    return insets.consumeSystemWindowInsets();
                }
            });
        }
    }
}