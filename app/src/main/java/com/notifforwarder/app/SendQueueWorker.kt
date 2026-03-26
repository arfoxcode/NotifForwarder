package com.notifforwarder.app

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SendQueueWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db  = AppDatabase.getInstance(applicationContext)
        val dao = db.notificationDao()

        // Hapus dulu yang sudah expired (retry >= 10)
        dao.deleteExpired()

        val queue = dao.getAll()
        if (queue.isEmpty()) return@withContext Result.success()

        var allSuccess = true

        for (item in queue) {
            val success = sendToTelegram(item.botToken, item.chatId, item.message)
            if (success) {
                dao.delete(item)
                Log.d("SendQueueWorker", "Terkirim: id=${item.id}")
            } else {
                dao.incrementRetry(item.id)
                allSuccess = false
                Log.w("SendQueueWorker", "Gagal kirim id=${item.id}, retry=${item.retryCount + 1}")
            }
        }

        return@withContext if (allSuccess) Result.success() else Result.retry()
    }

    private fun sendToTelegram(botToken: String, chatId: String, message: String): Boolean {
        return try {
            val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

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

    companion object {
        const val WORK_NAME = "send_queue_work"

        // Jadwalkan worker: jalan saat ada internet, retry otomatis
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SendQueueWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS  // retry: 30s, 60s, 120s, ...
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP, // jangan duplikat jika sudah terjadwal
                request
            )
        }
    }
}
