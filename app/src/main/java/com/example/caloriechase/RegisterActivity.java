package com.example.caloriechase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {
    
    private TextInputEditText etFullname, etUsername, etEmail, etPassword;
    private MaterialButton btnSignUp, btnGoogleSignIn;
    private TextView tvSignIn;
    
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_FULLNAME = "user_fullname";
    private static final String KEY_USER_USERNAME = "user_username";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        etFullname = findViewById(R.id.et_fullname);
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSignUp = findViewById(R.id.btn_sign_up);
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
        tvSignIn = findViewById(R.id.tv_sign_in);
    }
    
    private void setupListeners() {
        btnSignUp.setOnClickListener(v -> handleSignUp());
        
        btnGoogleSignIn.setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign In - Coming Soon", Toast.LENGTH_SHORT).show();
            // TODO: Implement Google Sign In
        });
        
        tvSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void handleSignUp() {
        String fullname = etFullname.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        // // Validate inputs
        // if (!validateInputs(fullname, username, email, password)) {
        //     return;
        // }
        
        // Save user data
        saveUserData(fullname, username, email);
        
        // Navigate to PersonalInfoActivity
        Intent intent = new Intent(RegisterActivity.this, PersonalInfoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private boolean validateInputs(String fullname, String username, String email, String password) {
        if (TextUtils.isEmpty(fullname)) {
            etFullname.setError("Full name is required");
            etFullname.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return false;
        }
        
        if (username.length() < 3) {
            etUsername.setError("Username must be at least 3 characters");
            etUsername.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }
        
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void saveUserData(String fullname, String username, String email) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_FULLNAME, fullname);
        editor.putString(KEY_USER_USERNAME, username);
        editor.apply();
        
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
    }
}
