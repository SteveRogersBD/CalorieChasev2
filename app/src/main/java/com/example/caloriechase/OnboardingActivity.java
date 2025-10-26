package com.example.caloriechase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class OnboardingActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "OnboardingPrefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_EMAIL = "user_email";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check user authentication status
        SharedPreferences userPrefs = getSharedPreferences(USER_PREFS_NAME, MODE_PRIVATE);
        boolean isLoggedIn = userPrefs.getBoolean(KEY_IS_LOGGED_IN, false);
        boolean hasAccount = !userPrefs.getString(KEY_USER_EMAIL, "").isEmpty();
        
        if (isLoggedIn) {
            // User is logged in, go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        
        if (hasAccount) {
            // User has account but not logged in, go to sign in
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
        
        // Check if onboarding was already completed
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)) {
            // Onboarding completed but no account, go to register
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
            return;
        }
        
        // First time user, start with welcome screen
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }
    
    public static void markOnboardingCompleted(AppCompatActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply();
    }
}