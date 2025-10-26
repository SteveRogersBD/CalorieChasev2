package com.example.caloriechase;

import android.content.Context;
import android.content.SharedPreferences;

public class UserDataManager {
    
    private static final String PREFS_NAME = "UserPrefs";
    private static final String ONBOARDING_PREFS = "OnboardingPrefs";
    
    public static class UserData {
        public int age;
        public float height;
        public float weight;
        public String heightUnit;
        public String weightUnit;
        public String sex;
        public boolean isImperial;
        
        public UserData(int age, float height, float weight, String heightUnit, 
                       String weightUnit, String sex, boolean isImperial) {
            this.age = age;
            this.height = height;
            this.weight = weight;
            this.heightUnit = heightUnit;
            this.weightUnit = weightUnit;
            this.sex = sex;
            this.isImperial = isImperial;
        }
    }
    
    public static UserData getUserData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        return new UserData(
            prefs.getInt("age", 0),
            prefs.getFloat("height", 0),
            prefs.getFloat("weight", 0),
            prefs.getString("height_unit", "cm"),
            prefs.getString("weight_unit", "kg"),
            prefs.getString("sex", "Other"),
            prefs.getBoolean("is_imperial", false)
        );
    }
    
    public static boolean isOnboardingCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean("onboarding_completed", false);
    }
    
    public static double calculateBMR(UserData userData) {
        // Using Mifflin-St Jeor Equation
        double weight = userData.weight;
        double height = userData.height;
        int age = userData.age;
        
        // Convert to metric if needed
        if (userData.isImperial) {
            weight = weight * 0.453592; // lb to kg
            height = height * 2.54; // inches to cm
        }
        
        double bmr;
        if ("Male".equals(userData.sex)) {
            bmr = 10 * weight + 6.25 * height - 5 * age + 5;
        } else {
            bmr = 10 * weight + 6.25 * height - 5 * age - 161;
        }
        
        return bmr;
    }
}