package com.example.caloriechase;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays a full-screen Google Map with the selected starting point
 * and a circular track representing the chosen distance
 */
public class SessionMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private GoogleMap mMap;
    private double startLatitude;
    private double startLongitude;
    private float distance;
    private String startAddress;
    private List<LatLng> treasureLocations;
    private List<LatLng> trackPoints;
    
    // AI-generated points
    private boolean useAiPoints = false;
    private double[] aiPointsLats;
    private double[] aiPointsLngs;
    private String[] aiPointsNames;
    
    // Directions API route
    private boolean useDirectionsRoute = false;
    private double[] directionsRouteLats;
    private double[] directionsRouteLngs;
    
    private FloatingActionButton fabStartSession;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_map);
        
        // Enable edge-to-edge display
        enableEdgeToEdge();
        
        // Get data from intent
        Intent intent = getIntent();
        startLatitude = intent.getDoubleExtra("start_latitude", 0);
        startLongitude = intent.getDoubleExtra("start_longitude", 0);
        distance = intent.getFloatExtra("distance", 1.0f);
        startAddress = intent.getStringExtra("start_address");
        
        // Check for Directions API route
        useDirectionsRoute = intent.getBooleanExtra("use_directions_route", false);
        if (useDirectionsRoute) {
            directionsRouteLats = intent.getDoubleArrayExtra("directions_route_latitudes");
            directionsRouteLngs = intent.getDoubleArrayExtra("directions_route_longitudes");
        }
        
        // Check for AI-generated points
        useAiPoints = intent.getBooleanExtra("use_ai_points", false);
        if (useAiPoints) {
            aiPointsLats = intent.getDoubleArrayExtra("ai_points_latitudes");
            aiPointsLngs = intent.getDoubleArrayExtra("ai_points_longitudes");
            aiPointsNames = intent.getStringArrayExtra("ai_points_names");
        }
        
        // Initialize views
        fabStartSession = findViewById(R.id.fab_start_session);
        fabStartSession.setOnClickListener(v -> {
            Log.d("SessionMapActivity", "FAB button clicked!");
            showSessionRecapDialogBeforeStart();
        });
        
        // Initialize treasure locations list
        treasureLocations = new ArrayList<>();
        
        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Configure map for navigation/walking style
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        
        // Set map type to hybrid for better navigation view
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        
        // Enable 3D buildings and indoor maps for navigation feel
        mMap.setBuildingsEnabled(true);
        mMap.setIndoorEnabled(true);
        googleMap.setTrafficEnabled(true);
        
        // Add starting point marker
        LatLng startPoint = new LatLng(startLatitude, startLongitude);
        mMap.addMarker(new MarkerOptions()
                .position(startPoint)
                .title("Starting Point")
                .snippet(startAddress != null ? startAddress : "Your treasure hunt begins here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        
        // Draw track based on Directions API route, AI points, or random generation
        if (useDirectionsRoute && directionsRouteLats != null && directionsRouteLngs != null) {
            trackPoints = drawDirectionsRoute(startPoint);
        } else if (useAiPoints && aiPointsLats != null && aiPointsLngs != null) {
            trackPoints = drawAiGeneratedTrack(startPoint);
        } else {
            trackPoints = drawSimpleTrack(startPoint);
        }
        
        Log.d("SessionMapActivity", "Track drawn with " + trackPoints.size() + " points");
        Log.d("SessionMapActivity", "Treasures generated: " + treasureLocations.size());
        
        // Move camera to show the entire track
        setupNavigationView(trackPoints);
        
        // Don't show recap here - it will show when user clicks "Start Gameplay"
    }
    private List<LatLng> drawDirectionsRoute(LatLng startPoint) {
        List<LatLng> trackPoints = new ArrayList<>();
        
        // Convert directions route to LatLng list
        for (int i = 0; i < directionsRouteLats.length; i++) {
            LatLng point = new LatLng(directionsRouteLats[i], directionsRouteLngs[i]);
            trackPoints.add(point);
        }
        
        // Draw the entire route as a polyline
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(trackPoints)
                .color(ContextCompat.getColor(this, R.color.primary_orange))
                .width(10f)
                .clickable(false);
        mMap.addPolyline(polylineOptions);
        
        // Add treasure markers along the route at regular intervals
        int treasureCount = Math.max(5, (int)(distance * 3)); // ~3 treasures per km
        int pointsPerTreasure = Math.max(1, trackPoints.size() / treasureCount);
        
        for (int i = pointsPerTreasure; i < trackPoints.size(); i += pointsPerTreasure) {
            LatLng treasurePoint = trackPoints.get(i);
            addDirectionsTreasureMarker(treasurePoint, treasureLocations.size() + 1);
        }
        
        // Add destination marker at the end
        if (!trackPoints.isEmpty()) {
            LatLng destination = trackPoints.get(trackPoints.size() - 1);
            mMap.addMarker(new MarkerOptions()
                    .position(destination)
                    .title("Destination")
                    .snippet("Follow the route to reach here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
        
        Log.d("SessionMapActivity", "Directions route drawn with " + trackPoints.size() + " points");
        
        return trackPoints;
    }
    
    private void addDirectionsTreasureMarker(LatLng position, int treasureNumber) {
        if (treasureLocations == null) treasureLocations = new ArrayList<>();
        treasureLocations.add(position);
        
        String treasureType;
        if (treasureNumber % 8 == 0) {
            treasureType = "Epic Treasure";
        } else if (treasureNumber % 4 == 0) {
            treasureType = "Rare Treasure";
        } else {
            treasureType = "Common Treasure";
        }
        
        BitmapDescriptor goldIcon = createScaledGoldIcon();
        
        mMap.addMarker(new MarkerOptions()
                .position(position)
                .title(treasureType)
                .snippet("On the route")
                .icon(goldIcon));
    }
    
    private List<LatLng> drawAiGeneratedTrack(LatLng startPoint) {
        List<LatLng> trackPoints = new ArrayList<>();
        trackPoints.add(startPoint);
        
        // Convert AI points to LatLng and create track
        LatLng currentPoint = startPoint;
        
        for (int i = 0; i < aiPointsLats.length; i++) {
            LatLng aiPoint = new LatLng(aiPointsLats[i], aiPointsLngs[i]);
            trackPoints.add(aiPoint);
            
            // Draw segment from current point to AI point
            drawSegment(currentPoint, aiPoint);
            
            // Add treasure marker at AI point
            String treasureName = (aiPointsNames != null && i < aiPointsNames.length) 
                                  ? aiPointsNames[i] : "Treasure " + (i + 1);
            addAiTreasureMarker(aiPoint, treasureName, i);
            
            currentPoint = aiPoint;
        }
        
        // Close the loop back to start
        trackPoints.add(startPoint);
        drawSegment(currentPoint, startPoint);
        
        Log.d("SessionMapActivity", "AI track drawn with " + aiPointsLats.length + " AI points");
        
        return trackPoints;
    }
    
    private void addAiTreasureMarker(LatLng position, String name, int index) {
        if (treasureLocations == null) treasureLocations = new ArrayList<>();
        treasureLocations.add(position);
        
        String treasureType;
        if (index % 8 == 0) {
            treasureType = "Epic Treasure";
        } else if (index % 4 == 0) {
            treasureType = "Rare Treasure";
        } else {
            treasureType = "Common Treasure";
        }
        
        BitmapDescriptor goldIcon = createScaledGoldIcon();
        
        mMap.addMarker(new MarkerOptions()
                .position(position)
                .title(treasureType + ": " + name)
                .snippet("AI-generated location")
                .icon(goldIcon));
    }
    
    private List<LatLng> drawSimpleTrack(LatLng startPoint) {
        List<LatLng> trackPoints = new ArrayList<>();
        trackPoints.add(startPoint);
        
        // Closed loop algorithm: create a random path that returns to start
        // Reserve 15% of distance for the return journey
        float outboundDistance = distance * 0.85f; // 85% for outbound journey
        float returnDistance = distance * 0.15f;    // 15% for return journey
        
        LatLng currentPoint = startPoint;
        double currentBearing = Math.random() * 360; // Start with random direction
        float remainingOutbound = outboundDistance;
        
        // Phase 1: Create outbound path with random segments
        while (remainingOutbound > 0) {
            // Random segment length between 100m and 400m
            float minSegment = 0.1f;
            float maxSegment = 0.4f;
            float randomSegmentLength = minSegment + (float)(Math.random() * (maxSegment - minSegment));
            
            // Use smaller segment if remaining distance is less
            float currentSegmentLength = Math.min(randomSegmentLength, remainingOutbound);
            
            // Random direction change: turn between -90° to +90° (less extreme turns)
            double turnAngle = -90 + (Math.random() * 180);
            currentBearing = (currentBearing + turnAngle) % 360;
            if (currentBearing < 0) currentBearing += 360;
            
            // Calculate next point
            LatLng nextPoint = calculateDestination(currentPoint, currentSegmentLength, currentBearing);
            trackPoints.add(nextPoint);
            
            // Draw line segment
            drawSegment(currentPoint, nextPoint);
            
            // Add treasure marker
            addTreasureMarker(nextPoint, trackPoints.size(), currentSegmentLength);
            
            // Update for next iteration
            currentPoint = nextPoint;
            remainingOutbound -= currentSegmentLength;
        }
        
        // Phase 2: Return to start point
        // Calculate direct distance back to start
        double distanceToStart = calculateDistance(currentPoint, startPoint);
        
        if (distanceToStart > returnDistance * 1000) {
            // If too far, create intermediate point closer to start
            double bearingToStart = calculateBearing(currentPoint, startPoint);
            LatLng intermediatePoint = calculateDestination(currentPoint, returnDistance * 0.7f, bearingToStart);
            
            trackPoints.add(intermediatePoint);
            drawSegment(currentPoint, intermediatePoint);
            addTreasureMarker(intermediatePoint, trackPoints.size(), returnDistance * 0.7f);
            
            currentPoint = intermediatePoint;
        }
        
        // Final segment back to start
        trackPoints.add(startPoint);
        drawSegment(currentPoint, startPoint);
        
        return trackPoints;
    }
    
    private void drawSegment(LatLng from, LatLng to) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(from, to)
                .color(ContextCompat.getColor(this, R.color.primary_orange))
                .width(8f)
                .clickable(false);
        mMap.addPolyline(polylineOptions);
    }

    private void addTreasureMarker(LatLng position, int segmentNumber, float segmentLength) {
        // make sure your list is not null
        if (treasureLocations == null) treasureLocations = new ArrayList<>();
        treasureLocations.add(position); // <-- add this line

        String treasureType;
        if (segmentNumber % 8 == 0) {
            treasureType = "Epic Treasure";
        } else if (segmentNumber % 4 == 0) {
            treasureType = "Rare Treasure";
        } else {
            treasureType = "Common Treasure";
        }

        BitmapDescriptor goldIcon = createScaledGoldIcon();

        mMap.addMarker(new MarkerOptions()
                .position(position)
                .title(treasureType)
                .snippet(String.format("Segment: %.0fm", segmentLength * 1000))
                .icon(goldIcon));
    }


    private double calculateDistance(LatLng point1, LatLng point2) {
        // Calculate distance between two points in meters using Haversine formula
        double earthRadius = 6371000; // Earth radius in meters
        
        double lat1Rad = Math.toRadians(point1.latitude);
        double lat2Rad = Math.toRadians(point2.latitude);
        double deltaLatRad = Math.toRadians(point2.latitude - point1.latitude);
        double deltaLngRad = Math.toRadians(point2.longitude - point1.longitude);
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadius * c;
    }
    
    private double calculateBearing(LatLng from, LatLng to) {
        // Calculate bearing from one point to another
        double lat1Rad = Math.toRadians(from.latitude);
        double lat2Rad = Math.toRadians(to.latitude);
        double deltaLngRad = Math.toRadians(to.longitude - from.longitude);
        
        double y = Math.sin(deltaLngRad) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLngRad);
        
        double bearingRad = Math.atan2(y, x);
        return Math.toDegrees(bearingRad);
    }
    
    private LatLng calculateDestination(LatLng start, float distanceKm, double bearingDegrees) {
        // Convert distance to meters
        double distanceMeters = distanceKm * 1000;
        
        // Convert bearing to radians
        double bearingRadians = Math.toRadians(bearingDegrees);
        
        // Earth's radius in meters
        double earthRadius = 6371000;
        
        // Convert start coordinates to radians
        double lat1 = Math.toRadians(start.latitude);
        double lng1 = Math.toRadians(start.longitude);
        
        // Calculate destination coordinates
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distanceMeters / earthRadius) +
                               Math.cos(lat1) * Math.sin(distanceMeters / earthRadius) * Math.cos(bearingRadians));
        
        double lng2 = lng1 + Math.atan2(Math.sin(bearingRadians) * Math.sin(distanceMeters / earthRadius) * Math.cos(lat1),
                                       Math.cos(distanceMeters / earthRadius) - Math.sin(lat1) * Math.sin(lat2));
        
        // Convert back to degrees
        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lng2));
    }
    
    private BitmapDescriptor createScaledGoldIcon() {
        // Load the original gold image
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.gold);
        
        // Scale it down to marker size (similar to default markers)
        int width = 48;  // Small marker size (same as default markers)
        int height = 48;
        
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);
        
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap);
    }
    
    private List<LatLng> drawTrackPolygon(LatLng center) {
        // Calculate number of sides based on distance for better shape
        int sides = calculatePolygonSides(distance);
        
        // Calculate radius from perimeter: perimeter = distance (km) * 1000 (m)
        // For regular polygon: perimeter = sides * sideLength
        // sideLength = 2 * radius * sin(π/sides)
        // Therefore: radius = perimeter / (2 * sides * sin(π/sides))
        double perimeterInMeters = distance * 1000;
        double angleStep = Math.PI / sides;
        double radius = perimeterInMeters / (2 * sides * Math.sin(angleStep));
        
        // Create polygon vertices
        List<LatLng> vertices = new ArrayList<>();
        for (int i = 0; i < sides; i++) {
            double angle = 2 * Math.PI * i / sides;
            
            // Convert radius from meters to degrees
            double latOffset = (radius * Math.cos(angle)) / 111320; // 1 degree lat ≈ 111320 meters
            double lngOffset = (radius * Math.sin(angle)) / (111320 * Math.cos(Math.toRadians(center.latitude)));
            
            LatLng vertex = new LatLng(
                center.latitude + latOffset,
                center.longitude + lngOffset
            );
            vertices.add(vertex);
        }
        
        // Draw the main track polygon
        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(vertices)
                .strokeColor(ContextCompat.getColor(this, R.color.primary_orange))
                .strokeWidth(8f)
                .fillColor(Color.argb(30, 255, 107, 53)) // Semi-transparent orange
                .clickable(false);
        
        mMap.addPolygon(polygonOptions);
        
        // Draw inner guide polygon (half perimeter) for longer distances
        if (distance > 2.0f) {
            List<LatLng> innerVertices = new ArrayList<>();
            double innerRadius = radius * 0.6; // Smaller inner polygon
            
            for (int i = 0; i < sides; i++) {
                double angle = 2 * Math.PI * i / sides;
                
                double latOffset = (innerRadius * Math.cos(angle)) / 111320;
                double lngOffset = (innerRadius * Math.sin(angle)) / (111320 * Math.cos(Math.toRadians(center.latitude)));
                
                LatLng vertex = new LatLng(
                    center.latitude + latOffset,
                    center.longitude + lngOffset
                );
                innerVertices.add(vertex);
            }
            
            PolygonOptions innerPolygonOptions = new PolygonOptions()
                    .addAll(innerVertices)
                    .strokeColor(ContextCompat.getColor(this, R.color.secondary_teal))
                    .strokeWidth(4f)
                    .fillColor(Color.TRANSPARENT)
                    .clickable(false);
            
            mMap.addPolygon(innerPolygonOptions);
        }
        
        return vertices;
    }
    
    private int calculatePolygonSides(float distance) {
        // Calculate appropriate number of sides based on distance
        // More sides for longer distances to create smoother shapes
        if (distance <= 1.0f) return 6;      // Hexagon for short distances
        else if (distance <= 2.0f) return 8;  // Octagon
        else if (distance <= 5.0f) return 10; // Decagon
        else if (distance <= 10.0f) return 12; // 12-sided
        else if (distance <= 20.0f) return 16; // 16-sided
        else return 20; // 20-sided for very long distances (nearly circular)
    }
    
    private void drawTreasureIndicators(LatLng center, List<LatLng> trackVertices) {
        if (trackVertices.isEmpty()) return;
        
        // Calculate number of treasures based on distance
        int treasureCount = Math.max(4, Math.min(20, (int)(distance * 3)));
        
        // Place treasures ON the polygon perimeter
        for (int i = 0; i < treasureCount; i++) {
            LatLng treasurePos;
            
            if (i < trackVertices.size()) {
                // Place treasures at polygon vertices first
                treasurePos = trackVertices.get(i);
            } else {
                // Place additional treasures on polygon edges (midpoints)
                int edgeIndex = (i - trackVertices.size()) % trackVertices.size();
                LatLng vertex1 = trackVertices.get(edgeIndex);
                LatLng vertex2 = trackVertices.get((edgeIndex + 1) % trackVertices.size());
                
                // Calculate midpoint of the edge
                double midLat = (vertex1.latitude + vertex2.latitude) / 2;
                double midLng = (vertex1.longitude + vertex2.longitude) / 2;
                treasurePos = new LatLng(midLat, midLng);
            }
            
            // Determine treasure type based on index
            String treasureType;
            float hue;
            if (i % 8 == 0) {
                treasureType = "Epic Treasure";
                hue = BitmapDescriptorFactory.HUE_VIOLET;
            } else if (i % 4 == 0) {
                treasureType = "Rare Treasure";
                hue = BitmapDescriptorFactory.HUE_BLUE;
            } else {
                treasureType = "Common Treasure";
                hue = BitmapDescriptorFactory.HUE_YELLOW;
            }
            
            mMap.addMarker(new MarkerOptions()
                    .position(treasurePos)
                    .title(treasureType)
                    .snippet("Walk along the path to collect this treasure!")
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        }
    }
    
    private double calculateAverageRadius(LatLng center, List<LatLng> vertices) {
        if (vertices.isEmpty()) return 0;
        
        double totalDistance = 0;
        for (LatLng vertex : vertices) {
            // Calculate distance from center to vertex in meters
            double latDiff = (vertex.latitude - center.latitude) * 111320;
            double lngDiff = (vertex.longitude - center.longitude) * 111320 * Math.cos(Math.toRadians(center.latitude));
            double distance = Math.sqrt(latDiff * latDiff + lngDiff * lngDiff);
            totalDistance += distance;
        }
        
        return totalDistance / vertices.size();
    }
    
    private void setupNavigationView(List<LatLng> trackPoints) {
        if (trackPoints.isEmpty()) return;
        
        // Calculate bounds to include all track points
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        
        for (LatLng point : trackPoints) {
            boundsBuilder.include(point);
        }
        
        LatLngBounds bounds = boundsBuilder.build();
        
        // Add padding around the bounds (in pixels)
        int padding = 150; // 150 pixels padding
        
        // Move camera to show all track points with padding
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding), 2000, null);
    }
    
    private float calculateNavigationZoom(float distance) {
        // Much closer zoom levels to show entire track in detail
        if (distance <= 0.5f) return 19f;      // Very close for short walks
        else if (distance <= 1.0f) return 18f;  // Close street-level view
        else if (distance <= 2.0f) return 17f;
        else if (distance <= 3.0f) return 16.5f;
        else if (distance <= 5.0f) return 16f;
        else if (distance <= 8.0f) return 15.5f;
        else if (distance <= 10.0f) return 15f;
        else if (distance <= 15.0f) return 14.5f;
        else if (distance <= 20.0f) return 14f;
        else if (distance <= 30.0f) return 13.5f;
        else return 13f;
    }
    
    private void startActiveSession() {
        // Create intent to start the gameplay activity
        Intent intent = new Intent(this, GameplayActivity.class);
        intent.putExtra("start_latitude", startLatitude);
        intent.putExtra("start_longitude", startLongitude);
        intent.putExtra("distance_goal", distance);
        intent.putExtra("start_address", startAddress);
        
        // Pass treasure locations to gameplay activity
        if (treasureLocations != null && !treasureLocations.isEmpty()) {
            double[] latitudes = new double[treasureLocations.size()];
            double[] longitudes = new double[treasureLocations.size()];
            
            for (int i = 0; i < treasureLocations.size(); i++) {
                LatLng location = treasureLocations.get(i);
                latitudes[i] = location.latitude;
                longitudes[i] = location.longitude;
            }
            
            intent.putExtra("treasure_latitudes", latitudes);
            intent.putExtra("treasure_longitudes", longitudes);
        }
        
        // Pass track path to gameplay activity
        if (trackPoints != null && !trackPoints.isEmpty()) {
            double[] trackLats = new double[trackPoints.size()];
            double[] trackLngs = new double[trackPoints.size()];
            
            for (int i = 0; i < trackPoints.size(); i++) {
                LatLng point = trackPoints.get(i);
                trackLats[i] = point.latitude;
                trackLngs[i] = point.longitude;
            }
            
            intent.putExtra("track_latitudes", trackLats);
            intent.putExtra("track_longitudes", trackLngs);
        }
        
        startActivity(intent);
    }
    
    /**
     * Show countdown (3, 2, 1) before starting gameplay
     */
    private void showCountdownAndStart() {
        // Create countdown dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setCancelable(false);
        
        // Create custom view for countdown
        TextView countdownText = new TextView(this);
        countdownText.setTextSize(72);
        countdownText.setGravity(android.view.Gravity.CENTER);
        countdownText.setTextColor(getColor(R.color.primary_orange));
        countdownText.setPadding(100, 100, 100, 100);
        
        builder.setView(countdownText);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // Countdown sequence
        android.os.Handler handler = new android.os.Handler();
        
        // 3
        countdownText.setText("3");
        handler.postDelayed(() -> {
            // 2
            countdownText.setText("2");
            handler.postDelayed(() -> {
                // 1
                countdownText.setText("1");
                handler.postDelayed(() -> {
                    // GO!
                    countdownText.setText("GO!");
                    countdownText.setTextColor(getColor(R.color.success));
                    handler.postDelayed(() -> {
                        dialog.dismiss();
                        startActiveSession();
                    }, 500);
                }, 1000);
            }, 1000);
        }, 1000);
    }
    
    /**
     * Show session recap dialog before starting (with Let's Go button)
     */
    private void showSessionRecapDialogBeforeStart() {
        // Calculate total points (25 for common, 50 for rare, 100 for epic)
        int totalPoints = 0;
        int commonCount = 0;
        int rareCount = 0;
        int epicCount = 0;
        
        for (int i = 0; i < treasureLocations.size(); i++) {
            if (i % 8 == 0) {
                epicCount++;
                totalPoints += 100;
            } else if (i % 4 == 0) {
                rareCount++;
                totalPoints += 50;
            } else {
                commonCount++;
                totalPoints += 25;
            }
        }
        
        // Get destination name (if using AI points)
        String destination = "Complete the route";
        if (useAiPoints && aiPointsNames != null && aiPointsNames.length > 0) {
            destination = aiPointsNames[0];
        }
        
        // Build recap message
        String message = String.format(
            "🎯 Distance: %.1f km\n\n" +
            "🏁 Destination: %s\n\n" +
            "💎 Total Coins: %d\n" +
            "   • %d Common (25 pts)\n" +
            "   • %d Rare (50 pts)\n" +
            "   • %d Epic (100 pts)\n\n" +
            "🏆 Total Points Available: %,d\n\n" +
            "Ready to start your treasure hunt?",
            distance,
            destination,
            treasureLocations.size(),
            commonCount,
            rareCount,
            epicCount,
            totalPoints
        );
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🗺️ Session Overview")
                .setMessage(message)
                .setPositiveButton("Let's Go!", (dialog, which) -> {
                    // Start countdown after user clicks Let's Go!
                    showCountdownAndStart();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled, stay on map
                })
                .setCancelable(true)
                .show();
    }
    
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Return to SessionSetupActivity
        finish();
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