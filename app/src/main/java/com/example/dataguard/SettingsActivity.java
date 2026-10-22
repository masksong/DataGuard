package com.example.dataguard;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {
    private SwitchCompat swMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.cvFlow).setOnClickListener(v -> startActivity(new Intent(this, FlowSettingsActivity.class)));
        findViewById(R.id.cvReminder).setOnClickListener(v -> startActivity(new Intent(this, ReminderSettingsActivity.class)));

        swMonitor = findViewById(R.id.swMonitor);
        boolean isRunning = getSharedPreferences("dataguard", MODE_PRIVATE).getBoolean("monitor_enabled", false);
        swMonitor.setChecked(isRunning);

        swMonitor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!TrafficStatsHelper.hasUsageAccess(this)) {
                    swMonitor.setChecked(false);
                    Toast.makeText(this, "请先授予使用情况访问权限", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    return;
                }
                Intent i = new Intent(this, MonitorService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
                getSharedPreferences("dataguard", MODE_PRIVATE).edit().putBoolean("monitor_enabled", true).apply();
            } else {
                stopService(new Intent(this, MonitorService.class));
                getSharedPreferences("dataguard", MODE_PRIVATE).edit().putBoolean("monitor_enabled", false).apply();
            }
        });
    }
}