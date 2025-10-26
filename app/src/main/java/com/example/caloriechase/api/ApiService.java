package com.example.caloriechase.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    // GET /find_places?lat=..&lng=..&keyword=..&distance_km=..
    @GET("find_places")
    Call<AgentResponse.Root> findPlaces(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("keyword") String keyword,
            @Query("distance_km") double distanceKm
    );
}
