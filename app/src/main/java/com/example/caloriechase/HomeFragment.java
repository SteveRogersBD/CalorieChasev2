package com.example.caloriechase;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.example.caloriechase.error.ErrorHandler;
import com.example.caloriechase.error.ServiceRecoveryManager;

import java.util.Locale;


public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private MaterialButton btnStartSession;
    private Location currentLocation;
    
    // Active session overlay
    private LinearLayout llActiveSessionOverlay;
    
    // Error handling
    private ErrorHandler errorHandler;
    private ServiceRecoveryManager recoveryManager;
    private TextView tvActiveSessionDistance;
    private TextView tvActiveSessionSteps;
    private TextView tvActiveSessionCalories;
    private TextView tvActiveSessionTime;
    
    // Broadcast receiver for session updates
    private BroadcastReceiver sessionUpdateReceiver;
    private boolean isActiveSessionVisible = false;
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        initViews(view);
        initMap();
        initLocationClient();
        initErrorHandling();
        
        return view;
    }
    
    private void initViews(View view) {
        btnStartSession = view.findViewById(R.id.btn_start_session);
        btnStartSession.setOnClickListener(v -> startSessionSetup());
        
        // Initialize active session overlay
        llActiveSessionOverlay = view.findViewById(R.id.ll_active_session_overlay);
        tvActiveSessionDistance = view.findViewById(R.id.tv_active_session_distance);
        tvActiveSessionSteps = view.findViewById(R.id.tv_active_session_steps);
        tvActiveSessionCalories = view.findViewById(R.id.tv_active_session_calories);
        tvActiveSessionTime = view.findViewById(R.id.tv_active_session_time);
        
        // Set up click listener to open active session
        if (llActiveSessionOverlay != null) {
            llActiveSessionOverlay.setOnClickListener(v -> openActiveSession());
        }
    }
    
    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    
    private void initLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        // Configure map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        
        // Enable location if permission granted
        enableMyLocation();
        
        // Set default location (San Francisco)
        LatLng defaultLocation = new LatLng(37.7749, -122.4194);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
    }
    
    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
            
        } else {
            // Request location permission
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            currentLocation = location;
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            
                            // Add custom user marker with converted bitmap
                            BitmapDescriptor customIcon = getBitmapDescriptorFromVector(R.drawable.ic_user_marker);
                            mMap.addMarker(new MarkerOptions()
                                    .position(userLocation)
                                    .title("You are here")
                                    .snippet("Current location")
                                    .icon(customIcon));
                            
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));
                        }
                    });
        }
    }
    
    /**
     * Convert vector drawable to BitmapDescriptor for Google Maps marker
     */
    private BitmapDescriptor getBitmapDescriptorFromVector(int vectorResId) {
        try {
            Drawable vectorDrawable = ContextCompat.getDrawable(requireContext(), vectorResId);
            if (vectorDrawable == null) {
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
            }
            
            // Set size for the marker (48dp converted to pixels)
            int width = (int) (48 * getResources().getDisplayMetrics().density);
            int height = (int) (48 * getResources().getDisplayMetrics().density);
            
            vectorDrawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.draw(canvas);
            
            return BitmapDescriptorFactory.fromBitmap(bitmap);
            
        } catch (Exception e) {
            // Fallback to default orange marker if conversion fails
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
        }
    }
    
    private void initErrorHandling() {
        errorHandler = new ErrorHandler(requireContext());
        recoveryManager = new ServiceRecoveryManager(requireContext());
        
        // Check for service recovery on fragment initialization
        checkServiceRecovery();
    }
    
    private void checkServiceRecovery() {
        if (recoveryManager.isRecoveryNeeded()) {
            recoveryManager.attemptServiceRecovery(new ServiceRecoveryManager.ServiceRecoveryCallback() {
                @Override
                public void onRecoverySuccessful(com.example.caloriechase.data.ActiveSession recoveredSession, 
                                               ErrorHandler.ServiceRecoveryType recoveryType) {
                    // Show active session overlay
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (llActiveSessionOverlay != null) {
                                llActiveSessionOverlay.setVisibility(View.VISIBLE);
                                btnStartSession.setVisibility(View.GONE);
                                isActiveSessionVisible = true;
                            }
                        });
                    }
                }
                
                @Override
                public void onRecoveryFailed(String reason) {
                    // Recovery failed, continue normally
                }
                
                @Override
                public void onRecoveryNotNeeded() {
                    // No recovery needed, continue normally
                }
            });
        }
    }
    
    private void startSessionSetup() {
        // Check for location permission before starting session setup
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            
            errorHandler.handlePermissionDenied(ErrorHandler.PermissionType.LOCATION, 
                    new ErrorHandler.PermissionDenialCallback() {
                @Override
                public void onSettingsOpened() {
                    // User went to settings
                }
                
                @Override
                public void onContinueWithoutPermission() {
                    // Start session setup anyway with limited functionality
                    launchSessionSetup();
                }
                
                @Override
                public void onCancel() {
                    // User cancelled, don't start session setup
                }
            });
        } else {
            launchSessionSetup();
        }
    }
    
    private void launchSessionSetup() {
        try {
            Intent intent = new Intent(requireContext(), SessionSetupActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            errorHandler.handleSessionError(ErrorHandler.SessionErrorType.SESSION_CREATION_FAILED, 
                    e.getMessage(), new ErrorHandler.SessionErrorCallback() {
                @Override
                public void onRetry() {
                    launchSessionSetup();
                }
                
                @Override
                public void onCancelSession() {
                    // Don't start session
                }
                
                @Override
                public void onContinueAnyway() {
                    // Try to start anyway
                    launchSessionSetup();
                }
            });
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        registerSessionUpdateReceiver();
        checkForActiveSession();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        unregisterSessionUpdateReceiver();
    }
    
    /**
     * Register broadcast receiver for session updates
     */
    private void registerSessionUpdateReceiver() {
        if (sessionUpdateReceiver == null) {
            sessionUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    handleSessionUpdate(intent);
                }
            };
        }
        
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                sessionUpdateReceiver, 
                new IntentFilter(TrackingService.BROADCAST_SESSION_UPDATE)
        );
    }
    
    /**
     * Unregister broadcast receiver
     */
    private void unregisterSessionUpdateReceiver() {
        if (sessionUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(sessionUpdateReceiver);
        }
    }
    
    /**
     * Handle session update broadcasts from TrackingService
     */
    private void handleSessionUpdate(Intent intent) {
        if (intent == null) return;
        
        int steps = intent.getIntExtra(TrackingService.EXTRA_STEPS, 0);
        float distance = intent.getFloatExtra(TrackingService.EXTRA_DISTANCE, 0.0f);
        int calories = intent.getIntExtra(TrackingService.EXTRA_CALORIES, 0);
        long duration = intent.getLongExtra(TrackingService.EXTRA_DURATION, 0);
        
        updateActiveSessionOverlay(steps, distance, calories, duration);
    }
    
    /**
     * Update the active session overlay with live metrics
     */
    private void updateActiveSessionOverlay(int steps, float distance, int calories, long duration) {
        if (llActiveSessionOverlay == null) return;
        
        // Show overlay if not visible
        if (!isActiveSessionVisible) {
            llActiveSessionOverlay.setVisibility(View.VISIBLE);
            btnStartSession.setVisibility(View.GONE);
            isActiveSessionVisible = true;
        }
        
        // Update metrics using FitnessTracker for consistent calculations
        if (tvActiveSessionDistance != null) {
            tvActiveSessionDistance.setText(String.format(Locale.getDefault(), "%.2f km", distance));
        }
        if (tvActiveSessionSteps != null) {
            tvActiveSessionSteps.setText(String.valueOf(steps));
        }
        if (tvActiveSessionCalories != null) {
            // Use FitnessTracker for calorie calculation if needed
            int calculatedCalories = calories > 0 ? calories : FitnessTracker.calculateCaloriesFromSteps(requireContext(), steps);
            tvActiveSessionCalories.setText(String.valueOf(calculatedCalories));
        }
        if (tvActiveSessionTime != null) {
            tvActiveSessionTime.setText(FitnessTracker.formatDuration(duration));
        }
    }
    
    /**
     * Check if there's an active session on fragment resume
     */
    private void checkForActiveSession() {
        // Check if tracking service is running
        TrackingServiceManager serviceManager = new TrackingServiceManager(requireContext());
        if (serviceManager.isServiceRunning()) {
            // Service is running, overlay should be visible
            if (llActiveSessionOverlay != null && !isActiveSessionVisible) {
                llActiveSessionOverlay.setVisibility(View.VISIBLE);
                btnStartSession.setVisibility(View.GONE);
                isActiveSessionVisible = true;
            }
        } else {
            // No active session, hide overlay
            hideActiveSessionOverlay();
        }
    }
    
    /**
     * Hide the active session overlay
     */
    private void hideActiveSessionOverlay() {
        if (llActiveSessionOverlay != null && isActiveSessionVisible) {
            llActiveSessionOverlay.setVisibility(View.GONE);
            btnStartSession.setVisibility(View.VISIBLE);
            isActiveSessionVisible = false;
        }
    }
    
    /**
     * Open the active session activity
     */
    private void openActiveSession() {
        Intent intent = new Intent(requireContext(), ActiveSessionActivity.class);
        startActivity(intent);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission required for tracking", Toast.LENGTH_LONG).show();
            }
        }
    }
}