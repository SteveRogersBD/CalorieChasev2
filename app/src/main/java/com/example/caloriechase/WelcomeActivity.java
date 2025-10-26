package com.example.caloriechase;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class WelcomeActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        
        initViews();
        startAnimations();
    }
    
    private void initViews() {
        Button getStartedButton = findViewById(R.id.btn_get_started);
        getStartedButton.setOnClickListener(v -> {
            // Add button press animation
            animateButtonPress(v);
            
            // Navigate with delay for animation
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(WelcomeActivity.this, InfoSlidesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 150);
        });
    }
    
    private void startAnimations() {
        // Animate logo
        CardView logoCard = findViewById(R.id.cv_logo);
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
        logoCard.startAnimation(scaleIn);
        
        // Animate app name with delay
        TextView appName = findViewById(R.id.tv_app_name);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            appName.startAnimation(fadeIn);
            appName.setVisibility(View.VISIBLE);
        }, 200);
        
        // Animate tagline with delay
        TextView tagline = findViewById(R.id.tv_tagline);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            tagline.startAnimation(fadeIn);
            tagline.setVisibility(View.VISIBLE);
        }, 400);
        
        // Animate features with delay
        LinearLayout features = findViewById(R.id.ll_features);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
            features.startAnimation(slideIn);
            features.setVisibility(View.VISIBLE);
        }, 600);
        
        // Animate button with delay
        Button button = findViewById(R.id.btn_get_started);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Animation scaleInButton = AnimationUtils.loadAnimation(this, R.anim.scale_in);
            button.startAnimation(scaleInButton);
            button.setVisibility(View.VISIBLE);
        }, 800);
        
        // Animate background circles
        animateBackgroundElements();
    }
    
    private void animateBackgroundElements() {
        View circle1 = findViewById(R.id.bg_circle_1);
        View circle2 = findViewById(R.id.bg_circle_2);
        
        // Rotate circles continuously
        ObjectAnimator rotate1 = ObjectAnimator.ofFloat(circle1, "rotation", 0f, 360f);
        rotate1.setDuration(20000);
        rotate1.setRepeatCount(ObjectAnimator.INFINITE);
        rotate1.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate1.start();
        
        ObjectAnimator rotate2 = ObjectAnimator.ofFloat(circle2, "rotation", 360f, 0f);
        rotate2.setDuration(15000);
        rotate2.setRepeatCount(ObjectAnimator.INFINITE);
        rotate2.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate2.start();
    }
    
    private void animateButtonPress(View button) {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f, 1f);
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(150);
        animatorSet.start();
    }
}