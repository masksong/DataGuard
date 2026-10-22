package com.example.dataguard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView tvUsed, tvRemaining, tvTotal, tvRemainingPercent, tvCycle, tvMonitorStatus;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        initViews();
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 20);
        }
    }

    private void initViews() {
        tvUsed = findViewById(R.id.tvUsedData);
        tvRemaining = findViewById(R.id.tvRemainingData);
        tvTotal = findViewById(R.id.tvTotalData);
        tvRemainingPercent = findViewById(R.id.tvRemainingPercent);
        tvCycle = findViewById(R.id.tvCycle);
        tvMonitorStatus = findViewById(R.id.tvMonitorStatus);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.ivSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.llBackgroundStatus).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void refresh() {
        float gb = prefs().getFloat("limit_gb", 1.0f);
        float warningGb = prefs().getFloat("warning_gb", 0.8f);
        long bytes = prefs().getLong("last_bytes", -1);

        tvTotal.setText(String.format(Locale.getDefault(), "/ 共 %.2f GB", gb));
        
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        tvCycle.setText(String.format(Locale.getDefault(), "%04d-%02d-01 ~ %04d-%02d-%02d", year, month, year, month, maxDay));

        if (bytes >= 0) {
            double usedGb = bytes / (1024d * 1024d * 1024d);
            long remainingBytes = Math.max(0, (long)(gb * 1024d * 1024d * 1024d) - bytes);
            
            if (bytes < 1024L * 1024 * 1024) {
                tvUsed.setText(String.format(Locale.getDefault(), "%.2f MB", bytes / (1024d * 1024d)));
            } else {
                tvUsed.setText(String.format(Locale.getDefault(), "%.2f GB", usedGb));
            }

            if (remainingBytes < 1024L * 1024 * 1024) {
                tvRemaining.setText(String.format(Locale.getDefault(), "%.2f MB", remainingBytes / (1024d * 1024d)));
            } else {
                tvRemaining.setText(String.format(Locale.getDefault(), "%.2f GB", remainingBytes / (1024d * 1024d * 1024d)));
            }

            int progress = (int) ((usedGb / gb) * 100);
            progress = Math.min(100, progress);
            progressBar.setProgress(progress);
            tvRemainingPercent.setText(String.format(Locale.getDefault(), "剩余 %d%%", 100 - progress));

            if (usedGb >= warningGb) {
                tvRemainingPercent.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                tvRemainingPercent.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        } else {
            tvUsed.setText("等待统计...");
            tvRemaining.setText("--");
            progressBar.setProgress(0);
            tvRemainingPercent.setText("--");
        }

        boolean hasAccess = TrafficStatsHelper.hasUsageAccess(this);
        tvMonitorStatus.setText(hasAccess ? "已开启 >" : "未授权 >");
    }

    private android.content.SharedPreferences prefs() {
        return getSharedPreferences("dataguard", MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
}