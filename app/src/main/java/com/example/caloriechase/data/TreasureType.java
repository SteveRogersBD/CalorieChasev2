package com.example.caloriechase.data;

/**
 * Enum representing different types of treasures with their properties
 */
public enum TreasureType {
    COMMON(50, "#4ECDC4"),    // 50m radius, teal color
    RARE(75, "#45B7D1"),      // 75m radius, blue color
    EPIC(100, "#FF6B35");     // 100m radius, orange color

    private final int radiusMeters;
    private final String colorHex;

    TreasureType(int radiusMeters, String colorHex) {
        this.radiusMeters = radiusMeters;
        this.colorHex = colorHex;
    }

    public int getRadiusMeters() {
        return radiusMeters;
    }

    public String getColorHex() {
        return colorHex;
    }
}