package com.notifforwarder.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class PersistentService : Service() {

    companion object {
        const val CHANNEL_ID = "notif_forwarder_channel"
        const val NOTIF_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY = sistem akan restart service ini jika dibunuh
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Restart diri sendiri jika app di-swipe dari recent
        val restartIntent = Intent(applicationContext, PersistentService::class.java)
        startService(restartIntent)
        super.onTaskRemoved(rootIntent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Notif Forwarder",
            // IMPORTANCE_MIN = tidak ada suara, tidak ada popup, tidak tampil di status bar
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Layanan penerusan notifikasi"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Notif Forwarder aktif")
            .setContentText("Meneruskan notifikasi ke Telegram")
            // Ikon kecil tidak mencolok (pakai ikon sistem)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(Notification.PRIORITY_MIN)
            .setSilent(true)
            .build()
    }
}
