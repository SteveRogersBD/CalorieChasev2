package com.example.caloriechase.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GooglePlacesApiService {
    
    // GET https://maps.googleapis.com/maps/api/place/nearbysearch/json
    @GET("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
    Call<GooglePlacesResponse.Root> nearbySearch(
            @Query("location") String location,  // format: "lat,lng"
            @Query("radius") int radius,          // in meters
            @Query("keyword") String keyword,
            @Query("key") String apiKey
    );
    
    // GET https://maps.googleapis.com/maps/api/directions/json
    @GET("https://maps.googleapis.com/maps/api/directions/json")
    Call<DirectionsResponse.Root> getDirections(
            @Query("origin") String origin,       // format: "lat,lng" or address
            @Query("destination") String destination, // format: "lat,lng" or address
            @Query("mode") String mode,           // "walking", "driving", "bicycling", "transit"
            @Query("key") String apiKey
    );
}
