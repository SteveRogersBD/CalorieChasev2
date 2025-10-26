package com.example.caloriechase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class PersonalInfoActivity extends AppCompatActivity {
    
    private EditText etAge, etHeight, etWeight;
    private Spinner spinnerHeightUnit, spinnerWeightUnit;
    private RadioGroup rgSex;
    private SwitchMaterial switchUnits;
    private Button btnContinue;
    
    private static final String PREFS_NAME = "UserPrefs";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);
        
        initViews();
        setupSpinners();
        setupListeners();
    }
    
    private void initViews() {
        etAge = findViewById(R.id.et_age);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        spinnerHeightUnit = findViewById(R.id.spinner_height_unit);
        spinnerWeightUnit = findViewById(R.id.spinner_weight_unit);
        rgSex = findViewById(R.id.rg_sex);
        switchUnits = findViewById(R.id.switch_units);
        btnContinue = findViewById(R.id.btn_continue);
    }
    
    private void setupSpinners() {
        // Height units
        ArrayAdapter<CharSequence> heightAdapter = ArrayAdapter.createFromResource(this,
                R.array.height_units, android.R.layout.simple_spinner_item);
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHeightUnit.setAdapter(heightAdapter);
        
        // Weight units
        ArrayAdapter<CharSequence> weightAdapter = ArrayAdapter.createFromResource(this,
                R.array.weight_units, android.R.layout.simple_spinner_item);
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWeightUnit.setAdapter(weightAdapter);
    }
    
    private void setupListeners() {
        switchUnits.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Imperial
                spinnerHeightUnit.setSelection(1); // ft-in
                spinnerWeightUnit.setSelection(1); // lb
            } else {
                // Metric
                spinnerHeightUnit.setSelection(0); // cm
                spinnerWeightUnit.setSelection(0); // kg
            }
        });
        
        btnContinue.setOnClickListener(v -> {
            if (validateInput()) {
                saveUserData();
                Intent intent = new Intent(PersonalInfoActivity.this, PermissionsActivity.class);
                startActivity(intent);
            }
        });
    }
    
    private boolean validateInput() {
        if (TextUtils.isEmpty(etAge.getText().toString())) {
            etAge.setError("Age is required");
            return false;
        }
        
        if (TextUtils.isEmpty(etHeight.getText().toString())) {
            etHeight.setError("Height is required");
            return false;
        }
        
        if (TextUtils.isEmpty(etWeight.getText().toString())) {
            etWeight.setError("Weight is required");
            return false;
        }
        
        if (rgSex.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select your sex", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        int age = Integer.parseInt(etAge.getText().toString());
        if (age < 13 || age > 120) {
            etAge.setError("Please enter a valid age (13-120)");
            return false;
        }
        
        return true;
    }
    
    private void saveUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putInt("age", Integer.parseInt(etAge.getText().toString()));
        editor.putFloat("height", Float.parseFloat(etHeight.getText().toString()));
        editor.putFloat("weight", Float.parseFloat(etWeight.getText().toString()));
        editor.putString("height_unit", spinnerHeightUnit.getSelectedItem().toString());
        editor.putString("weight_unit", spinnerWeightUnit.getSelectedItem().toString());
        editor.putBoolean("is_imperial", switchUnits.isChecked());
        
        // Get selected sex
        String sex = "Other";
        int selectedId = rgSex.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_male) sex = "Male";
        else if (selectedId == R.id.rb_female) sex = "Female";
        else if (selectedId == R.id.rb_other) sex = "Other";
        else if (selectedId == R.id.rb_prefer_not_to_say) sex = "Prefer not to say";
        
        editor.putString("sex", sex);
        editor.apply();
    }
}