package com.example.dataguard;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class ReminderSettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_settings);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        SwitchCompat swSound = findViewById(R.id.swSound);
        SwitchCompat swVibrate = findViewById(R.id.swVibrate);
        android.content.SharedPreferences prefs = getSharedPreferences("dataguard", MODE_PRIVATE);

        swSound.setChecked(prefs.getBoolean("enable_ringtone", true));
        swVibrate.setChecked(prefs.getBoolean("enable_vibration", true));

        swSound.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("enable_ringtone", isChecked).apply());
        swVibrate.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("enable_vibration", isChecked).apply());
    }
}