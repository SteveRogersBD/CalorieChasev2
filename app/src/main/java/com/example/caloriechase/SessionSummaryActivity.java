package com.example.caloriechase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.caloriechase.data.SessionManager;
import com.example.caloriechase.data.SessionRecord;
import com.example.caloriechase.data.TreasureLocation;

import java.util.List;
import java.util.Locale;

/**
 * SessionSummaryActivity displays session completion results and achievements
 */
public class SessionSummaryActivity extends AppCompatActivity {
    
    public static final String EXTRA_SESSION_ID = "session_id";
    
    // UI Components
    private TextView tvSessionTitle;
    private TextView tvCompletionStatus;
    private ImageView ivCompletionIcon;
    
    // Stats Cards
    private CardView cardDistance;
    private CardView cardTime;
    private CardView cardSteps;
    private CardView cardCalories;
    private CardView cardTreasures;
    private CardView cardPace;
    
    // Stats Values
    private TextView tvDistanceValue;
    private TextView tvDistanceGoal;
    private ProgressBar pbDistanceProgress;
    
    private TextView tvTimeValue;
    private TextView tvStepsValue;
    private TextView tvCaloriesValue;
    
    private TextView tvTreasuresValue;
    private TextView tvTreasuresTotal;
    private ProgressBar pbTreasureProgress;
    
    private TextView tvPaceValue;
    
    // Action Buttons
    private Button btnViewRoute;
    private Button btnShareResults;
    private Button btnBackToDashboard;
    
    // Data
    private String sessionId;
    private SessionRecord sessionRecord;
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_summary);
        
        // Get session ID from intent
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        if (sessionId == null || sessionId.isEmpty()) {
            Toast.makeText(this, "Invalid session data", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Initialize components
        initializeViews();
        initializeSessionManager();
        
        // Load session data
        loadSessionData();
    }
    
    private void initializeViews() {
        // Header
        tvSessionTitle = findViewById(R.id.tv_session_title);
        tvCompletionStatus = findViewById(R.id.tv_completion_status);
        ivCompletionIcon = findViewById(R.id.iv_completion_icon);
        
        // Stats Cards
        cardDistance = findViewById(R.id.card_distance);
        cardTime = findViewById(R.id.card_time);
        cardSteps = findViewById(R.id.card_steps);
        cardCalories = findViewById(R.id.card_calories);
        cardTreasures = findViewById(R.id.card_treasures);
        cardPace = findViewById(R.id.card_pace);
        
        // Stats Values
        tvDistanceValue = findViewById(R.id.tv_distance_value);
        tvDistanceGoal = findViewById(R.id.tv_distance_goal);
        pbDistanceProgress = findViewById(R.id.pb_distance_progress);
        
        tvTimeValue = findViewById(R.id.tv_time_value);
        tvStepsValue = findViewById(R.id.tv_steps_value);
        tvCaloriesValue = findViewById(R.id.tv_calories_value);
        
        tvTreasuresValue = findViewById(R.id.tv_treasures_value);
        tvTreasuresTotal = findViewById(R.id.tv_treasures_total);
        pbTreasureProgress = findViewById(R.id.pb_treasure_progress);
        
        tvPaceValue = findViewById(R.id.tv_pace_value);
        
        // Action Buttons
        btnViewRoute = findViewById(R.id.btn_view_route);
        btnShareResults = findViewById(R.id.btn_share_results);
        btnBackToDashboard = findViewById(R.id.btn_back_to_dashboard);
        
        // Set button listeners
        btnViewRoute.setOnClickListener(v -> viewRoute());
        btnShareResults.setOnClickListener(v -> shareResults());
        btnBackToDashboard.setOnClickListener(v -> backToDashboard());
    }
    
    private void initializeSessionManager() {
        sessionManager = SessionManager.getInstance(this);
    }
    
    private void loadSessionData() {
        sessionManager.getSessionHistory(100, new SessionManager.SessionCallback<List<SessionRecord>>() {
            @Override
            public void onSuccess(List<SessionRecord> records) {
                // Find the specific session record
                SessionRecord targetRecord = null;
                for (SessionRecord record : records) {
                    if (record.sessionId.equals(sessionId)) {
                        targetRecord = record;
                        break;
                    }
                }
                
                if (targetRecord != null) {
                    sessionRecord = targetRecord;
                    runOnUiThread(() -> displaySessionStats());
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(SessionSummaryActivity.this, 
                                     "Session record not found", Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    Toast.makeText(SessionSummaryActivity.this, 
                                 "Failed to load session: " + error.getMessage(), 
                                 Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }
    
    private void displaySessionStats() {
        if (sessionRecord == null) return;
        
        // Set header information
        tvSessionTitle.setText("Treasure Hunt Complete!");
        
        // Determine completion status
        boolean goalAchieved = sessionRecord.wasGoalAchieved();
        if (goalAchieved) {
            tvCompletionStatus.setText("Goal Achieved! 🎉");
            tvCompletionStatus.setTextColor(getColor(R.color.success));
            ivCompletionIcon.setImageResource(R.drawable.ic_check_circle);
            ivCompletionIcon.setColorFilter(getColor(R.color.success));
        } else {
            tvCompletionStatus.setText("Great Effort! 💪");
            tvCompletionStatus.setTextColor(getColor(R.color.primary));
            ivCompletionIcon.setImageResource(R.drawable.ic_star);
            ivCompletionIcon.setColorFilter(getColor(R.color.primary));
        }
        
        // Display distance stats
        tvDistanceValue.setText(String.format(Locale.getDefault(), "%.2f", sessionRecord.currentDistance));
        tvDistanceGoal.setText(String.format(Locale.getDefault(), "/ %.1f km", sessionRecord.distanceGoal));
        
        float distanceProgress = (sessionRecord.currentDistance / sessionRecord.distanceGoal) * 100.0f;
        pbDistanceProgress.setProgress((int) Math.min(100, distanceProgress));
        
        // Display time
        tvTimeValue.setText(sessionRecord.getFormattedDuration());
        
        // Display steps
        tvStepsValue.setText(String.format(Locale.getDefault(), "%,d", sessionRecord.currentSteps));
        
        // Display calories
        tvCaloriesValue.setText(String.valueOf(sessionRecord.caloriesBurned));
        
        // Display treasures
        int collectedTreasures = sessionRecord.collectedTreasures.size();
        tvTreasuresValue.setText(String.valueOf(collectedTreasures));
        tvTreasuresTotal.setText(String.format(Locale.getDefault(), "/ %d", sessionRecord.totalTreasures));
        
        float treasureProgress = sessionRecord.getTreasureCollectionRate();
        pbTreasureProgress.setProgress((int) treasureProgress);
        
        // Display pace
        tvPaceValue.setText(sessionRecord.getFormattedPace() + " /km");
        
        // Show/hide route button based on available data
        btnViewRoute.setVisibility(sessionRecord.routePoints != null && !sessionRecord.routePoints.isEmpty() 
                                  ? View.VISIBLE : View.GONE);
    }
    
    private void viewRoute() {
        if (sessionRecord == null || sessionRecord.routePoints == null || sessionRecord.routePoints.isEmpty()) {
            Toast.makeText(this, "No route data available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // TODO: Implement route viewing (could open a map activity showing the path taken)
        Toast.makeText(this, "Route viewing coming soon!", Toast.LENGTH_SHORT).show();
    }
    
    private void shareResults() {
        if (sessionRecord == null) return;
        
        // Create share text
        String shareText = createShareText();
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Treasure Hunt Results");
        
        startActivity(Intent.createChooser(shareIntent, "Share your results"));
    }
    
    private String createShareText() {
        StringBuilder sb = new StringBuilder();
        sb.append("🏃‍♂️ Just completed a Treasure Hunt with CalorieChase! 🏃‍♀️\n\n");
        
        sb.append("📊 My Results:\n");
        sb.append(String.format("🎯 Distance: %.2f / %.1f km", 
                sessionRecord.currentDistance, sessionRecord.distanceGoal));
        
        if (sessionRecord.wasGoalAchieved()) {
            sb.append(" ✅ Goal Achieved!\n");
        } else {
            sb.append("\n");
        }
        
        sb.append(String.format("⏱️ Time: %s\n", sessionRecord.getFormattedDuration()));
        sb.append(String.format("👟 Steps: %,d\n", sessionRecord.currentSteps));
        sb.append(String.format("🔥 Calories: %d\n", sessionRecord.caloriesBurned));
        sb.append(String.format("💎 Treasures: %d/%d (%.0f%%)\n", 
                sessionRecord.collectedTreasures.size(), 
                sessionRecord.totalTreasures,
                sessionRecord.getTreasureCollectionRate()));
        sb.append(String.format("⚡ Pace: %s /km\n", sessionRecord.getFormattedPace()));
        
        sb.append("\n#CalorieChase #TreasureHunt #Fitness");
        
        return sb.toString();
    }
    
    private void backToDashboard() {
        // Update daily progress with completed session data
        updateDailyProgress();
        
        // Navigate back to main activity with dashboard fragment
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("show_dashboard", true);
        intent.putExtra("refresh_dashboard", true); // Signal to refresh dashboard data
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Update daily progress with completed session data using FitnessTracker
     */
    private void updateDailyProgress() {
        if (sessionRecord == null) return;
        
        // Update daily progress by adding session data to existing totals
        FitnessTracker.updateDailyProgressFromSession(
            this,
            sessionRecord.currentSteps,
            sessionRecord.currentDistance,
            sessionRecord.caloriesBurned,
            sessionRecord.collectedTreasures.size()
        );
    }
    
    @Override
    public void onBackPressed() {
        backToDashboard();
    }
}