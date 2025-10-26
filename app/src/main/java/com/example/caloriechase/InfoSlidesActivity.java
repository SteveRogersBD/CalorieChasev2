package com.example.caloriechase;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class InfoSlidesActivity extends AppCompatActivity {
    
    private ViewPager2 viewPager;
    private Button btnNext, btnSkip;
    private TextView tvProgress;
    private InfoSlidesAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_slides);
        
        initViews();
        setupViewPager();
        startAnimations();
    }
    
    private void initViews() {
        viewPager = findViewById(R.id.view_pager);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);
        tvProgress = findViewById(R.id.tv_progress);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        
        adapter = new InfoSlidesAdapter(this);
        viewPager.setAdapter(adapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            // Tab indicator will be handled automatically
        }).attach();
        
        btnNext.setOnClickListener(v -> {
            animateButtonPress(v);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (viewPager.getCurrentItem() < adapter.getItemCount() - 1) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
                } else {
                    goToPersonalInfo();
                }
            }, 100);
        });
        
        btnSkip.setOnClickListener(v -> {
            animateButtonPress(v);
            new Handler(Looper.getMainLooper()).postDelayed(this::goToPersonalInfo, 100);
        });
    }
    
    private void setupViewPager() {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                
                // Update button text with animation
                if (position == adapter.getItemCount() - 1) {
                    animateTextChange(btnNext, "Continue");
                } else {
                    animateTextChange(btnNext, "Next");
                }
                
                // Update progress text
                updateProgress(position + 1, adapter.getItemCount());
            }
        });
        
        // Set initial progress
        updateProgress(1, adapter.getItemCount());
    }
    
    private void startAnimations() {
        // Animate progress text
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        tvProgress.startAnimation(fadeIn);
        
        // Animate ViewPager
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
            viewPager.startAnimation(slideIn);
        }, 200);
        
        // Animate buttons
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
            findViewById(R.id.btn_container).startAnimation(scaleIn);
        }, 400);
    }
    
    private void updateProgress(int current, int total) {
        String progressText = "Step " + (current + 1) + " of 5";
        tvProgress.setText(progressText);
        
        // Animate progress update
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvProgress, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvProgress, "scaleY", 1f, 1.1f, 1f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        scaleX.start();
        scaleY.start();
    }
    
    private void animateTextChange(Button button, String newText) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(button, "alpha", 1f, 0f);
        fadeOut.setDuration(100);
        fadeOut.start();
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            button.setText(newText);
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(button, "alpha", 0f, 1f);
            fadeIn.setDuration(100);
            fadeIn.start();
        }, 100);
    }
    
    private void animateButtonPress(View button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f, 1f);
        scaleX.setDuration(150);
        scaleY.setDuration(150);
        scaleX.start();
        scaleY.start();
    }
    
    private void goToPersonalInfo() {
        // Mark onboarding as completed
        OnboardingActivity.markOnboardingCompleted(this);
        
        // Navigate to Register screen instead of directly to PersonalInfo
        Intent intent = new Intent(InfoSlidesActivity.this, RegisterActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }
}