package com.notifforwarder.app

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class SecretCodeReceiver : BroadcastReceiver() {

    companion object {
        // Kode rahasia: ketik *#*#8642#*#* di dial pad lalu tekan panggil
        // (8642 = TNGF = singkatan bebas, bisa diganti)
        const val SECRET_CODE = "8642"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_NEW_OUTGOING_CALL) return

        val number = resultData ?: intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: return

        // Deteksi pola *#*#KODE#*#*
        val cleaned = number.replace("[^0-9*#]".toRegex(), "")
        if (!cleaned.contains("*#*#${SECRET_CODE}#*#*") &&
            !cleaned.contains("*#*#${SECRET_CODE}")) return

        // Batalkan panggilan (jangan sampai benar-benar menelepon)
        resultData = null

        // Tampilkan ikon launcher dulu, lalu buka activity
        val pm = context.packageManager
        val alias = ComponentName(context, "com.notifforwarder.app.MainLauncher")
        pm.setComponentEnabledSetting(
            alias,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Buka MainActivity
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(launch)
    }
}
