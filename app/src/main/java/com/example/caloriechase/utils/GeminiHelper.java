package com.example.caloriechase.utils;

import android.content.Context;
import android.util.Log;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiHelper {
    
    private static final String TAG = "GeminiHelper";
    private final GenerativeModelFutures model;
    private final Executor executor;
    
    public interface GeminiCallback {
        void onSuccess(String refinedPrompt);
        void onError(String error);
    }
    
    public GeminiHelper(Context context) {
        String apiKey = context.getString(com.example.caloriechase.R.string.gemini_api_key);
        Log.d(TAG, "Initializing Gemini with API key: " + apiKey.substring(0, 10) + "...");
        
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey);
        model = GenerativeModelFutures.from(gm);
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Refines a user prompt to make it suitable for Google Places API keyword search
     */
    public void refinePromptForPlaces(String userPrompt, GeminiCallback callback) {
        Log.d(TAG, "Refining prompt: " + userPrompt);
        
        try {
            String systemPrompt = "You are a helpful assistant that refines user prompts for Google Places API searches. " +
                    "The user will provide a description of places they want to visit. " +
                    "Your job is to extract the most relevant keyword or short phrase (1-3 words) that can be used with Google Places API. " +
                    "Return ONLY the keyword/phrase, nothing else. No explanations, no punctuation, just the search term.\n\n" +
                    "Examples:\n" +
                    "User: 'I want to find some nice coffee shops to visit'\n" +
                    "You: coffee shop\n\n" +
                    "User: 'Looking for parks with playgrounds for kids'\n" +
                    "You: park playground\n\n" +
                    "User: 'historic landmarks and museums'\n" +
                    "You: museum landmark\n\n" +
                    "User: 'places to eat pizza'\n" +
                    "You: pizza restaurant\n\n" +
                    "Now refine this prompt: " + userPrompt;
            
            Content content = new Content.Builder()
                    .addText(systemPrompt)
                    .build();
            
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    try {
                        Log.d(TAG, "Gemini response received");
                        String refinedPrompt = result.getText().trim();
                        Log.d(TAG, "Refined prompt: " + refinedPrompt);
                        callback.onSuccess(refinedPrompt);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Gemini response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
                
                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Gemini API call failed", t);
                    String errorMsg = t.getMessage() != null ? t.getMessage() : "Unknown error";
                    Log.e(TAG, "Error details: " + errorMsg);
                    
                    // Provide more specific error messages
                    if (t.getMessage() != null) {
                        if (t.getMessage().contains("API key")) {
                            errorMsg = "Invalid API key";
                        } else if (t.getMessage().contains("quota")) {
                            errorMsg = "API quota exceeded";
                        } else if (t.getMessage().contains("network")) {
                            errorMsg = "Network error";
                        }
                    }
                    
                    callback.onError(errorMsg);
                }
            }, executor);
        } catch (Exception e) {
            Log.e(TAG, "Exception in refinePromptForPlaces", e);
            callback.onError("Unexpected error: " + e.getMessage());
        }
    }
}
