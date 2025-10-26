package com.example.caloriechase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class FinishActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish);
        
        TextView tvSummary = findViewById(R.id.tv_summary);
        Button btnGoToDashboard = findViewById(R.id.btn_go_to_dashboard);
        
        // Display user summary
        displayUserSummary(tvSummary);
        
        btnGoToDashboard.setOnClickListener(v -> {
            // Mark onboarding as completed
            OnboardingActivity.markOnboardingCompleted(this);
            
            // Go to main activity
            Intent intent = new Intent(FinishActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
    
    private void displayUserSummary(TextView tvSummary) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        
        int age = prefs.getInt("age", 0);
        float height = prefs.getFloat("height", 0);
        float weight = prefs.getFloat("weight", 0);
        String heightUnit = prefs.getString("height_unit", "cm");
        String weightUnit = prefs.getString("weight_unit", "kg");
        String sex = prefs.getString("sex", "Other");
        
        String summary = String.format(
            "Age: %d years\nHeight: %.1f %s\nWeight: %.1f %s\nSex: %s",
            age, height, heightUnit, weight, weightUnit, sex
        );
        
        tvSummary.setText(summary);
    }
}