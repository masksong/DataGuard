package com.example.dataguard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView usage, status;
    private EditText limitInput, warningInput;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 20);
        }
        refresh();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("DataGuard 流量守卫");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title, lp());

        usage = new TextView(this);
        usage.setTextSize(20);
        usage.setPadding(0, dp(18), 0, dp(8));
        root.addView(usage, lp());

        status = new TextView(this);
        status.setTextSize(14);
        root.addView(status, lp());

        limitInput = new EditText(this);
        limitInput.setSingleLine(true);
        limitInput.setInputType(8194);
        limitInput.setHint("每月阈值 GB，例如 30");
        limitInput.setText(String.valueOf(prefs().getFloat("limit_gb", 30f)));
        root.addView(limitInput, lp());

        warningInput = new EditText(this);
        warningInput.setSingleLine(true);
        warningInput.setInputType(8194);
        warningInput.setHint("强提醒触发流量 GB，例如 29.58");
        warningInput.setText(String.format(Locale.US, "%.2f",
                prefs().getFloat("warning_gb", 29.58f)));
        root.addView(warningInput, lp());

        Button save = button("保存阈值和预警比例");
        save.setOnClickListener(v -> {
            try {
                float gb = Float.parseFloat(limitInput.getText().toString().trim());
                float warningGb = Float.parseFloat(warningInput.getText().toString().trim());
                if (gb <= 0 || warningGb < 0 || warningGb >= gb
                        || Float.isNaN(gb) || Float.isInfinite(gb)
                        || Float.isNaN(warningGb) || Float.isInfinite(warningGb))
                    throw new NumberFormatException();
                gb = Math.round(gb * 100f) / 100f;
                warningGb = Math.round(warningGb * 100f) / 100f;
                if (warningGb >= gb) throw new NumberFormatException();
                prefs().edit().putFloat("limit_gb", gb).putFloat("warning_gb", warningGb)
                        .putBoolean("alert80_sent", false).putBoolean("alert100_sent", false)
                        .putBoolean("alert_acknowledged", false).apply();
                Toast.makeText(this, "设置已保存；强提醒流量必须小于月度上限", Toast.LENGTH_SHORT).show();
                refresh();
            } catch (Exception e) {
                Toast.makeText(this, "请输入有效阈值，百分比须为 1–99", Toast.LENGTH_LONG).show();
            }
        });
        root.addView(save, lp());

        Button access = button("授权流量统计权限");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        root.addView(access, lp());

        Button start = button("启动后台监控");
        start.setOnClickListener(v -> {
            if (!TrafficStatsHelper.hasUsageAccess(this)) {
                Toast.makeText(this, "请先授予使用情况访问权限", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                return;
            }
            Intent i = new Intent(this, MonitorService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            Toast.makeText(this, "后台监控已启动，请保留常驻通知", Toast.LENGTH_LONG).show();
        });
        root.addView(start, lp());

        Button stop = button("停止后台监控");
        stop.setOnClickListener(v -> stopService(new Intent(this, MonitorService.class)));
        root.addView(stop, lp());

        Button wireless = button("打开无线网络设置（手动断网）");
        wireless.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)));
        root.addView(wireless, lp());

        Button acknowledge = button("确认提醒并停止震动");
        acknowledge.setOnClickListener(v -> {
            AlertHelper.stopVibration(this);
            Toast.makeText(this, "已停止本机震动；铃声可能需要从通知/音量控制停止", Toast.LENGTH_LONG).show();
        });
        root.addView(acknowledge, lp());

        TextView note = new TextView(this);
        note.setText("\n说明：预警默认 80%，达到阈值 100% 会另行提醒。系统统计按自然月查询，重启后不靠本地累加恢复。普通未 Root APK 不能自动关闭蜂窝数据。");
        note.setTextSize(13);
        root.addView(note, lp());

        setContentView(root);
    }

    private void refresh() {
        float gb = prefs().getFloat("limit_gb", 30f);
        int pct = prefs().getInt("warning_percent", 80);
        long bytes = prefs().getLong("last_bytes", -1);
        if (bytes >= 0) {
            double used = bytes / (1024d * 1024d * 1024d);
            usage.setText(String.format(Locale.getDefault(),
                    "本月已用：%.2f GB\n月度阈值：%.2f GB\n预警点：%.2f GB（%d%%）\n剩余：%.2f GB",
                    used, gb, gb * pct / 100d, pct, Math.max(0, gb - used)));
        } else {
            usage.setText(String.format(Locale.getDefault(),
                    "本月已用：等待首次统计\n月度阈值：%.2f GB\n预警比例：%d%%",
                    gb, pct));
        }
        status.setText(TrafficStatsHelper.hasUsageAccess(this)
                ? "流量统计权限：已授权" : "流量统计权限：未授权");
    }

    private android.content.SharedPreferences prefs() {
        return getSharedPreferences("dataguard", MODE_PRIVATE);
    }
    private Button button(String text) {
        Button b = new Button(this); b.setText(text); return b;
    }
    private LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }
    @Override protected void onResume() { super.onResume(); refresh(); }
}
