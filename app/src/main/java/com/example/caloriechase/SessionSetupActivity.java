package com.example.caloriechase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.caloriechase.data.SessionManager;
import com.example.caloriechase.data.TreasureGenerator;
import com.example.caloriechase.data.TreasureLocation;
import com.example.caloriechase.data.SessionDraft;
import com.example.caloriechase.data.ActivityType;
import com.example.caloriechase.data.ActiveSession;
import com.example.caloriechase.data.TreasureType;
import com.example.caloriechase.error.ErrorHandler;
import com.example.caloriechase.views.TrackVisualizationView;
import com.example.caloriechase.api.RetrofitClient;
import com.example.caloriechase.api.AgentResponse;
import com.example.caloriechase.api.GooglePlacesResponse;
import com.example.caloriechase.api.DirectionsResponse;
import com.example.caloriechase.utils.PolylineDecoder;
import com.example.caloriechase.utils.GeminiHelper;
import com.example.caloriechase.utils.GMHelper;
import android.util.Log;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;
import java.util.ArrayList;

public class SessionSetupActivity extends AppCompatActivity {
    
    private static final String TAG = "SessionSetupActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION
    };
    
    // UI Components
    private CardView cvPermissionCheck;
    private CardView cvGpsCheck;
    private CardView cvBatteryCheck;
    private CardView cvDistanceGoal;
    private CardView cvStartingPoint;
    private TextView tvPermissionStatus;
    private TextView tvGpsStatus;
    private TextView tvBatteryStatus;
    private TextView tvDistanceValue;
    private SeekBar seekBarDistance;
    private EditText etManualDistance;
    private MaterialButton btnChooseStartingPoint;
    private MaterialButton btnStartSession;
    private Button btnOpenSettings;
    private Button btnEnableGps;
    private Button btnBatterySettings;
    private TrackVisualizationView trackVisualization;
    private TextView tvSelectedLocation;
    private EditText etAiPrompt;
    private Spinner spinnerActivityType;
    
    // Data
    private SessionManager sessionManager;
    private TreasureGenerator treasureGenerator;
    private float selectedDistance = 1.0f; // Default 1km
    private boolean permissionsGranted = false;
    private boolean gpsEnabled = false;
    private boolean batteryOptimized = false;
    private boolean startingPointSelected = false;
    
    // Selected location data
    private double selectedLatitude;
    private double selectedLongitude;
    private String selectedAddress;
    private float distanceFromCurrent;
    
    // Session and treasure data
    private SessionDraft currentSessionDraft;
    private List<TreasureLocation> generatedTreasures;
    private boolean treasuresGenerated = false;
    
    // AI-generated track points (from Google Places)
    private List<GooglePlacesResponse.Result> googlePlacesResults;
    private boolean useAiPoints = false;
    
    // Directions API route data
    private List<com.google.android.gms.maps.model.LatLng> directionsRoutePoints;
    private boolean useDirectionsRoute = false;
    
    // Google Places API Key
    private static final String GOOGLE_PLACES_API_KEY = "AIzaSyD6nuU_yZkx0gnmhlNW_9A6muRIIUwBOVQ";
    
    // Error handling
    private ErrorHandler errorHandler;
    
    // Gemini AI helper
    private GeminiHelper geminiHelper;
    
    // GMHelper for prompt refinement
    private GMHelper gmHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_setup);
        
        // Enable edge-to-edge display
        enableEdgeToEdge();
        
        sessionManager = SessionManager.getInstance(this);
        treasureGenerator = new TreasureGenerator();
        errorHandler = new ErrorHandler(this);
        geminiHelper = new GeminiHelper(this);
        gmHelper = new GMHelper();
        
        initViews();
        setupDistanceGoal();
        setupDistanceSeekBarListener();
        checkInitialStates();
        setupListeners();
    }
    
    private void initViews() {
        // Status cards
        cvPermissionCheck = findViewById(R.id.cv_permission_check);
        cvGpsCheck = findViewById(R.id.cv_gps_check);
        cvBatteryCheck = findViewById(R.id.cv_battery_check);
        cvDistanceGoal = findViewById(R.id.cv_distance_goal);
        cvStartingPoint = findViewById(R.id.cv_starting_point);
        
        // Status text views
        tvPermissionStatus = findViewById(R.id.tv_permission_status);
        tvGpsStatus = findViewById(R.id.tv_gps_status);
        tvBatteryStatus = findViewById(R.id.tv_battery_status);
        tvDistanceValue = findViewById(R.id.tv_distance_value);
        
        // Controls
        seekBarDistance = findViewById(R.id.seekbar_distance);
        etManualDistance = findViewById(R.id.et_manual_distance);
        btnChooseStartingPoint = findViewById(R.id.btn_choose_starting_point);
        btnStartSession = findViewById(R.id.btn_start_session);
        btnOpenSettings = findViewById(R.id.btn_open_settings);
        btnEnableGps = findViewById(R.id.btn_enable_gps);
        btnBatterySettings = findViewById(R.id.btn_battery_settings);
        trackVisualization = findViewById(R.id.track_visualization);
        tvSelectedLocation = findViewById(R.id.tv_selected_location);
        etAiPrompt = findViewById(R.id.et_ai_prompt);
        spinnerActivityType = findViewById(R.id.spinner_activity_type);
        
        // Setup activity type spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.activity_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityType.setAdapter(adapter);
        spinnerActivityType.setSelection(0); // Default to "Run"
    }
    
    @SuppressLint("DefaultLocale")
    private void setupDistanceGoal() {
        // SeekBar for 0.1km to 50km range (in 0.1km increments)
        seekBarDistance.setMax(499); // 0.1 to 50.0 in 0.1 increments = 499 steps
        seekBarDistance.setProgress(9); // Default to 1.0km (position 9)
        
        seekBarDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    selectedDistance = calculateDistanceFromProgress(progress);
                    tvDistanceValue.setText(String.format("%.1f km", selectedDistance));
                    etManualDistance.setText(String.format("%.1f", selectedDistance));
                    updateStartingPointButton();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Manual distance input
        etManualDistance.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateDistanceFromEditText();
            }
        });
        
        etManualDistance.setOnEditorActionListener((v, actionId, event) -> {
            updateDistanceFromEditText();
            etManualDistance.clearFocus();
            return true;
        });
        
        // Set initial values
        tvDistanceValue.setText(String.format("%.1f km", selectedDistance));
        etManualDistance.setText(String.format("%.1f", selectedDistance));
        trackVisualization.setDistance(selectedDistance);
    }
    
    private float calculateDistanceFromProgress(int progress) {
        // 0.1 to 50.0 in 0.1km increments
        return 0.1f + (progress * 0.1f);
    }
    
    private int calculateProgressFromDistance(float distance) {
        // 0.1 to 50.0 in 0.1km increments
        return Math.round((distance - 0.1f) / 0.1f);
    }
    
    @SuppressLint("DefaultLocale")
    private void updateDistanceFromEditText() {
        try {
            String text = etManualDistance.getText().toString().trim();
            if (!text.isEmpty()) {
                float inputDistance = Float.parseFloat(text);
                
                // Validate range
                if (inputDistance < 0.1f) {
                    inputDistance = 0.1f;
                    Toast.makeText(this, "Minimum distance is 0.1 km", Toast.LENGTH_SHORT).show();
                } else if (inputDistance > 50.0f) {
                    inputDistance = 50.0f;
                    Toast.makeText(this, "Maximum distance is 50 km", Toast.LENGTH_SHORT).show();
                }
                
                selectedDistance = inputDistance;
                tvDistanceValue.setText(String.format("%.1f km", selectedDistance));
                etManualDistance.setText(String.format("%.1f", selectedDistance));
                trackVisualization.setDistance(selectedDistance);
                
                // Update seekbar without triggering listener
                seekBarDistance.setOnSeekBarChangeListener(null);
                seekBarDistance.setProgress(calculateProgressFromDistance(selectedDistance));
                setupDistanceSeekBarListener();
                
                updateStartingPointButton();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            etManualDistance.setText(String.format("%.1f", selectedDistance));
        }
    }
    
    private void setupDistanceSeekBarListener() {
        seekBarDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    selectedDistance = calculateDistanceFromProgress(progress);
                    tvDistanceValue.setText(String.format("%.1f km", selectedDistance));
                    etManualDistance.setText(String.format("%.1f", selectedDistance));
                    trackVisualization.setDistance(selectedDistance);
                    updateStartingPointButton();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void checkInitialStates() {
        checkPermissions();
        checkGpsStatus();
        checkBatteryOptimization();
        updateUI();
    }
    
    private void setupListeners() {
        btnOpenSettings.setOnClickListener(v -> openAppSettings());
        btnEnableGps.setOnClickListener(v -> openLocationSettings());
        btnBatterySettings.setOnClickListener(v -> openBatterySettings());
        btnChooseStartingPoint.setOnClickListener(v -> openMapSearch());
        btnStartSession.setOnClickListener(v -> startSession());
    }
    
    private void checkPermissions() {
        permissionsGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsGranted = false;
                break;
            }
        }
        
        if (!permissionsGranted) {
            requestPermissions();
        }
    }
    
    private void requestPermissions() {
        // Use ErrorHandler for consistent permission request handling
        errorHandler.handlePermissionDenied(ErrorHandler.PermissionType.LOCATION, 
                new ErrorHandler.PermissionDenialCallback() {
            @Override
            public void onSettingsOpened() {
                // User went to settings, they'll return via onResume()
            }
            
            @Override
            public void onContinueWithoutPermission() {
                // User chose to continue without permissions
                updateUI();
                Toast.makeText(SessionSetupActivity.this, 
                             "Limited functionality without permissions", 
                             Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onCancel() {
                // User cancelled, update UI to reflect permission state
                updateUI();
            }
        });
    }
    
    private void checkGpsStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        gpsEnabled = locationManager != null && 
                    (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                     locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }
    
    private void checkBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            batteryOptimized = powerManager.isPowerSaveMode();
            
            // Also check if app is whitelisted from battery optimization (Android 6+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(getPackageName());
                if (!isIgnoringOptimizations) {
                    batteryOptimized = true; // Consider it optimized if not whitelisted
                }
            }
        }
    }
    
    @SuppressLint("SetTextI18n")
    private void updateUI() {
        // Update permission status
        if (permissionsGranted) {
            tvPermissionStatus.setText("✓ Permissions granted");
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
            btnOpenSettings.setVisibility(View.GONE);
        } else {
            tvPermissionStatus.setText("⚠ Permissions required");
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.warning));
            btnOpenSettings.setVisibility(View.VISIBLE);
        }
        
        // Update GPS status
        if (gpsEnabled) {
            tvGpsStatus.setText("✓ GPS enabled");
            tvGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
            btnEnableGps.setVisibility(View.GONE);
        } else {
            tvGpsStatus.setText("⚠ GPS disabled");
            tvGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.warning));
            btnEnableGps.setVisibility(View.VISIBLE);
        }
        
        // Update battery status
        if (batteryOptimized) {
            tvBatteryStatus.setText("⚠ Battery saver active - GPS/step tracking may be affected");
            tvBatteryStatus.setTextColor(ContextCompat.getColor(this, R.color.warning));
            btnBatterySettings.setVisibility(View.VISIBLE);
        } else {
            tvBatteryStatus.setText("✓ Battery optimization OK");
            tvBatteryStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
            btnBatterySettings.setVisibility(View.GONE);
        }
        
        updateStartingPointButton();
        updateStartSessionButton();
    }
    
    private void updateStartingPointButton() {
        btnChooseStartingPoint.setEnabled(selectedDistance > 0);
        if (selectedDistance > 0) {
            if (startingPointSelected) {
                btnChooseStartingPoint.setText("Change Starting Point");
            } else {
                btnChooseStartingPoint.setText("Choose Starting Point");
            }
            btnChooseStartingPoint.setAlpha(1.0f);
        } else {
            btnChooseStartingPoint.setText("Select distance first");
            btnChooseStartingPoint.setAlpha(0.5f);
        }
    }
    
    private void updateSelectedLocationDisplay() {
        if (startingPointSelected && selectedAddress != null) {
            tvSelectedLocation.setText(selectedAddress);
            tvSelectedLocation.setVisibility(View.VISIBLE);
            tvSelectedLocation.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else {
            tvSelectedLocation.setVisibility(View.GONE);
        }
    }
    
    private void updateStartSessionButton() {
        boolean canStart = permissionsGranted && startingPointSelected;
        btnStartSession.setEnabled(canStart);
        
        if (canStart) {
            btnStartSession.setText("Start Session");
            btnStartSession.setAlpha(1.0f);
        } else if (!permissionsGranted) {
            btnStartSession.setText("Grant permissions first");
            btnStartSession.setAlpha(0.5f);
        } else if (!startingPointSelected) {
            btnStartSession.setText("Choose starting point first");
            btnStartSession.setAlpha(0.5f);
        }
    }
    
    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to general settings
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Please find app permissions manually in settings", Toast.LENGTH_LONG).show();
        }
    }
    
    private void openLocationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to general settings if location settings not available
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Please enable location services manually", Toast.LENGTH_LONG).show();
        }
    }
    
    private void openBatterySettings() {
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Try to open battery optimization settings for this app
                intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
            } else {
                // Fallback to general battery settings
                intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
            }
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to general settings if specific battery settings not available
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Please find battery optimization settings manually", Toast.LENGTH_LONG).show();
        }
    }
    
    private void openMapSearch() {
        if (!permissionsGranted) {
            Toast.makeText(this, "Please grant permissions first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, MapSearchActivity.class);
        intent.putExtra("distance_goal", selectedDistance);
        startActivityForResult(intent, 1002);
    }
    
    private void startSession() {
        if (!permissionsGranted) {
            Toast.makeText(this, "Please grant permissions first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!startingPointSelected) {
            Toast.makeText(this, "Please choose a starting point first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show warning if GPS is disabled
        if (!gpsEnabled) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("GPS Disabled")
                    .setMessage("GPS is disabled. Your session will rely on step counting which may be less accurate. Continue anyway?")
                    .setPositiveButton("Continue", (dialog, which) -> generateTreasuresAndProceed())
                    .setNegativeButton("Enable GPS", (dialog, which) -> openLocationSettings())
                    .show();
            return;
        }
        
        // Show warning if battery saver is on
        if (batteryOptimized) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Battery Saver Active")
                    .setMessage("Battery saver mode may affect GPS accuracy and step counting. Consider disabling it for better tracking. Continue anyway?")
                    .setPositiveButton("Continue", (dialog, which) -> generateTreasuresAndProceed())
                    .setNegativeButton("Battery Settings", (dialog, which) -> openBatterySettings())
                    .show();
            return;
        }
        
        generateTreasuresAndProceed();
    }
    
    /**
     * Generate treasures (with AI if prompt provided) and then proceed to map
     */
    private void generateTreasuresAndProceed() {
        // Check if AI prompt is provided
        String aiPrompt = etAiPrompt.getText().toString().trim();
        
        if (!aiPrompt.isEmpty()) {
            Log.d(TAG, "User entered AI prompt: " + aiPrompt);
            
            // Show loading dialog while Gemini refines the prompt
            MaterialAlertDialogBuilder loadingDialog = new MaterialAlertDialogBuilder(this)
                    .setTitle("Processing with AI")
                    .setMessage("Refining your search with Gemini AI...")
                    .setCancelable(false);
            
            androidx.appcompat.app.AlertDialog loading = loadingDialog.show();
            
            // Create a refined prompt instruction for Gemini
            String refinementInstruction = "Convert the following user prompt into a concise keyword " +
                    "or phrase suitable for Google Places API search. The output must contain " +
                    "**only** the refined keyword or phrase — no explanations, punctuation, " +
                    "or extra text. \nUser prompt: " + aiPrompt;
            
            // Use GMHelper to refine the prompt
            gmHelper.callGemini(refinementInstruction, new GMHelper.GeminiCallback() {
                @Override
                public void onSuccess(String refinedPrompt) {
                    Log.d(TAG, "GMHelper success! Refined prompt: " + refinedPrompt);
                    runOnUiThread(() -> {
                        loading.dismiss();
                        // Clean up the response (remove quotes, trim whitespace)
                        String cleanedPrompt = refinedPrompt.trim().replaceAll("^\"|\"$", "");
                        // Use the refined prompt to fetch places
                        fetchAiGeneratedPointsAndProceed(cleanedPrompt);
                    });
                }
                
                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "GMHelper error: " + t.getMessage());
                    runOnUiThread(() -> {
                        loading.dismiss();
                        Toast.makeText(SessionSetupActivity.this, 
                                     "AI refinement failed: " + t.getMessage() + ". Using original prompt.", 
                                     Toast.LENGTH_LONG).show();
                        // Fallback to using the original prompt
                        fetchAiGeneratedPointsAndProceed(aiPrompt);
                    });
                }
            });
        } else {
            Log.d(TAG, "No AI prompt provided, using default generation");
            // Use default random generation
            generateDefaultTreasuresAndProceed();
        }
    }
    
    private void proceedToActiveSession() {
        if (!treasuresGenerated || currentSessionDraft == null || generatedTreasures == null) {
            Toast.makeText(this, "Session not properly configured. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Launch SessionMapActivity to show the route before starting
        Intent intent = new Intent(SessionSetupActivity.this, SessionMapActivity.class);
        intent.putExtra("distance", selectedDistance);
        intent.putExtra("start_latitude", selectedLatitude);
        intent.putExtra("start_longitude", selectedLongitude);
        intent.putExtra("start_address", selectedAddress);
        
        // Pass Directions API route if available
        if (useDirectionsRoute && directionsRoutePoints != null && !directionsRoutePoints.isEmpty()) {
            double[] routeLats = new double[directionsRoutePoints.size()];
            double[] routeLngs = new double[directionsRoutePoints.size()];
            
            for (int i = 0; i < directionsRoutePoints.size(); i++) {
                com.google.android.gms.maps.model.LatLng point = directionsRoutePoints.get(i);
                routeLats[i] = point.latitude;
                routeLngs[i] = point.longitude;
            }
            
            intent.putExtra("directions_route_latitudes", routeLats);
            intent.putExtra("directions_route_longitudes", routeLngs);
            intent.putExtra("use_directions_route", true);
        }
        
        // Pass Google Places points if available
        if (useAiPoints && googlePlacesResults != null && !googlePlacesResults.isEmpty()) {
            double[] aiLats = new double[googlePlacesResults.size()];
            double[] aiLngs = new double[googlePlacesResults.size()];
            String[] aiNames = new String[googlePlacesResults.size()];
            
            for (int i = 0; i < googlePlacesResults.size(); i++) {
                GooglePlacesResponse.Result place = googlePlacesResults.get(i);
                if (place.geometry != null && place.geometry.location != null) {
                    aiLats[i] = place.geometry.location.lat;
                    aiLngs[i] = place.geometry.location.lng;
                    aiNames[i] = place.name != null ? place.name : "Place " + (i + 1);
                }
            }
            
            intent.putExtra("ai_points_latitudes", aiLats);
            intent.putExtra("ai_points_longitudes", aiLngs);
            intent.putExtra("ai_points_names", aiNames);
            intent.putExtra("use_ai_points", true);
        }
        
        startActivity(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recheck states when returning from settings
        checkInitialStates();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1002 && resultCode == RESULT_OK && data != null) {
            // Starting point was selected in MapSearchActivity
            selectedLatitude = data.getDoubleExtra("selected_latitude", 0);
            selectedLongitude = data.getDoubleExtra("selected_longitude", 0);
            selectedAddress = data.getStringExtra("selected_address");
            distanceFromCurrent = data.getFloatExtra("distance_from_current", 0);
            
            startingPointSelected = true;
            updateSelectedLocationDisplay();
            updateUI();
            
            // Don't auto-generate - wait for user to click Start Session
            // This allows them to enter an AI prompt first
        }
    }
    
    /**
     * Fetch places from Google Places API and proceed to map
     */
    private void fetchAiGeneratedPointsAndProceed(String keyword) {
        MaterialAlertDialogBuilder loadingDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Finding Places")
                .setMessage("Searching for " + keyword + " nearby...")
                .setCancelable(false);
        
        androidx.appcompat.app.AlertDialog loading = loadingDialog.show();
        
        // Use fixed 500m radius for API call
        int radiusMeters = (int) (selectedDistance*1000);
        
        // Format location as "lat,lng"
        String location = selectedLatitude + "," + selectedLongitude;
        
        // Call Google Places API
        Call<GooglePlacesResponse.Root> call = RetrofitClient.googlePlacesApi().nearbySearch(
                location,
                radiusMeters,
                keyword,
                GOOGLE_PLACES_API_KEY
        );
        
        call.enqueue(new Callback<GooglePlacesResponse.Root>() {
            @Override
            public void onResponse(Call<GooglePlacesResponse.Root> call, Response<GooglePlacesResponse.Root> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        GooglePlacesResponse.Root root = response.body();
                        
                        if ("OK".equals(root.status) && root.results != null && !root.results.isEmpty()) {
                            // Find the place with distance closest to user's selected distance
                            GooglePlacesResponse.Result selectedPlace = findPlaceClosestToTargetDistance(root.results);
                            
                            if (selectedPlace == null) {
                                loading.dismiss();
                                Toast.makeText(SessionSetupActivity.this, 
                                             "No valid places found. Using default generation.", 
                                             Toast.LENGTH_SHORT).show();
                                useAiPoints = false;
                                generateDefaultTreasuresAndProceed();
                                return;
                            }
                            
                            // Store all results for reference
                            googlePlacesResults = root.results;
                            useAiPoints = true;
                            
                            loading.setMessage("Getting route to " + 
                                (selectedPlace.name != null ? selectedPlace.name : "destination") + "...");
                            
                            // Fetch directions from starting point to selected place
                            fetchDirectionsAndProceed(selectedPlace, loading);
                        } else {
                            loading.dismiss();
                            String errorMsg = root.status != null ? root.status : "No places found";
                            Toast.makeText(SessionSetupActivity.this, 
                                         errorMsg + ". Using default generation.", 
                                         Toast.LENGTH_SHORT).show();
                            useAiPoints = false;
                            generateDefaultTreasuresAndProceed();
                        }
                    } else {
                        loading.dismiss();
                        Toast.makeText(SessionSetupActivity.this, 
                                     "Places API request failed. Using default generation.", 
                                     Toast.LENGTH_LONG).show();
                        useAiPoints = false;
                        generateDefaultTreasuresAndProceed();
                    }
                });
            }
            
            @Override
            public void onFailure(Call<GooglePlacesResponse.Root> call, Throwable t) {
                runOnUiThread(() -> {
                    loading.dismiss();
                    Toast.makeText(SessionSetupActivity.this, 
                                 "Network error: " + t.getMessage() + ". Using default generation.", 
                                 Toast.LENGTH_LONG).show();
                    useAiPoints = false;
                    generateDefaultTreasuresAndProceed();
                });
            }
        });
    }
    
    /**
     * Fetch directions from starting point to selected place
     */
    private void fetchDirectionsAndProceed(GooglePlacesResponse.Result destination, 
                                          androidx.appcompat.app.AlertDialog loadingDialog) {
        if (destination.geometry == null || destination.geometry.location == null) {
            loadingDialog.dismiss();
            Toast.makeText(this, "Invalid destination. Using default generation.", 
                         Toast.LENGTH_SHORT).show();
            useAiPoints = false;
            useDirectionsRoute = false;
            generateDefaultTreasuresAndProceed();
            return;
        }
        
        // Format origin and destination as "lat,lng"
        String origin = selectedLatitude + "," + selectedLongitude;
        String dest = destination.geometry.location.lat + "," + destination.geometry.location.lng;
        
        // Get the selected activity mode from spinner and convert to Directions API mode
        String selectedActivity = spinnerActivityType.getSelectedItem().toString();
        String travelMode = convertActivityToTravelMode(selectedActivity);
        
        // Call Directions API with the selected mode
        Call<DirectionsResponse.Root> call = RetrofitClient.googlePlacesApi().getDirections(
                origin,
                dest,
                travelMode,
                GOOGLE_PLACES_API_KEY
        );
        
        call.enqueue(new Callback<DirectionsResponse.Root>() {
            @Override
            public void onResponse(Call<DirectionsResponse.Root> call, 
                                 Response<DirectionsResponse.Root> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        DirectionsResponse.Root root = response.body();
                        
                        if ("OK".equals(root.status) && root.routes != null && !root.routes.isEmpty()) {
                            DirectionsResponse.Route route = root.routes.get(0);
                            
                            // Decode the overview polyline
                            if (route.overview_polyline != null && 
                                route.overview_polyline.points != null) {
                                
                                directionsRoutePoints = PolylineDecoder.decode(
                                    route.overview_polyline.points);
                                useDirectionsRoute = true;
                                
                                loadingDialog.dismiss();
                                
                                String destName = destination.name != null ? 
                                    destination.name : "destination";
                                Toast.makeText(SessionSetupActivity.this, 
                                             "Route to " + destName + " loaded with " + 
                                             directionsRoutePoints.size() + " points!", 
                                             Toast.LENGTH_SHORT).show();
                                
                                // Now create session and proceed to map
                                generateDefaultTreasuresAndProceed();
                            } else {
                                loadingDialog.dismiss();
                                Toast.makeText(SessionSetupActivity.this, 
                                             "No route polyline found. Using default generation.", 
                                             Toast.LENGTH_SHORT).show();
                                useDirectionsRoute = false;
                                generateDefaultTreasuresAndProceed();
                            }
                        } else {
                            loadingDialog.dismiss();
                            String errorMsg = root.status != null ? root.status : "No route found";
                            Toast.makeText(SessionSetupActivity.this, 
                                         errorMsg + ". Using default generation.", 
                                         Toast.LENGTH_SHORT).show();
                            useDirectionsRoute = false;
                            generateDefaultTreasuresAndProceed();
                        }
                    } else {
                        loadingDialog.dismiss();
                        Toast.makeText(SessionSetupActivity.this, 
                                     "Directions API request failed. Using default generation.", 
                                     Toast.LENGTH_LONG).show();
                        useDirectionsRoute = false;
                        generateDefaultTreasuresAndProceed();
                    }
                });
            }
            
            @Override
            public void onFailure(Call<DirectionsResponse.Root> call, Throwable t) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(SessionSetupActivity.this, 
                                 "Network error: " + t.getMessage() + ". Using default generation.", 
                                 Toast.LENGTH_LONG).show();
                    useDirectionsRoute = false;
                    generateDefaultTreasuresAndProceed();
                });
            }
        });
    }
    
    /**
     * Find the place whose distance from starting point is closest to the user's selected distance
     */
    private GooglePlacesResponse.Result findPlaceClosestToTargetDistance(List<GooglePlacesResponse.Result> allPlaces) {
        if (allPlaces == null || allPlaces.isEmpty()) {
            return null;
        }
        
        // Target distance in meters
        double targetDistanceMeters = selectedDistance * 1000;
        
        GooglePlacesResponse.Result closestPlace = null;
        double smallestDifference = Double.MAX_VALUE;
        
        // Check each place and find the one with the smallest distance difference
        for (GooglePlacesResponse.Result place : allPlaces) {
            if (place.geometry == null || place.geometry.location == null) {
                continue; // Skip places without location
            }
            
            double placeLat = place.geometry.location.lat;
            double placeLng = place.geometry.location.lng;
            
            // Calculate distance from starting point to this place
            double distanceToPlace = calculateDistance(selectedLatitude, selectedLongitude, placeLat, placeLng);
            
            // Calculate the difference from target distance
            double difference = Math.abs(distanceToPlace - targetDistanceMeters);
            
            // Update if this is the closest match so far
            if (difference < smallestDifference) {
                smallestDifference = difference;
                closestPlace = place;
            }
        }
        
        // Log the selected place for debugging
        if (closestPlace != null) {
            double actualDistance = calculateDistance(selectedLatitude, selectedLongitude, 
                                                     closestPlace.geometry.location.lat, 
                                                     closestPlace.geometry.location.lng);
            Log.d(TAG, String.format("Selected place: %s, Distance: %.2f km (Target: %.2f km)", 
                                    closestPlace.name, actualDistance / 1000, selectedDistance));
        }
        
        return closestPlace;
    }
    
    /**
     * Convert activity type to Google Directions API travel mode
     */
    private String convertActivityToTravelMode(String activityType) {
        switch (activityType.toLowerCase()) {
            case "run":
            case "jog":
            case "walk":
                return "walking";
            default:
                return "walking"; // Default to walking
        }
    }
    
    /**
     * Calculate distance between two lat/lng points in meters using Haversine formula
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double EARTH_RADIUS = 6371000; // Earth radius in meters
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLngRad = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }
    
    /**
     * Generate treasures using default random method and proceed to map
     */
    private void generateDefaultTreasuresAndProceed() {
        MaterialAlertDialogBuilder loadingDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Generating Treasures")
                .setMessage("Creating your treasure hunt...")
                .setCancelable(false);
        
        androidx.appcompat.app.AlertDialog loading = loadingDialog.show();
        
        // Create session draft first
        sessionManager.createSessionDraft(selectedLatitude, selectedLongitude, 
                                        selectedDistance, ActivityType.WALK, 
                                        new SessionManager.SessionCallback<SessionDraft>() {
            @Override
            public void onSuccess(SessionDraft draft) {
                currentSessionDraft = draft;
                
                // Generate treasures
                generatedTreasures = treasureGenerator.generateTreasureRing(
                    draft.sessionId, selectedLatitude, selectedLongitude, selectedDistance);
                
                treasuresGenerated = true;
                
                // Dismiss loading and proceed to map
                runOnUiThread(() -> {
                    loading.dismiss();
                    proceedToActiveSession();
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    loading.dismiss();
                    Toast.makeText(SessionSetupActivity.this, 
                                 "Failed to create session: " + error.getMessage(), 
                                 Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * Show session preview with treasure information
     */
    private void showSessionPreview() {
        if (!treasuresGenerated || generatedTreasures == null) {
            Toast.makeText(this, "Treasures not generated yet", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String addressText = selectedAddress != null ? selectedAddress : "Selected location";
        @SuppressLint("DefaultLocale") String distanceText = distanceFromCurrent > 0 ?
            String.format(" (%.1f km away)", distanceFromCurrent) : "";
        
        // Count treasures by type
        int commonCount = 0, rareCount = 0, epicCount = 0;
        for (TreasureLocation treasure : generatedTreasures) {
            switch (treasure.type) {
                case COMMON: commonCount++; break;
                case RARE: rareCount++; break;
                case EPIC: epicCount++; break;
            }
        }
        
        @SuppressLint("DefaultLocale") String treasureSummary = String.format("We placed %d treasures within ~%.1f km:\n" +
                                             "• %d Common treasures\n" +
                                             "• %d Rare treasures\n" +
                                             "• %d Epic treasures",
                                             generatedTreasures.size(), selectedDistance,
                                             commonCount, rareCount, epicCount);
        
        @SuppressLint("DefaultLocale") String message = String.format(
            "Session Preview:\n\n" +
            "📍 Starting Point: %s%s\n" +
            "🎯 Distance Goal: %.1f km\n" +
            "💎 %s\n\n" +
            "Ready to start your treasure hunt?",
            addressText, distanceText, selectedDistance, treasureSummary
        );
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Session Preview")
                .setMessage(message)
                .setPositiveButton("Start Now", (dialog, which) -> startSession())
                .setNeutralButton("Edit Treasures", (dialog, which) -> showTreasureEditOptions())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Clean up session draft if user cancels
                    cleanupSessionDraft();
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * Show treasure editing options for advanced users
     */
    private void showTreasureEditOptions() {
        String[] options = {
            "Regenerate All Treasures",
            "Change Treasure Density",
            "Preview Treasure Locations",
            "Back to Session Preview"
        };
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Treasures")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Regenerate All Treasures
                            regenerateTreasures();
                            break;
                        case 1: // Change Treasure Density
                            showTreasureDensityOptions();
                            break;
                        case 2: // Preview Treasure Locations
                            showTreasureLocationPreview();
                            break;
                        case 3: // Back to Session Preview
                            showSessionPreview();
                            break;
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> showSessionPreview())
                .show();
    }
    
    /**
     * Regenerate treasures with new random positions
     */
    private void regenerateTreasures() {
        MaterialAlertDialogBuilder loadingDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Regenerating Treasures")
                .setMessage("Creating new treasure locations...")
                .setCancelable(false);
        
        androidx.appcompat.app.AlertDialog loading = loadingDialog.show();
        
        // Generate new treasures
        new Thread(() -> {
            try {
                generatedTreasures = treasureGenerator.generateTreasureRing(
                    currentSessionDraft.sessionId, selectedLatitude, selectedLongitude, selectedDistance);
                
                runOnUiThread(() -> {
                    loading.dismiss();
                    Toast.makeText(this, "Treasures regenerated!", Toast.LENGTH_SHORT).show();
                    showSessionPreview();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    loading.dismiss();
                    Toast.makeText(this, "Failed to regenerate treasures: " + e.getMessage(), 
                                 Toast.LENGTH_LONG).show();
                    showSessionPreview();
                });
            }
        }).start();
    }
    
    /**
     * Show treasure density adjustment options
     */
    private void showTreasureDensityOptions() {
        TreasureGenerator.TreasureGenerationStats stats = treasureGenerator.getGenerationStats(selectedDistance);
        
        String message = String.format(
            "Current Settings:\n" +
            "• Distance Goal: %.1f km\n" +
            "• Treasure Count: %d\n" +
            "• Search Radius: %.0f meters\n\n" +
            "Treasure density is automatically calculated based on your distance goal. " +
            "Would you like to regenerate with the same settings?",
            selectedDistance, stats.totalTreasures, stats.ringRadiusMeters
        );
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Treasure Density")
                .setMessage(message)
                .setPositiveButton("Regenerate", (dialog, which) -> regenerateTreasures())
                .setNegativeButton("Back", (dialog, which) -> showTreasureEditOptions())
                .show();
    }
    
    /**
     * Show treasure location preview information
     */
    private void showTreasureLocationPreview() {
        if (generatedTreasures == null || generatedTreasures.isEmpty()) {
            Toast.makeText(this, "No treasures generated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringBuilder locationInfo = new StringBuilder();
        locationInfo.append("Treasure Locations:\n\n");
        
        for (int i = 0; i < Math.min(5, generatedTreasures.size()); i++) {
            TreasureLocation treasure = generatedTreasures.get(i);
            double distance = calculateDistance(selectedLatitude, selectedLongitude, 
                                              treasure.latitude, treasure.longitude);
            locationInfo.append(String.format("• %s treasure: %.0fm away\n", 
                                             treasure.type.name(), distance));
        }
        
        if (generatedTreasures.size() > 5) {
            locationInfo.append(String.format("... and %d more treasures", 
                                            generatedTreasures.size() - 5));
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Treasure Preview")
                .setMessage(locationInfo.toString())
                .setPositiveButton("Looks Good", (dialog, which) -> showSessionPreview())
                .setNeutralButton("Regenerate", (dialog, which) -> regenerateTreasures())
                .setNegativeButton("Edit More", (dialog, which) -> showTreasureEditOptions())
                .show();
    }
    
    /**
     * Clean up session draft if user cancels
     */
    private void cleanupSessionDraft() {
        if (currentSessionDraft != null) {
            // Clean up the session draft in background
            new Thread(() -> {
                try {
                    // Note: We would need a delete method in SessionManager for proper cleanup
                    // For now, we'll just clear the local reference
                    currentSessionDraft = null;
                    generatedTreasures = null;
                    treasuresGenerated = false;
                } catch (Exception e) {
                    // Log error but don't show to user since they're canceling anyway
                }
            }).start();
        }
        
        // Reset UI state
        startingPointSelected = false;
        treasuresGenerated = false;
        updateUI();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            checkPermissions();
            updateUI();
            
            if (permissionsGranted) {
                Toast.makeText(this, "Permissions granted! You can now set up your session.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Some permissions were denied. Please grant them in settings to continue.", Toast.LENGTH_LONG).show();
            }
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
}