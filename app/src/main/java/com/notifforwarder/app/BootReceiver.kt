package com.notifforwarder.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start foreground service agar tetap hidup setelah reboot
            val serviceIntent = Intent(context, PersistentService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
