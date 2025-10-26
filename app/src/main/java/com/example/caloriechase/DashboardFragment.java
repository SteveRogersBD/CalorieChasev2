package com.example.caloriechase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.caloriechase.data.DailyStats;
import com.example.caloriechase.data.TreasureHuntDatabase;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor stepDetectorSensor;
    
    // UI Components
    private TextView tvSteps, tvDistance, tvCalories, tvDuration, tvPace, tvTreasures;
    private ProgressBar progressSteps;
    
    // Charts
    private BarChart chartSteps;
    private LineChart chartDistance;
    private BarChart chartCalories;
    
    // Active session UI
    private LinearLayout llActiveSessionCard;
    private TextView tvActiveSessionTitle;
    private TextView tvActiveSessionDistance;
    private TextView tvActiveSessionSteps;
    private TextView tvActiveSessionCalories;
    private TextView tvActiveSessionTime;
    
    // Data
    private int dailySteps = 0;
    private int dailyGoal = 10000;
    private float totalDistance = 0.0f;
    private int caloriesBurned = 0;
    private String sessionDuration = "00:00:00";
    private String averagePace = "0:00 /km";
    private int treasuresCollected = 0;
    
    // Active session data
    private int activeSessionSteps = 0;
    private float activeSessionDistance = 0.0f;
    private int activeSessionCalories = 0;
    private long activeSessionDuration = 0;
    private boolean hasActiveSession = false;
    
    // Broadcast receiver for session updates
    private BroadcastReceiver sessionUpdateReceiver;
    
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "DashboardPrefs";
    
    // Database and executor
    private TreasureHuntDatabase database;
    private ExecutorService executorService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        initViews(view);
        initSensors();
        initDatabase();
        loadDailyData();
        
        // Check if we should refresh data (from session completion)
        Bundle args = getArguments();
        if (args != null && args.getBoolean("refresh_on_create", false)) {
            refreshDashboardData();
        }
        
        updateUI();
        loadChartData();
        
        return view;
    }
    
    private void initViews(View view) {
        // Today's Summary Card
        tvSteps = view.findViewById(R.id.tv_steps);
        tvDistance = view.findViewById(R.id.tv_distance);
        tvCalories = view.findViewById(R.id.tv_calories);
        progressSteps = view.findViewById(R.id.progress_steps);
        
        // Extra Cards
        tvDuration = view.findViewById(R.id.tv_duration);
        tvPace = view.findViewById(R.id.tv_pace);
        tvTreasures = view.findViewById(R.id.tv_treasures);
        
        // Active session card
        llActiveSessionCard = view.findViewById(R.id.ll_active_session_card);
        tvActiveSessionTitle = view.findViewById(R.id.tv_active_session_title);
        tvActiveSessionDistance = view.findViewById(R.id.tv_active_session_distance);
        tvActiveSessionSteps = view.findViewById(R.id.tv_active_session_steps);
        tvActiveSessionCalories = view.findViewById(R.id.tv_active_session_calories);
        tvActiveSessionTime = view.findViewById(R.id.tv_active_session_time);
        
        // Set up click listener to open active session
        if (llActiveSessionCard != null) {
            llActiveSessionCard.setOnClickListener(v -> openActiveSession());
        }
        
        // Initialize charts
        chartSteps = view.findViewById(R.id.chart_steps);
        chartDistance = view.findViewById(R.id.chart_distance);
        chartCalories = view.findViewById(R.id.chart_calories);
        
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    private void initSensors() {
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        }
    }
    
    private void loadDailyData() {
        // Load saved data from SharedPreferences
        dailySteps = prefs.getInt("daily_steps", 0);
        totalDistance = prefs.getFloat("total_distance", 0.0f);
        caloriesBurned = prefs.getInt("calories_burned", 0);
        treasuresCollected = prefs.getInt("treasures_collected", 0);
        
        // Load user data for calorie calculation
        SharedPreferences userPrefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        dailyGoal = userPrefs.getInt("daily_goal", 10000);
    }
    
    private void updateUI() {
        // Calculate total daily metrics (including active session)
        int totalDailySteps = dailySteps + activeSessionSteps;
        float totalDailyDistance = totalDistance + activeSessionDistance;
        int totalDailyCalories = caloriesBurned + activeSessionCalories;
        
        // Update steps and progress
        tvSteps.setText(String.valueOf(totalDailySteps));
        progressSteps.setMax(dailyGoal);
        progressSteps.setProgress(totalDailySteps);
        
        // Update distance
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", totalDailyDistance));
        
        // Update calories
        tvCalories.setText(String.valueOf(totalDailyCalories));
        
        // Update extra data
        tvDuration.setText(sessionDuration);
        tvPace.setText(averagePace);
        tvTreasures.setText(String.valueOf(treasuresCollected));
        
        // Update active session card
        updateActiveSessionCard();
    }
    
    /**
     * Update the active session card visibility and data
     */
    private void updateActiveSessionCard() {
        if (llActiveSessionCard == null) return;
        
        if (hasActiveSession) {
            llActiveSessionCard.setVisibility(View.VISIBLE);
            
            if (tvActiveSessionTitle != null) {
                tvActiveSessionTitle.setText("Active Treasure Hunt");
            }
            if (tvActiveSessionDistance != null) {
                tvActiveSessionDistance.setText(String.format(Locale.getDefault(), "%.2f km", activeSessionDistance));
            }
            if (tvActiveSessionSteps != null) {
                tvActiveSessionSteps.setText(String.valueOf(activeSessionSteps));
            }
            if (tvActiveSessionCalories != null) {
                tvActiveSessionCalories.setText(String.valueOf(activeSessionCalories));
            }
            if (tvActiveSessionTime != null) {
                tvActiveSessionTime.setText(FitnessTracker.formatDuration(activeSessionDuration));
            }
        } else {
            llActiveSessionCard.setVisibility(View.GONE);
        }
    }
    
    private int calculateCalories(int steps) {
        // Get user data for more accurate calculation
        SharedPreferences userPrefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        float weight = userPrefs.getFloat("weight", 70.0f); // Default 70kg
        boolean isImperial = userPrefs.getBoolean("is_imperial", false);
        
        if (isImperial) {
            weight = weight * 0.453592f; // Convert lb to kg
        }
        
        // Simple calorie calculation: ~0.04 calories per step per kg of body weight
        return Math.round(steps * 0.04f * weight);
    }
    
    private void saveDailyData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("daily_steps", dailySteps);
        editor.putFloat("total_distance", totalDistance);
        editor.putInt("calories_burned", caloriesBurned);
        editor.putInt("treasures_collected", treasuresCollected);
        editor.apply();
        
        // Also save to database for charts
        saveTodayStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register sensor listeners
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        }
        if (stepDetectorSensor != null) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
        
        // Register session update receiver
        registerSessionUpdateReceiver();
        
        // Check for active session
        checkForActiveSession();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister sensor listeners to save battery
        sensorManager.unregisterListener(this);
        
        // Unregister session update receiver
        unregisterSessionUpdateReceiver();
        
        saveDailyData();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // Step counter gives total steps since device boot
            // We need to calculate daily steps
            int totalSteps = (int) event.values[0];
            int savedBootSteps = prefs.getInt("boot_steps", 0);
            
            if (savedBootSteps == 0) {
                // First time, save current total as baseline
                prefs.edit().putInt("boot_steps", totalSteps).apply();
                savedBootSteps = totalSteps;
            }
            
            dailySteps = totalSteps - savedBootSteps;
            updateUI();
            
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            // Step detector triggers for each step
            dailySteps++;
            updateUI();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }
    
    /**
     * Register broadcast receiver for session updates
     */
    private void registerSessionUpdateReceiver() {
        if (sessionUpdateReceiver == null) {
            sessionUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    handleSessionUpdate(intent);
                }
            };
        }
        
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                sessionUpdateReceiver, 
                new IntentFilter(TrackingService.BROADCAST_SESSION_UPDATE)
        );
    }
    
    /**
     * Unregister broadcast receiver
     */
    private void unregisterSessionUpdateReceiver() {
        if (sessionUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(sessionUpdateReceiver);
        }
    }
    
    /**
     * Handle session update broadcasts from TrackingService
     */
    private void handleSessionUpdate(Intent intent) {
        if (intent == null) return;
        
        activeSessionSteps = intent.getIntExtra(TrackingService.EXTRA_STEPS, 0);
        activeSessionDistance = intent.getFloatExtra(TrackingService.EXTRA_DISTANCE, 0.0f);
        activeSessionCalories = intent.getIntExtra(TrackingService.EXTRA_CALORIES, 0);
        activeSessionDuration = intent.getLongExtra(TrackingService.EXTRA_DURATION, 0);
        
        // Use FitnessTracker for consistent calorie calculation if needed
        if (activeSessionCalories == 0 && activeSessionSteps > 0) {
            activeSessionCalories = FitnessTracker.calculateCaloriesFromSteps(requireContext(), activeSessionSteps);
        }
        
        hasActiveSession = true;
        updateUI();
    }
    
    /**
     * Check if there's an active session on fragment resume
     */
    private void checkForActiveSession() {
        // Check if tracking service is running
        TrackingServiceManager serviceManager = new TrackingServiceManager(requireContext());
        if (serviceManager.isServiceRunning()) {
            hasActiveSession = true;
        } else {
            hasActiveSession = false;
            // Reset active session data
            activeSessionSteps = 0;
            activeSessionDistance = 0.0f;
            activeSessionCalories = 0;
            activeSessionDuration = 0;
        }
        updateUI();
    }
    
    /**
     * Open the active session activity
     */
    private void openActiveSession() {
        Intent intent = new Intent(requireContext(), ActiveSessionActivity.class);
        startActivity(intent);
    }
    
    /**
     * Update daily progress with completed session data
     * Called when a session is completed
     */
    public void updateDailyProgressFromSession(int sessionSteps, float sessionDistance, int sessionCalories, int sessionTreasures) {
        // Add session data to daily totals
        dailySteps += sessionSteps;
        totalDistance += sessionDistance;
        caloriesBurned += sessionCalories;
        treasuresCollected += sessionTreasures;
        
        // Reset active session data
        activeSessionSteps = 0;
        activeSessionDistance = 0.0f;
        activeSessionCalories = 0;
        activeSessionDuration = 0;
        hasActiveSession = false;
        
        // Save updated daily data
        saveDailyData();
        
        // Update UI
        updateUI();
    }
    
    /**
     * Refresh dashboard data (called when returning from session summary)
     */
    public void refreshDashboardData() {
        loadDailyData();
        checkForActiveSession();
        updateUI();
        loadChartData();
    }
    
    /**
     * Initialize database and executor
     */
    private void initDatabase() {
        database = TreasureHuntDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Load chart data from database
     */
    private void loadChartData() {
        if (database == null) return;
        
        executorService.execute(() -> {
            List<DailyStats> stats = database.dailyStatsDao().getLastNDays(7);
            
            // Reverse to show oldest to newest
            Collections.reverse(stats);
            
            // Update UI on main thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    setupStepsChart(stats);
                    setupDistanceChart(stats);
                    setupCaloriesChart(stats);
                });
            }
        });
    }
    
    /**
     * Setup steps bar chart
     */
    private void setupStepsChart(List<DailyStats> stats) {
        if (chartSteps == null || stats.isEmpty()) return;
        
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        
        for (int i = 0; i < stats.size(); i++) {
            DailyStats stat = stats.get(i);
            entries.add(new BarEntry(i, stat.getSteps()));
            labels.add(formatDateLabel(stat.getDate()));
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Steps");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary_orange));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        
        configureChart(chartSteps, barData, labels);
    }
    
    /**
     * Setup distance line chart
     */
    private void setupDistanceChart(List<DailyStats> stats) {
        if (chartDistance == null || stats.isEmpty()) return;
        
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        
        for (int i = 0; i < stats.size(); i++) {
            DailyStats stat = stats.get(i);
            entries.add(new Entry(i, stat.getDistance()));
            labels.add(formatDateLabel(stat.getDate()));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Distance (km)");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.secondary_teal));
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.secondary_teal));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.secondary_teal));
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        
        LineData lineData = new LineData(dataSet);
        
        configureLineChart(chartDistance, lineData, labels);
    }
    
    /**
     * Setup calories bar chart
     */
    private void setupCaloriesChart(List<DailyStats> stats) {
        if (chartCalories == null || stats.isEmpty()) return;
        
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        
        for (int i = 0; i < stats.size(); i++) {
            DailyStats stat = stats.get(i);
            entries.add(new BarEntry(i, stat.getCalories()));
            labels.add(formatDateLabel(stat.getDate()));
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Calories");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.vibrant_pink));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        
        configureChart(chartCalories, barData, labels);
    }
    
    /**
     * Configure bar chart appearance
     */
    private void configureChart(BarChart chart, BarData data, ArrayList<String> labels) {
        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.getLegend().setEnabled(false);
        
        // X-Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        
        // Y-Axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        
        chart.getAxisRight().setEnabled(false);
        
        chart.animateY(1000);
        chart.invalidate();
    }
    
    /**
     * Configure line chart appearance
     */
    private void configureLineChart(LineChart chart, LineData data, ArrayList<String> labels) {
        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.getLegend().setEnabled(false);
        
        // X-Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        
        // Y-Axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        
        chart.getAxisRight().setEnabled(false);
        
        chart.animateX(1000);
        chart.invalidate();
    }
    
    /**
     * Format date for chart labels (e.g., "Mon" or "12/25")
     */
    private String formatDateLabel(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(dateStr));
        } catch (Exception e) {
            return dateStr.substring(dateStr.length() - 2); // Last 2 chars (day)
        }
    }
    
    /**
     * Save today's stats to database
     */
    public void saveTodayStats() {
        if (database == null) return;
        
        executorService.execute(() -> {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
            
            int totalDailySteps = dailySteps + activeSessionSteps;
            float totalDailyDistance = totalDistance + activeSessionDistance;
            int totalDailyCalories = caloriesBurned + activeSessionCalories;
            
            DailyStats stats = new DailyStats(today, totalDailySteps, totalDailyDistance, totalDailyCalories);
            database.dailyStatsDao().insert(stats);
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}