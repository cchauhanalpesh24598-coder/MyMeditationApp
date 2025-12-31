package com.example.meditationcounter;

import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.media.AudioManager;
import android.database.ContentObserver;

public class BackgroundService extends Service {
    private AudioManager audioManager;
    private ContentObserver observer;

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Volume lock logic jo aapke Bluetooth button ko count mein badalta hai
        observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                // Agar volume change hota hai (BT button se), toh count bhejo aur volume 7 pe rakho
                if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != 7) {
                    Intent intent = new Intent("COUNT_INCREMENTED");
                    sendBroadcast(intent);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 7, 0);
                }
            }
        };
        getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, observer);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel chan = new NotificationChannel("m_svc", "Mantra Counter", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(chan);
            startForeground(1, new Notification.Builder(this, "m_svc")
                            .setContentTitle("Mantra Active")
                            .setContentText("Volume/BT buttons monitoring...")
                            .setSmallIcon(android.R.drawable.ic_dialog_info).build());
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (observer != null) getContentResolver().unregisterContentObserver(observer);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}


