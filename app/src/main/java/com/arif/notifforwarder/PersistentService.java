package com.arif.notifforwarder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;

public class PersistentService extends Service {

    public static final String CHANNEL_ID = "nf_ch";
    public static final int    NOTIF_ID   = 1001;

    private final NetworkChangeReceiver netReceiver = new NetworkChangeReceiver();

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIF_ID, buildNotif());
        registerReceiver(netReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        startService(new Intent(getApplicationContext(), PersistentService.class));
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        try { unregisterReceiver(netReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }

    private void createChannel() {
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Notif Forwarder", NotificationManager.IMPORTANCE_MIN);
        ch.setShowBadge(false);
        ch.setSound(null, null);
        ch.enableVibration(false);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .createNotificationChannel(ch);
    }

    private Notification buildNotif() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Notif Forwarder aktif")
                .setContentText("Meneruskan notifikasi ke Telegram")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setPriority(Notification.PRIORITY_MIN)
                .build();
    }
}
