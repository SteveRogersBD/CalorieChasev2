# Treasure Collection System

This package implements the geofencing and treasure collection system for the CalorieChase treasure hunt feature.

## Components

### GeofenceManager
- Manages Android geofences for treasure locations
- Sets up circular geofences around treasure positions
- Handles geofence registration and cleanup
- Provides callbacks for success/failure

### GeofenceReceiver
- BroadcastReceiver that handles geofence transition events
- Automatically triggered when user enters a treasure geofence
- Marks treasures as collected in the database
- Sends broadcasts for UI updates and animations

### TreasureCollectionManager
- Handles treasure collection animations and user feedback
- Provides fallback proximity checking when geofences fail
- Manages treasure collection state persistence
- Calculates distances and handles manual treasure collection

### TreasureHuntLocationManager
- High-level coordinator for the entire treasure collection system
- Provides simple interface for activities to use
- Handles both geofencing and fallback proximity checking
- Manages cleanup and resource management

## Usage

### Starting Treasure Hunt
```java
TreasureHuntLocationManager manager = new TreasureHuntLocationManager(context);
List<TreasureLocation> treasures = getTreasuresForSession(sessionId);

manager.startTreasureHunt(treasures, sessionId, new TreasureHuntLocationManager.TreasureHuntCallback() {
    @Override
    public void onSuccess(String message) {
        // Treasure hunt started successfully
    }
    
    @Override
    public void onFailure(String error) {
        // Handle error
    }
});
```

### Location Updates (for proximity checking)
```java
// Call this from your location update callback
manager.updateLocation(location, sessionId);
```

### Stopping Treasure Hunt
```java
manager.stopTreasureHunt(sessionId, callback);
manager.cleanup();
```

## Integration with TrackingService

The treasure collection system is automatically integrated with the TrackingService:

1. When tracking starts, treasures are loaded and geofences are set up
2. Location updates trigger proximity checking as fallback
3. When tracking stops, geofences are cleaned up

## Broadcast Actions

The system sends several broadcast actions for UI updates:

- `TreasureCollectionManager.ACTION_TREASURE_COLLECTED` - When a treasure is collected
- `TreasureCollectionManager.ACTION_TREASURE_ANIMATION` - For treasure animations
- `TreasureCollectionManager.ACTION_SESSION_PROGRESS_UPDATED` - When session progress changes

## Error Handling

The system includes comprehensive error handling:

- **Geofence failures**: Automatically falls back to proximity checking
- **Permission issues**: Provides clear error messages
- **Database errors**: Handles gracefully with logging
- **Service recovery**: Supports app kill/restart scenarios

## Requirements Satisfied

This implementation satisfies the following requirements:

- **3.5**: Geofence creation and treasure collection triggers
- **6.5**: Real-time treasure collection during active sessions  
- **9.2**: Fallback proximity checking when geofences fail

## Testing

To test the treasure collection system:

1. Create a session with treasures using TreasureGenerator
2. Start tracking with TrackingService
3. Move to treasure locations to trigger collection
4. Verify treasures are marked as collected in database
5. Check that UI receives broadcast updates