package com.notifforwarder.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class NotificationForwarderService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val ignoredPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.notifforwarder.app"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE)

        if (!prefs.getBoolean("enabled", true)) return

        val botToken = prefs.getString("bot_token", "") ?: return
        val chatId   = prefs.getString("chat_id", "")   ?: return
        val prefix   = prefs.getString("prefix", "📱 Notifikasi dari HP:") ?: ""

        if (botToken.isEmpty() || chatId.isEmpty()) return
        if (sbn.packageName in ignoredPackages) return
        if (sbn.isOngoing) return

        // ── CEK FILTER APP ────────────────────────────────────────────────
        val filterEnabled = prefs.getBoolean("filter_enabled", false)
        if (filterEnabled) {
            val allowed = prefs.getStringSet("allowed_packages", emptySet()) ?: emptySet()
            if (allowed.isEmpty() || sbn.packageName !in allowed) return
        }
        // ──────────────────────────────────────────────────────────────────

        val extras  = sbn.notification.extras
        val title   = extras.getString("android.title") ?: ""
        val text    = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val body    = if (bigText.isNotEmpty()) bigText else text

        if (title.isEmpty() && body.isEmpty()) return

        val appName = try {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) { sbn.packageName }

        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val message = buildString {
            appendLine(prefix)
            appendLine()
            appendLine("📦 *App:* ${escapeMarkdown(appName)}")
            if (title.isNotEmpty()) appendLine("📌 *Judul:* ${escapeMarkdown(title)}")
            if (body.isNotEmpty())  appendLine("💬 *Pesan:* ${escapeMarkdown(body)}")
            appendLine("🕐 *Waktu:* $time")
        }

        serviceScope.launch {
            val db  = AppDatabase.getInstance(applicationContext)
            val dao = db.notificationDao()

            // 1. Simpan ke antrian lokal DULU
            val queued = QueuedNotification(
                message  = message,
                botToken = botToken,
                chatId   = chatId
            )
            dao.insert(queued)

            // 2. Coba kirim langsung
            val sent = sendToTelegram(botToken, chatId, message)

            if (sent) {
                // Berhasil → hapus dari antrian
                val justSent = dao.getAll().lastOrNull {
                    it.message == message && it.botToken == botToken
                }
                if (justSent != null) dao.delete(justSent)
                Log.d("NotifForwarder", "Terkirim langsung: $appName")
            } else {
                // Gagal (tidak ada internet) → jadwalkan WorkManager
                Log.w("NotifForwarder", "Tidak ada internet, masuk antrian: $appName")
                SendQueueWorker.schedule(applicationContext)
            }
        }
    }

    private fun sendToTelegram(botToken: String, chatId: String, message: String): Boolean {
        return try {
            val url  = URL("https://api.telegram.org/bot$botToken/sendMessage")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput       = true
            conn.connectTimeout = 8000
            conn.readTimeout    = 8000

            val params = "chat_id=${URLEncoder.encode(chatId, "UTF-8")}" +
                    "&text=${URLEncoder.encode(message, "UTF-8")}" +
                    "&parse_mode=Markdown"

            conn.outputStream.use { it.write(params.toByteArray()) }
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (e: Exception) {
            false
        }
    }

    private fun escapeMarkdown(text: String): String {
        return text.replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("`", "\\`")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
