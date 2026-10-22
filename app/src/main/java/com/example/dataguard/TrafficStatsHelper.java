package com.example.dataguard;

import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Process;
import android.telephony.TelephonyManager;
import java.util.Calendar;

public final class TrafficStatsHelper {
    private TrafficStatsHelper() {}

    public static boolean hasUsageAccess(Context c) {
        AppOpsManager a = (AppOpsManager)c.getSystemService(Context.APP_OPS_SERVICE);
        return a != null && a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), c.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    public static long monthStartMillis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static String monthKey() {
        Calendar c = Calendar.getInstance();
        return String.format(java.util.Locale.US, "%04d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }

    public static long getMonthMobileBytes(Context c) throws Exception {
        if (!hasUsageAccess(c)) throw new SecurityException("尚未授予使用情况访问权限");
        TelephonyManager tm = (TelephonyManager)c.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) throw new IllegalStateException("无法访问电话服务");
        String subscriberId;
        try {
            subscriberId = tm.getSubscriberId();
        } catch (SecurityException e) {
            throw new SecurityException("系统不允许读取 SIM 标识；请检查权限或设备兼容性");
        }
        if (subscriberId == null || subscriberId.isEmpty()) {
            throw new IllegalStateException("无法读取 SIM 标识；双卡设备可能需要额外适配");
        }
        NetworkStatsManager nsm = (NetworkStatsManager)c.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats.Bucket b = nsm.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE, subscriberId,
                monthStartMillis(), System.currentTimeMillis());
        if (b == null) throw new IllegalStateException("系统未返回统计结果");
        return b.getRxBytes() + b.getTxBytes();
    }

    public static String formatGB(long bytes) {
        return String.format(java.util.Locale.getDefault(), "%.2f GB",
                bytes / (1024d * 1024d * 1024d));
    }
}
