package com.example.dataguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public final class AlertHelper {
    private AlertHelper() {}

    public static void alert(Context c, String title, String message, int id, boolean playSound, boolean vibrate) {
        NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "dataguard_alerts";
        if (Build.VERSION.SDK_INT >= 26 && nm != null) {
            NotificationChannel ch = new NotificationChannel(
                    channelId, "流量强提醒", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("流量百分比和超限提醒");
            ch.enableVibration(vibrate);
            ch.setVibrationPattern(new long[]{0, 700, 300, 700, 300, 1000});
            if(playSound){
                Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                ch.setSound(sound, new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            }
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(c, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(c, id, open, flags);
        Notification.Builder b = new Notification.Builder(c)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title).setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(pi).setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MAX);
                
        if (vibrate) b.setVibrate(new long[]{0, 700, 300, 700, 300, 1000});
        if (Build.VERSION.SDK_INT >= 26) b.setChannelId(channelId);
        if (nm != null) nm.notify(id, b.build());

        if (playSound) {
            try {
                Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarm == null) alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone ringtone = RingtoneManager.getRingtone(c, alarm);
                if (ringtone != null) {
                    if (Build.VERSION.SDK_INT >= 28) ringtone.setLooping(true);
                    ringtone.play();
                }
            } catch (Exception ignored) {}
        }

        if (vibrate) {
            try {
                Vibrator v = (Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    long[] pattern = {0, 700, 300, 700, 300, 1000};
                    if (Build.VERSION.SDK_INT >= 26)
                        v.vibrate(VibrationEffect.createWaveform(pattern, 0));
                    else
                        v.vibrate(pattern, 0);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void stopVibration(Context c) {
        try {
            Vibrator v = (Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.cancel();
        } catch (Exception ignored) {}
    }
}