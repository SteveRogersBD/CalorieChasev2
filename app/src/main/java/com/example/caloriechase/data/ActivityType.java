package com.example.caloriechase.data;

/**
 * Enum representing different types of physical activities for treasure hunt sessions
 */
public enum ActivityType {
    WALK("Walking"),
    RUN("Running");

    private final String displayName;

    ActivityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}