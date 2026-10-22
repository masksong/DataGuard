package com.example.dataguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class MonitorService extends Service {
    public static final String ACTION_STOP = "com.example.dataguard.STOP";
    public static final String ACTION_ACK = "com.example.dataguard.ACK";
    private static final String CHANNEL = "dataguard_monitor";
    private static final int FOREGROUND_ID = 4101;
    private static final long INTERVAL = 15000L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable task = new Runnable() {
        @Override public void run() {
            checkUsage();
            handler.postDelayed(this, INTERVAL);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(FOREGROUND_ID, buildStatus("监控已启动，正在读取系统统计"));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            handler.removeCallbacks(task);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_ACK.equals(intent.getAction())) {
            AlertHelper.stopVibration(this);
            getSharedPreferences("dataguard", MODE_PRIVATE).edit()
                    .putBoolean("alert_acknowledged", true).apply();
            return START_STICKY;
        }
        handler.removeCallbacks(task);
        handler.post(task);
        return START_STICKY;
    }

    private void checkUsage() {
        new Thread(() -> {
            try {
                long bytes = TrafficStatsHelper.getMonthMobileBytes(this);
                String month = TrafficStatsHelper.monthKey();
                android.content.SharedPreferences p =
                        getSharedPreferences("dataguard", MODE_PRIVATE);
                String savedMonth = p.getString("alert_month", "");
                if (!month.equals(savedMonth)) {
                    p.edit().putString("alert_month", month)
                            .putBoolean("alert80_sent", false)
                            .putBoolean("alert100_sent", false)
                            .putBoolean("alert_acknowledged", false).apply();
                }

                float limitGb = p.getFloat("limit_gb", 30f);
                float warningGb = p.getFloat("warning_gb", 29.58f);
                long limit = (long)(limitGb * 1024d * 1024d * 1024d);
                long warningBytes = (long)(warningGb * 1024d * 1024d * 1024d);
                boolean atWarning = bytes >= warningBytes;
                boolean over = bytes >= limit;

                p.edit().putLong("last_bytes", bytes).putString("last_month", month)
                        .putBoolean("over_limit", over).apply();

                boolean sent80 = p.getBoolean("alert80_sent", false);
                boolean sent100 = p.getBoolean("alert100_sent", false);

                if (over && !sent100) {
                    p.edit().putBoolean("alert100_sent", true)
                            .putBoolean("alert_acknowledged", false).apply();
                    AlertHelper.alert(this, "流量已达到 100% 阈值",
                            "本月已用 " + TrafficStatsHelper.formatGB(bytes)
                                    + "，月度上限 " + limitGb + " GB。请立即手动关闭移动数据。",
                            4103);
                } else if (atWarning && !sent80) {
                    p.edit().putBoolean("alert80_sent", true)
                            .putBoolean("alert_acknowledged", false).apply();
                    AlertHelper.alert(this, "流量达到预警比例",
                            "本月已用 " + TrafficStatsHelper.formatGB(bytes)
                                    + "，已达到强提醒值 " + String.format(java.util.Locale.getDefault(), "%.2f GB", warningGb)
                                    + "。请检查流量使用情况。",
                            4102);
                }

                handler.post(() -> {
                    String msg = "本月 " + TrafficStatsHelper.formatGB(bytes)
                            + " / " + String.format(java.util.Locale.getDefault(), "%.2f GB", limitGb)
                            + "（强提醒 " + String.format(java.util.Locale.getDefault(), "%.2f GB", warningGb) + "）";
                    NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) nm.notify(FOREGROUND_ID, buildStatus(msg));
                });
            } catch (Exception e) {
                handler.post(() -> {
                    NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) nm.notify(FOREGROUND_ID, buildStatus("统计失败：" + e.getMessage()));
                });
            }
        }, "DataGuard-Stats").start();
    }

    private Notification buildStatus(String text) {
        Intent open = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 1, open, flags);
        Notification.Builder b = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("DataGuard 流量守卫")
                .setContentText(text).setContentIntent(pi)
                .setOngoing(true).setOnlyAlertOnce(true);
        if (Build.VERSION.SDK_INT >= 26) b.setChannelId(CHANNEL);
        return b.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "后台流量监控", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override public void onTaskRemoved(Intent rootIntent) { super.onTaskRemoved(rootIntent); }
    @Override public void onDestroy() { handler.removeCallbacks(task); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
