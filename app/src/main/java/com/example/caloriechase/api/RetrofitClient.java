package com.example.caloriechase.api;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static volatile Retrofit retrofit;

    // change to your hosted base url; include trailing slash
    private static final String BASE_URL = "http://10.0.2.2:8080/"; // android emulator to localhost

    public static Retrofit getInstance() {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(logging)
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .writeTimeout(20, TimeUnit.SECONDS)
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(client)
                            .build();
                }
            }
        }
        return retrofit;
    }

    public static ApiService api() {
        return getInstance().create(ApiService.class);
    }
    
    // Separate Retrofit instance for Google Places API (uses full URL)
    private static volatile Retrofit googlePlacesRetrofit;
    
    public static Retrofit getGooglePlacesInstance() {
        if (googlePlacesRetrofit == null) {
            synchronized (RetrofitClient.class) {
                if (googlePlacesRetrofit == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(logging)
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .writeTimeout(20, TimeUnit.SECONDS)
                            .build();

                    googlePlacesRetrofit = new Retrofit.Builder()
                            .baseUrl("https://maps.googleapis.com/") // Base URL for Google APIs
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(client)
                            .build();
                }
            }
        }
        return googlePlacesRetrofit;
    }
    
    public static GooglePlacesApiService googlePlacesApi() {
        return getGooglePlacesInstance().create(GooglePlacesApiService.class);
    }
}
