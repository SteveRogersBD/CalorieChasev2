package com.example.caloriechase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.Locale;

/**
 * Direct session summary that displays stats without requiring a saved session
 */
public class SessionSummaryDirectActivity extends AppCompatActivity {
    
    // UI Components
    private TextView tvSessionTitle;
    private TextView tvCompletionStatus;
    
    // Stats Cards
    private CardView cardDistance;
    private CardView cardTime;
    private CardView cardSteps;
    private CardView cardCalories;
    private CardView cardVelocity;
    private CardView cardTreasures;
    
    // Stats Values
    private TextView tvDistanceValue;
    private TextView tvTimeValue;
    private TextView tvStepsValue;
    private TextView tvCaloriesValue;
    private TextView tvVelocityValue;
    private TextView tvTreasuresValue;
    
    // Action Buttons
    private Button btnBackToDashboard;
    
    // Session data
    private float distanceTraveled;
    private float distanceGoal;
    private long sessionDuration;
    private int totalSteps;
    private int caloriesBurned;
    private float averageVelocity;
    private int treasuresCollected;
    private int totalTreasures;
    private int finalScore;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_summary_direct);
        
        // Get data from intent
        Intent intent = getIntent();
        distanceTraveled = intent.getFloatExtra("distance_traveled", 0f);
        distanceGoal = intent.getFloatExtra("distance_goal", 0f);
        sessionDuration = intent.getLongExtra("session_duration", 0);
        totalSteps = intent.getIntExtra("total_steps", 0);
        caloriesBurned = intent.getIntExtra("calories_burned", 0);
        averageVelocity = intent.getFloatExtra("average_velocity", 0f);
        treasuresCollected = intent.getIntExtra("treasures_collected", 0);
        totalTreasures = intent.getIntExtra("total_treasures", 0);
        finalScore = intent.getIntExtra("final_score", 0);
        
        initializeViews();
        displayStats();
    }
    
    private void initializeViews() {
        // Header
        tvSessionTitle = findViewById(R.id.tv_session_title);
        tvCompletionStatus = findViewById(R.id.tv_completion_status);
        
        // Stats Cards
        cardDistance = findViewById(R.id.card_distance);
        cardTime = findViewById(R.id.card_time);
        cardSteps = findViewById(R.id.card_steps);
        cardCalories = findViewById(R.id.card_calories);
        cardVelocity = findViewById(R.id.card_velocity);
        cardTreasures = findViewById(R.id.card_treasures);
        
        // Stats Values
        tvDistanceValue = findViewById(R.id.tv_distance_value);
        tvTimeValue = findViewById(R.id.tv_time_value);
        tvStepsValue = findViewById(R.id.tv_steps_value);
        tvCaloriesValue = findViewById(R.id.tv_calories_value);
        tvVelocityValue = findViewById(R.id.tv_velocity_value);
        tvTreasuresValue = findViewById(R.id.tv_treasures_value);
        
        // Action Button
        btnBackToDashboard = findViewById(R.id.btn_back_to_dashboard);
        btnBackToDashboard.setOnClickListener(v -> backToDashboard());
    }
    
    private void displayStats() {
        // Set header
        tvSessionTitle.setText("Session Complete!");
        
        // Determine completion status
        boolean goalAchieved = distanceTraveled >= distanceGoal;
        if (goalAchieved) {
            tvCompletionStatus.setText("🎉 Goal Achieved!");
            tvCompletionStatus.setTextColor(getColor(R.color.success));
        } else {
            float percentComplete = (distanceTraveled / distanceGoal) * 100f;
            tvCompletionStatus.setText(String.format(Locale.getDefault(), 
                "💪 %.0f%% Complete - Great Effort!", percentComplete));
            tvCompletionStatus.setTextColor(getColor(R.color.primary));
        }
        
        // Display distance
        tvDistanceValue.setText(String.format(Locale.getDefault(), 
            "%.2f km / %.1f km", distanceTraveled, distanceGoal));
        
        // Display time
        tvTimeValue.setText(formatDuration(sessionDuration));
        
        // Display steps
        tvStepsValue.setText(String.format(Locale.getDefault(), "%,d steps", totalSteps));
        
        // Display calories
        tvCaloriesValue.setText(String.format(Locale.getDefault(), "%d kcal", caloriesBurned));
        
        // Display average velocity
        tvVelocityValue.setText(String.format(Locale.getDefault(), "%.2f km/h", averageVelocity));
        
        // Display treasures
        tvTreasuresValue.setText(String.format(Locale.getDefault(), 
            "%d / %d treasures", treasuresCollected, totalTreasures));
    }
    
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format(Locale.getDefault(), "%d:%02d", minutes, secs);
        }
    }
    
    private void backToDashboard() {
        // Navigate back to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        backToDashboard();
    }
}
