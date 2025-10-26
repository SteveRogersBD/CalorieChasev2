package com.example.caloriechase;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.caloriechase.error.NetworkErrorHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapSearchActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float DEFAULT_ZOOM = 15f;
    private static final float MINI_MAP_ZOOM = 13f;
    private static final double DISTANCE_WARNING_THRESHOLD_KM = 1.0;
    
    // UI Components
    private GoogleMap mainMap;
    private GoogleMap miniMap;
    private SupportMapFragment mainMapFragment;
    private SupportMapFragment miniMapFragment;
    private TextInputEditText etSearch;
    private ImageButton btnBack;
    private FloatingActionButton fabCurrentLocation;
    private MaterialCardView cvDistanceWarning;
    private TextView tvSelectedAddress;
    private TextView tvDistanceFromCurrent;
    private MaterialButton btnNavigate;
    private MaterialButton btnChangeLocation;
    private MaterialButton btnUseThisPoint;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    
    // Location and Places
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private LatLng currentLocation;
    private LatLng selectedLocation;
    private Marker selectedMarker;
    private String selectedAddress;
    private float distanceGoal;
    
    // Error handling
    private NetworkErrorHandler networkErrorHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_search);
        
        // Enable edge-to-edge display
        enableEdgeToEdge();
        
        // Get distance goal from intent
        distanceGoal = getIntent().getFloatExtra("distance_goal", 1.0f);
        
        initViews();
        initPlaces();
        initLocation();
        setupListeners();
        setupBottomSheet();
        
        // Initialize error handling
        networkErrorHandler = new NetworkErrorHandler(this);
        
        // Initialize map
        mainMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mainMapFragment != null) {
            mainMapFragment.getMapAsync(this);
        }
        
        // Initialize mini map
        miniMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mini_map_fragment);
        if (miniMapFragment != null) {
            miniMapFragment.getMapAsync(this::onMiniMapReady);
        }
    }
    
    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        btnBack = findViewById(R.id.btn_back);
        fabCurrentLocation = findViewById(R.id.fab_current_location);
        cvDistanceWarning = findViewById(R.id.cv_distance_warning);
        tvSelectedAddress = findViewById(R.id.tv_selected_address);
        tvDistanceFromCurrent = findViewById(R.id.tv_distance_from_current);
        btnNavigate = findViewById(R.id.btn_navigate);
        btnChangeLocation = findViewById(R.id.btn_change_location);
        btnUseThisPoint = findViewById(R.id.btn_use_this_point);
    }
    
    private void initPlaces() {
        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);
        sessionToken = AutocompleteSessionToken.newInstance();
    }
    
    private void initLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getCurrentLocation();
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        fabCurrentLocation.setOnClickListener(v -> useCurrentLocation());
        
        btnNavigate.setOnClickListener(v -> openNavigation());
        
        btnChangeLocation.setOnClickListener(v -> hideBottomSheet());
        
        btnUseThisPoint.setOnClickListener(v -> confirmSelection());
        
        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    searchPlaces(s.toString());
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupBottomSheet() {
        View bottomSheet = findViewById(R.id.bottom_sheet_confirmation);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        
        // Apply bottom padding to account for system navigation bar
        applySystemBarInsets(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    clearSelection();
                }
            }
            
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
    }
    
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mainMap = googleMap;
        setupMap();
    }
    
    private void onMiniMapReady(@NonNull GoogleMap googleMap) {
        miniMap = googleMap;
        miniMap.getUiSettings().setAllGesturesEnabled(false);
        miniMap.getUiSettings().setMapToolbarEnabled(false);
    }
    
    private void setupMap() {
        if (mainMap == null) return;
        
        // Enable location if permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            mainMap.setMyLocationEnabled(true);
        }
        
        // Set up map click listener for regular click
        mainMap.setOnMapClickListener(this::onMapClick);
        
        // Move camera to current location if available
        if (currentLocation != null) {
            mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
        }
    }
    
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        if (mainMap != null) {
                            mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void useCurrentLocation() {
        if (currentLocation == null) {
            getCurrentLocation();
            Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        selectLocation(currentLocation, "Current Location");
    }
    
    private void onMapClick(LatLng latLng) {
        // Get address for the selected location
        getAddressFromLocation(latLng, address -> {
            selectLocation(latLng, address != null ? address : "Selected Location");
        });
    }
    
    private void searchPlaces(String query) {
        if (placesClient == null) return;
        
        // Create bounds around current location for better results
        RectangularBounds bounds = null;
        if (currentLocation != null) {
            double offset = 0.1; // Roughly 11km
            bounds = RectangularBounds.newInstance(
                    new LatLng(currentLocation.latitude - offset, currentLocation.longitude - offset),
                    new LatLng(currentLocation.latitude + offset, currentLocation.longitude + offset)
            );
        }
        
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setLocationBias(bounds)
                .setSessionToken(sessionToken)
                .setQuery(query)
                .build();
        
        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    if (!response.getAutocompletePredictions().isEmpty()) {
                        // For simplicity, select the first prediction
                        AutocompletePrediction prediction = response.getAutocompletePredictions().get(0);
                        fetchPlaceDetails(prediction.getPlaceId());
                    }
                })
                .addOnFailureListener(exception -> {
                    networkErrorHandler.handlePlacesApiError(exception, query, 
                            new NetworkErrorHandler.PlacesApiCallback() {
                        @Override
                        public void onRetryPlacesSearch(String retryQuery) {
                            searchPlaces(retryQuery);
                        }
                        
                        @Override
                        public void onUseManualSelection() {
                            Toast.makeText(MapSearchActivity.this, 
                                         "Search unavailable. Tap on map to select location.", 
                                         Toast.LENGTH_LONG).show();
                        }
                    });
                });
    }
    
    private void fetchPlaceDetails(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
        );
        
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);
        
        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    if (place.getLatLng() != null) {
                        String address = place.getAddress() != null ? place.getAddress() : place.getName();
                        selectLocation(place.getLatLng(), address);
                        
                        // Move camera to selected location
                        if (mainMap != null) {
                            mainMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), DEFAULT_ZOOM));
                        }
                    }
                })
                .addOnFailureListener(exception -> {
                    networkErrorHandler.handlePlacesApiError(exception, placeId, 
                            new NetworkErrorHandler.PlacesApiCallback() {
                        @Override
                        public void onRetryPlacesSearch(String retryQuery) {
                            fetchPlaceDetails(retryQuery);
                        }
                        
                        @Override
                        public void onUseManualSelection() {
                            Toast.makeText(MapSearchActivity.this, 
                                         "Place details unavailable. You can still use this location.", 
                                         Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }
    
    private void selectLocation(LatLng location, String address) {
        selectedLocation = location;
        selectedAddress = address;
        
        // Clear previous marker
        if (selectedMarker != null) {
            selectedMarker.remove();
        }
        
        // Add new marker
        if (mainMap != null) {
            selectedMarker = mainMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Selected Starting Point"));
        }
        
        // Update mini map
        if (miniMap != null) {
            miniMap.clear();
            miniMap.addMarker(new MarkerOptions().position(location));
            miniMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, MINI_MAP_ZOOM));
        }
        
        // Calculate distance from current location
        float distance = 0;
        if (currentLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    currentLocation.latitude, currentLocation.longitude,
                    location.latitude, location.longitude,
                    results
            );
            distance = results[0] / 1000; // Convert to kilometers
        }
        
        // Update UI
        updateConfirmationSheet(address, distance);
        showBottomSheet();
    }
    
    private void updateConfirmationSheet(String address, float distanceKm) {
        tvSelectedAddress.setText(address);
        
        if (currentLocation != null) {
            tvDistanceFromCurrent.setText(String.format(Locale.getDefault(), 
                    "%.1f km from your current location", distanceKm));
        } else {
            tvDistanceFromCurrent.setText("Distance from current location unknown");
        }
        
        // Show/hide distance warning
        if (distanceKm > DISTANCE_WARNING_THRESHOLD_KM) {
            cvDistanceWarning.setVisibility(View.VISIBLE);
            btnNavigate.setVisibility(View.VISIBLE);
        } else {
            cvDistanceWarning.setVisibility(View.GONE);
            btnNavigate.setVisibility(View.GONE);
        }
    }
    
    private void showBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
    
    private void hideBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }
    
    private void clearSelection() {
        if (selectedMarker != null) {
            selectedMarker.remove();
            selectedMarker = null;
        }
        selectedLocation = null;
        selectedAddress = null;
        
        if (miniMap != null) {
            miniMap.clear();
        }
    }
    
    private void openNavigation() {
        if (selectedLocation == null) return;
        
        try {
            Uri gmmIntentUri = Uri.parse(String.format(Locale.getDefault(),
                    "google.navigation:q=%f,%f", 
                    selectedLocation.latitude, selectedLocation.longitude));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Fallback to web maps
                Uri webUri = Uri.parse(String.format(Locale.getDefault(),
                        "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                        selectedLocation.latitude, selectedLocation.longitude));
                Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
                startActivity(webIntent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open navigation", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void confirmSelection() {
        if (selectedLocation == null) {
            Toast.makeText(this, "No location selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Return result to SessionSetupActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_latitude", selectedLocation.latitude);
        resultIntent.putExtra("selected_longitude", selectedLocation.longitude);
        resultIntent.putExtra("selected_address", selectedAddress);
        resultIntent.putExtra("distance_from_current", calculateDistanceFromCurrent());
        
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    private float calculateDistanceFromCurrent() {
        if (currentLocation == null || selectedLocation == null) {
            return 0;
        }
        
        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                selectedLocation.latitude, selectedLocation.longitude,
                results
        );
        return results[0] / 1000; // Convert to kilometers
    }
    
    private void getAddressFromLocation(LatLng location, AddressCallback callback) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.latitude, location.longitude, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                callback.onAddressReceived(addressText);
            } else {
                callback.onAddressReceived(null);
            }
        } catch (IOException e) {
            networkErrorHandler.handleGeocodingError(e, location, 
                    new NetworkErrorHandler.GeocodingCallback() {
                @Override
                public void onRetryGeocoding(LatLng retryLocation) {
                    getAddressFromLocation(retryLocation, callback);
                }
                
                @Override
                public void onUseFallbackAddress(String fallbackAddress) {
                    callback.onAddressReceived(fallbackAddress);
                }
            });
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
                setupMap();
            } else {
                Toast.makeText(this, "Location permission is required for this feature", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (selectedMarker != null) {
            selectedMarker.remove();
        }
    }
    
    // Callback interface for address geocoding
    private interface AddressCallback {
        void onAddressReceived(String address);
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
                    v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        systemBars.bottom + 24 // Add extra padding for bottom sheet
                    );
                    return insets;
                } else {
                    v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        insets.getSystemWindowInsetBottom() + 24
                    );
                    return insets.consumeSystemWindowInsets();
                }
            });
        }
    }
}