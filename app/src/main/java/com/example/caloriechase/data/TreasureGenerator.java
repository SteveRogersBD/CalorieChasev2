package com.example.caloriechase.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Algorithmic treasure placement generator for treasure hunt sessions.
 * Generates treasure locations based on distance goals and geographic constraints.
 */
public class TreasureGenerator {
    
    // Constants for treasure generation
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double MIN_TREASURE_DISTANCE_METERS = 100.0; // Minimum distance between treasures
    private static final double MAX_TREASURE_DISTANCE_FROM_START_METERS = 2500.0; // Maximum distance from start
    private static final int MAX_PLACEMENT_ATTEMPTS = 50; // Maximum attempts to place a treasure
    
    // Treasure density configuration (treasures per km)
    private static final double BASE_TREASURE_DENSITY = 3.0; // Base treasures per km
    private static final double MIN_TREASURES = 3; // Minimum treasures regardless of distance
    private static final double MAX_TREASURES = 15; // Maximum treasures to avoid overcrowding
    
    private final Random random;
    
    public TreasureGenerator() {
        this.random = new Random();
    }
    
    /**
     * Generate treasures for a session using ring distribution algorithm
     * 
     * @param sessionId The session ID to associate treasures with
     * @param startLatitude Starting point latitude
     * @param startLongitude Starting point longitude
     * @param distanceGoalKm Target distance goal in kilometers
     * @return List of generated treasure locations
     */
    public List<TreasureLocation> generateTreasureRing(String sessionId, double startLatitude, 
                                                      double startLongitude, float distanceGoalKm) {
        
        // Calculate optimal number of treasures based on distance goal
        int treasureCount = calculateTreasureDensity(distanceGoalKm);
        
        // Generate treasures in a ring pattern around the starting point
        List<TreasureLocation> treasures = new ArrayList<>();
        
        // Calculate ring radius based on distance goal (roughly half the target distance)
        double ringRadiusMeters = Math.min((distanceGoalKm * 1000.0) / 3.0, MAX_TREASURE_DISTANCE_FROM_START_METERS);
        
        // Generate treasures in multiple rings for better distribution
        treasures.addAll(generateRingTreasures(sessionId, startLatitude, startLongitude, 
                                             ringRadiusMeters * 0.6, treasureCount / 3, TreasureType.COMMON));
        treasures.addAll(generateRingTreasures(sessionId, startLatitude, startLongitude, 
                                             ringRadiusMeters * 0.8, treasureCount / 3, TreasureType.RARE));
        treasures.addAll(generateRingTreasures(sessionId, startLatitude, startLongitude, 
                                             ringRadiusMeters, treasureCount / 3, TreasureType.EPIC));
        
        // Validate treasure accessibility and adjust if needed
        List<TreasureLocation> validatedTreasures = validateTreasureAccessibility(treasures, 
                                                                                 startLatitude, startLongitude);
        
        return validatedTreasures;
    }
    
    /**
     * Generate treasures in a circular ring around a center point
     */
    private List<TreasureLocation> generateRingTreasures(String sessionId, double centerLat, double centerLng,
                                                        double radiusMeters, int count, TreasureType type) {
        List<TreasureLocation> ringTreasures = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // Calculate angle for even distribution around the ring
            double baseAngle = (2.0 * Math.PI * i) / count;
            
            // Add some randomness to avoid perfectly uniform placement
            double angleVariation = (random.nextDouble() - 0.5) * (Math.PI / 6); // ±30 degrees variation
            double angle = baseAngle + angleVariation;
            
            // Add radius variation for more natural placement
            double radiusVariation = radiusMeters * (0.8 + random.nextDouble() * 0.4); // 80%-120% of base radius
            
            // Calculate treasure position
            double[] treasurePosition = calculatePositionFromDistance(centerLat, centerLng, 
                                                                     radiusVariation, angle);
            
            // Create treasure with unique ID
            String treasureId = UUID.randomUUID().toString();
            TreasureLocation treasure = new TreasureLocation(treasureId, sessionId, 
                                                           treasurePosition[0], treasurePosition[1], type);
            
            ringTreasures.add(treasure);
        }
        
        return ringTreasures;
    }
    
    /**
     * Calculate treasure density based on target distance goal
     */
    private int calculateTreasureDensity(float distanceGoalKm) {
        // Base calculation: treasures per kilometer
        double calculatedTreasures = distanceGoalKm * BASE_TREASURE_DENSITY;
        
        // Apply min/max constraints
        int treasureCount = (int) Math.round(calculatedTreasures);
        treasureCount = Math.max((int) MIN_TREASURES, treasureCount);
        treasureCount = Math.min((int) MAX_TREASURES, treasureCount);
        
        // Ensure we have at least one treasure of each type for variety
        return Math.max(3, treasureCount);
    }
    
    /**
     * Validate treasure accessibility and ensure reachable locations
     */
    private List<TreasureLocation> validateTreasureAccessibility(List<TreasureLocation> treasures,
                                                               double startLat, double startLng) {
        List<TreasureLocation> validatedTreasures = new ArrayList<>();
        
        for (TreasureLocation treasure : treasures) {
            if (isTreasureAccessible(treasure, startLat, startLng, validatedTreasures)) {
                validatedTreasures.add(treasure);
            } else {
                // Try to relocate inaccessible treasure
                TreasureLocation relocatedTreasure = relocateTreasure(treasure, startLat, startLng, validatedTreasures);
                if (relocatedTreasure != null) {
                    validatedTreasures.add(relocatedTreasure);
                }
            }
        }
        
        return validatedTreasures;
    }
    
    /**
     * Check if a treasure location is accessible (not too close to other treasures, within reasonable distance)
     */
    private boolean isTreasureAccessible(TreasureLocation treasure, double startLat, double startLng,
                                       List<TreasureLocation> existingTreasures) {
        
        // Check distance from starting point
        double distanceFromStart = calculateDistance(startLat, startLng, treasure.latitude, treasure.longitude);
        if (distanceFromStart > MAX_TREASURE_DISTANCE_FROM_START_METERS) {
            return false;
        }
        
        // Check minimum distance from other treasures
        for (TreasureLocation existing : existingTreasures) {
            double distanceBetween = calculateDistance(treasure.latitude, treasure.longitude,
                                                     existing.latitude, existing.longitude);
            if (distanceBetween < MIN_TREASURE_DISTANCE_METERS) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Attempt to relocate an inaccessible treasure to a valid location
     */
    private TreasureLocation relocateTreasure(TreasureLocation originalTreasure, double startLat, double startLng,
                                            List<TreasureLocation> existingTreasures) {
        
        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
            // Generate a new random position within acceptable range
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double distance = MIN_TREASURE_DISTANCE_METERS + 
                            random.nextDouble() * (MAX_TREASURE_DISTANCE_FROM_START_METERS - MIN_TREASURE_DISTANCE_METERS);
            
            double[] newPosition = calculatePositionFromDistance(startLat, startLng, distance, angle);
            
            // Create new treasure at this position
            TreasureLocation newTreasure = new TreasureLocation(originalTreasure.treasureId, 
                                                              originalTreasure.sessionId,
                                                              newPosition[0], newPosition[1], 
                                                              originalTreasure.type);
            
            // Check if this new position is accessible
            if (isTreasureAccessible(newTreasure, startLat, startLng, existingTreasures)) {
                return newTreasure;
            }
        }
        
        // Could not find a valid relocation
        return null;
    }
    
    /**
     * Calculate a geographic position from a starting point, distance, and bearing
     */
    private double[] calculatePositionFromDistance(double startLat, double startLng, 
                                                  double distanceMeters, double bearingRadians) {
        
        double distanceKm = distanceMeters / 1000.0;
        double angularDistance = distanceKm / EARTH_RADIUS_KM;
        
        double startLatRad = Math.toRadians(startLat);
        double startLngRad = Math.toRadians(startLng);
        
        double endLatRad = Math.asin(Math.sin(startLatRad) * Math.cos(angularDistance) +
                                   Math.cos(startLatRad) * Math.sin(angularDistance) * Math.cos(bearingRadians));
        
        double endLngRad = startLngRad + Math.atan2(Math.sin(bearingRadians) * Math.sin(angularDistance) * Math.cos(startLatRad),
                                                   Math.cos(angularDistance) - Math.sin(startLatRad) * Math.sin(endLatRad));
        
        return new double[]{Math.toDegrees(endLatRad), Math.toDegrees(endLngRad)};
    }
    
    /**
     * Calculate distance between two geographic points using Haversine formula
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLngRad = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                  Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                  Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c * 1000; // Return distance in meters
    }
    
    /**
     * Generate treasures using alternative loop/route algorithm (for future enhancement)
     * This method creates treasures along a potential walking route rather than in rings
     */
    public List<TreasureLocation> generateTreasureLoop(String sessionId, double startLatitude, 
                                                      double startLongitude, float distanceGoalKm) {
        // This is a placeholder for future loop-based treasure generation
        // For now, delegate to ring generation
        return generateTreasureRing(sessionId, startLatitude, startLongitude, distanceGoalKm);
    }
    
    /**
     * Get treasure generation statistics for debugging/testing
     */
    public TreasureGenerationStats getGenerationStats(float distanceGoalKm) {
        int treasureCount = calculateTreasureDensity(distanceGoalKm);
        double ringRadius = Math.min((distanceGoalKm * 1000.0) / 3.0, MAX_TREASURE_DISTANCE_FROM_START_METERS);
        
        return new TreasureGenerationStats(treasureCount, ringRadius, distanceGoalKm);
    }
    
    /**
     * Statistics class for treasure generation information
     */
    public static class TreasureGenerationStats {
        public final int totalTreasures;
        public final double ringRadiusMeters;
        public final float distanceGoalKm;
        
        public TreasureGenerationStats(int totalTreasures, double ringRadiusMeters, float distanceGoalKm) {
            this.totalTreasures = totalTreasures;
            this.ringRadiusMeters = ringRadiusMeters;
            this.distanceGoalKm = distanceGoalKm;
        }
    }
}