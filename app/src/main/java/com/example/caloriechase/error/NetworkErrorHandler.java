package com.example.caloriechase.error;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Handles network-related errors for Maps and Places API calls
 * Provides fallback mechanisms and user-friendly error messages
 */
public class NetworkErrorHandler {
    
    private static final String TAG = "NetworkErrorHandler";
    
    private final Context context;
    private final ErrorHandler errorHandler;
    private final ConnectivityManager connectivityManager;
    
    public NetworkErrorHandler(Context context) {
        this.context = context.getApplicationContext();
        this.errorHandler = new ErrorHandler(context);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    /**
     * Handle Maps API failures with appropriate fallbacks
     */
    public void handleMapsApiError(Exception error, MapsApiCallback callback) {
        Log.e(TAG, "Maps API error occurred", error);
        
        ErrorHandler.NetworkFailureType failureType = determineNetworkFailureType(error);
        
        errorHandler.handleNetworkFailure(failureType, new ErrorHandler.NetworkFailureCallback() {
            @Override
            public void onRetryNetwork() {
                if (isNetworkAvailable()) {
                    callback.onRetryMapsApi();
                } else {
                    // Still no network, continue with offline mode
                    callback.onContinueOffline();
                }
            }
            
            @Override
            public void onContinueOffline() {
                callback.onContinueOffline();
            }
        });
    }
    
    /**
     * Handle Places API failures with search fallbacks
     */
    public void handlePlacesApiError(Exception error, String query, PlacesApiCallback callback) {
        Log.e(TAG, "Places API error occurred for query: " + query, error);
        
        ErrorHandler.NetworkFailureType failureType = determineNetworkFailureType(error);
        
        errorHandler.handleNetworkFailure(failureType, new ErrorHandler.NetworkFailureCallback() {
            @Override
            public void onRetryNetwork() {
                if (isNetworkAvailable()) {
                    callback.onRetryPlacesSearch(query);
                } else {
                    // Still no network, use manual map selection
                    callback.onUseManualSelection();
                }
            }
            
            @Override
            public void onContinueOffline() {
                callback.onUseManualSelection();
            }
        });
    }
    
    /**
     * Handle geocoding failures with coordinate fallbacks
     */
    public void handleGeocodingError(Exception error, LatLng location, GeocodingCallback callback) {
        Log.e(TAG, "Geocoding error occurred for location: " + location, error);
        
        ErrorHandler.NetworkFailureType failureType = determineNetworkFailureType(error);
        
        errorHandler.handleNetworkFailure(failureType, new ErrorHandler.NetworkFailureCallback() {
            @Override
            public void onRetryNetwork() {
                if (isNetworkAvailable()) {
                    callback.onRetryGeocoding(location);
                } else {
                    // Use coordinate-based address
                    String fallbackAddress = createFallbackAddress(location);
                    callback.onUseFallbackAddress(fallbackAddress);
                }
            }
            
            @Override
            public void onContinueOffline() {
                String fallbackAddress = createFallbackAddress(location);
                callback.onUseFallbackAddress(fallbackAddress);
            }
        });
    }
    
    /**
     * Handle general network connectivity issues
     */
    public void handleConnectivityError(ConnectivityCallback callback) {
        if (!isNetworkAvailable()) {
            errorHandler.handleNetworkFailure(
                ErrorHandler.NetworkFailureType.NO_INTERNET,
                new ErrorHandler.NetworkFailureCallback() {
                    @Override
                    public void onRetryNetwork() {
                        if (isNetworkAvailable()) {
                            callback.onConnectivityRestored();
                        } else {
                            callback.onContinueOffline();
                        }
                    }
                    
                    @Override
                    public void onContinueOffline() {
                        callback.onContinueOffline();
                    }
                }
            );
        } else {
            // Network is available but there might be other issues
            callback.onConnectivityRestored();
        }
    }
    
    /**
     * Check if network is available
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) {
            return false;
        }
        
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    /**
     * Check if network is metered (mobile data)
     */
    public boolean isNetworkMetered() {
        if (connectivityManager == null) {
            return false;
        }
        
        return connectivityManager.isActiveNetworkMetered();
    }
    
    /**
     * Get network type description
     */
    public String getNetworkTypeDescription() {
        if (connectivityManager == null) {
            return "Unknown";
        }
        
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null) {
            return "No connection";
        }
        
        switch (activeNetworkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return "WiFi";
            case ConnectivityManager.TYPE_MOBILE:
                return "Mobile data";
            case ConnectivityManager.TYPE_ETHERNET:
                return "Ethernet";
            default:
                return "Other";
        }
    }
    
    /**
     * Determine the type of network failure from the exception
     */
    private ErrorHandler.NetworkFailureType determineNetworkFailureType(Exception error) {
        if (error instanceof UnknownHostException) {
            return ErrorHandler.NetworkFailureType.NO_INTERNET;
        } else if (error instanceof SocketTimeoutException) {
            return ErrorHandler.NetworkFailureType.NETWORK_TIMEOUT;
        } else if (error instanceof IOException) {
            return ErrorHandler.NetworkFailureType.NETWORK_TIMEOUT;
        } else if (error instanceof ApiException) {
            ApiException apiException = (ApiException) error;
            switch (apiException.getStatusCode()) {
                case 7: // NETWORK_ERROR
                    return ErrorHandler.NetworkFailureType.NO_INTERNET;
                case 15: // TIMEOUT
                    return ErrorHandler.NetworkFailureType.NETWORK_TIMEOUT;
                case 9011: // REQUEST_DENIED (often due to API key issues)
                    return ErrorHandler.NetworkFailureType.MAPS_API_FAILED;
                default:
                    return ErrorHandler.NetworkFailureType.MAPS_API_FAILED;
            }
        } else if (error.getMessage() != null) {
            String message = error.getMessage().toLowerCase();
            if (message.contains("timeout")) {
                return ErrorHandler.NetworkFailureType.NETWORK_TIMEOUT;
            } else if (message.contains("network") || message.contains("connection")) {
                return ErrorHandler.NetworkFailureType.NO_INTERNET;
            } else if (message.contains("places")) {
                return ErrorHandler.NetworkFailureType.PLACES_API_FAILED;
            } else if (message.contains("maps")) {
                return ErrorHandler.NetworkFailureType.MAPS_API_FAILED;
            } else if (message.contains("geocod")) {
                return ErrorHandler.NetworkFailureType.GEOCODING_FAILED;
            }
        }
        
        // Default to general network timeout
        return ErrorHandler.NetworkFailureType.NETWORK_TIMEOUT;
    }
    
    /**
     * Create a fallback address from coordinates
     */
    private String createFallbackAddress(LatLng location) {
        return String.format("Location (%.4f, %.4f)", location.latitude, location.longitude);
    }
    
    /**
     * Retry network operation with exponential backoff
     */
    public void retryWithBackoff(NetworkOperation operation, int maxRetries, RetryCallback callback) {
        retryWithBackoff(operation, maxRetries, 1000, callback); // Start with 1 second delay
    }
    
    private void retryWithBackoff(NetworkOperation operation, int retriesLeft, long delayMs, RetryCallback callback) {
        if (retriesLeft <= 0) {
            callback.onMaxRetriesExceeded();
            return;
        }
        
        if (!isNetworkAvailable()) {
            callback.onNetworkUnavailable();
            return;
        }
        
        operation.execute(new NetworkOperation.OperationCallback() {
            @Override
            public void onSuccess() {
                callback.onRetrySuccess();
            }
            
            @Override
            public void onFailure(Exception error) {
                // Wait before retrying
                android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.postDelayed(() -> {
                    retryWithBackoff(operation, retriesLeft - 1, delayMs * 2, callback);
                }, delayMs);
            }
        });
    }
    
    /**
     * Interface for network operations that can be retried
     */
    public interface NetworkOperation {
        void execute(OperationCallback callback);
        
        interface OperationCallback {
            void onSuccess();
            void onFailure(Exception error);
        }
    }
    
    /**
     * Callback interfaces for different types of network errors
     */
    public interface MapsApiCallback {
        void onRetryMapsApi();
        void onContinueOffline();
    }
    
    public interface PlacesApiCallback {
        void onRetryPlacesSearch(String query);
        void onUseManualSelection();
    }
    
    public interface GeocodingCallback {
        void onRetryGeocoding(LatLng location);
        void onUseFallbackAddress(String fallbackAddress);
    }
    
    public interface ConnectivityCallback {
        void onConnectivityRestored();
        void onContinueOffline();
    }
    
    public interface RetryCallback {
        void onRetrySuccess();
        void onMaxRetriesExceeded();
        void onNetworkUnavailable();
    }
}