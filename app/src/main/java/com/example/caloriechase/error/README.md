# Comprehensive Error Handling System

This package implements comprehensive error handling and edge case management for the treasure hunt session feature, addressing all requirements from task 13.

## Components

### 1. ErrorHandler.java
Central error handling utility that provides consistent error messaging and recovery options for all types of errors.

**Features:**
- Permission denial handling with clear messaging and settings navigation
- GPS failure detection with step-based distance fallback
- Geofence failure handling with manual proximity checking
- Network failure handling for Maps and Places API calls
- General session error handling with recovery options

**Error Types Handled:**
- `PermissionType`: LOCATION, ACTIVITY_RECOGNITION, BACKGROUND_LOCATION
- `GpsFailureType`: GPS_DISABLED, GPS_WEAK_SIGNAL, GPS_TIMEOUT, GPS_PERMISSION_DENIED
- `GeofenceFailureType`: GEOFENCE_LIMIT_EXCEEDED, GEOFENCE_SERVICE_UNAVAILABLE, GEOFENCE_PERMISSION_DENIED, GEOFENCE_REGISTRATION_FAILED
- `NetworkFailureType`: MAPS_API_FAILED, PLACES_API_FAILED, GEOCODING_FAILED, NETWORK_TIMEOUT, NO_INTERNET
- `SessionErrorType`: SESSION_CREATION_FAILED, SESSION_LOAD_FAILED, TREASURE_GENERATION_FAILED, DATABASE_ERROR, SENSOR_ERROR

### 2. ServiceRecoveryManager.java
Manages service recovery logic for app kills and system restarts to ensure session continuity.

**Features:**
- Saves service state for recovery purposes
- Attempts service recovery after app restart or system restart
- Handles service crash recovery
- Manages low memory recovery scenarios
- Implements recovery timeout and attempt limits

**Recovery Types:**
- App killed recovery
- System restart recovery
- Service crash recovery
- Low memory recovery

### 3. NetworkErrorHandler.java
Specialized handler for network-related errors in Maps and Places API calls.

**Features:**
- Maps API failure handling with offline fallbacks
- Places API failure handling with manual selection fallbacks
- Geocoding failure handling with coordinate-based addresses
- Network connectivity checking and retry mechanisms
- Exponential backoff retry logic

## Integration Points

### Enhanced Activities

#### SessionSetupActivity
- Uses ErrorHandler for permission request handling
- Provides consistent error messaging for setup failures
- Integrates with network error handling for location services

#### MapSearchActivity
- Implements network error handling for Places API failures
- Provides fallback mechanisms for search and geocoding failures
- Handles offline scenarios gracefully

#### ActiveSessionActivity
- Integrates service recovery on startup
- Handles GPS status updates with error notifications
- Provides session error recovery options
- Implements comprehensive error handling for all session operations

### Enhanced Services

#### TrackingService
- Implements GPS failure detection and fallback to step-based tracking
- Saves service state for recovery purposes
- Handles location permission errors gracefully
- Provides retry mechanisms for GPS failures

#### GeofenceManager
- Implements comprehensive geofence failure handling
- Provides manual proximity checking fallback
- Handles geofence service unavailability
- Includes debugging and status information

### Enhanced Fragments

#### HomeFragment
- Checks for service recovery on startup
- Handles permission errors before session setup
- Provides error handling for session creation failures

## Error Handling Patterns

### 1. Permission Handling
```java
errorHandler.handlePermissionDenied(PermissionType.LOCATION, new PermissionDenialCallback() {
    @Override
    public void onSettingsOpened() {
        // User went to settings
    }
    
    @Override
    public void onContinueWithoutPermission() {
        // Continue with limited functionality
    }
    
    @Override
    public void onCancel() {
        // User cancelled
    }
});
```

### 2. GPS Failure Handling
```java
errorHandler.handleGpsFailure(GpsFailureType.GPS_WEAK_SIGNAL, new GpsFailureCallback() {
    @Override
    public void onRetryGps() {
        // Retry GPS acquisition
    }
    
    @Override
    public void onContinueWithSteps() {
        // Use step-based tracking
    }
});
```

### 3. Service Recovery
```java
recoveryManager.attemptServiceRecovery(new ServiceRecoveryCallback() {
    @Override
    public void onRecoverySuccessful(ActiveSession session, ServiceRecoveryType type) {
        // Service recovered successfully
    }
    
    @Override
    public void onRecoveryFailed(String reason) {
        // Recovery failed
    }
    
    @Override
    public void onRecoveryNotNeeded() {
        // No recovery needed
    }
});
```

### 4. Network Error Handling
```java
networkErrorHandler.handleMapsApiError(exception, new MapsApiCallback() {
    @Override
    public void onRetryMapsApi() {
        // Retry the Maps API call
    }
    
    @Override
    public void onContinueOffline() {
        // Continue with offline functionality
    }
});
```

## Error Recovery Mechanisms

### 1. GPS Failures
- Automatic fallback to step-based distance calculation
- User notification about reduced accuracy
- Retry mechanisms with exponential backoff
- Graceful degradation of functionality

### 2. Geofence Failures
- Automatic fallback to manual proximity checking
- Continued treasure collection functionality
- User notification about detection method change
- Retry mechanisms for geofence registration

### 3. Service Recovery
- Automatic service restart after app kills
- Session state preservation and restoration
- Recovery timeout and attempt limits
- User notification about recovery status

### 4. Network Failures
- Offline mode for Maps functionality
- Manual location selection fallbacks
- Cached data usage when available
- Retry mechanisms with connectivity checking

## Requirements Compliance

This implementation addresses all requirements from task 13:

✅ **9.1**: Permission denial handling with clear messaging and settings navigation
✅ **9.2**: GPS failure detection with step-based distance fallback  
✅ **9.3**: Geofence failure handling with manual proximity checking
✅ **9.4**: Service recovery logic for app kills and system restarts
✅ **9.5**: Network failure handling for Maps and Places API calls

## Usage Guidelines

1. **Initialize error handlers** in activity/fragment onCreate methods
2. **Use consistent error handling patterns** across all components
3. **Provide fallback mechanisms** for all critical functionality
4. **Save state appropriately** for recovery scenarios
5. **Test error scenarios** thoroughly during development

## Testing Scenarios

To test the error handling implementation:

1. **Permission Errors**: Deny permissions and verify graceful handling
2. **GPS Failures**: Test indoors or with GPS disabled
3. **Network Errors**: Test with airplane mode or poor connectivity
4. **Service Recovery**: Kill app during active session and restart
5. **Geofence Failures**: Test on devices with geofence limitations
6. **Low Memory**: Test on devices with limited memory
7. **API Failures**: Test with invalid API keys or service outages

This comprehensive error handling system ensures a robust and reliable user experience even when things go wrong.