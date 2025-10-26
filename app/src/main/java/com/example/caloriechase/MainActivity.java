package com.example.caloriechase;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupBottomNavigation();
        
        // Check if we should show dashboard (from session summary)
        boolean showDashboard = getIntent().getBooleanExtra("show_dashboard", false);
        boolean refreshDashboard = getIntent().getBooleanExtra("refresh_dashboard", false);
        
        // Load appropriate fragment
        if (savedInstanceState == null) {
            if (showDashboard) {
                DashboardFragment dashboardFragment = new DashboardFragment();
                // Pass refresh flag to fragment
                Bundle args = new Bundle();
                args.putBoolean("refresh_on_create", refreshDashboard);
                dashboardFragment.setArguments(args);
                
                loadFragment(dashboardFragment);
                bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
            } else {
                loadFragment(new HomeFragment());
                bottomNavigationView.setSelectedItemId(R.id.nav_home);
            }
        }
    }
    
    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }
    
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
            }
            
            if (selectedFragment != null) {
                return loadFragment(selectedFragment);
            }
            return false;
        });
    }
    
    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}