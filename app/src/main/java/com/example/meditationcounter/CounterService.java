package com.example.meditationcounter;

import android.app.*;
import android.content.*;
import android.os.*;

public class CounterService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Notification for Foreground Service
        String channelId = "jap_service_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Jap Counter Service", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        Notification notification = new Notification.Builder(this, channelId)
            .setContentTitle("Jap Counter Active")
            .setContentText("Counting is active in background")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build();

        startForeground(1, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}

