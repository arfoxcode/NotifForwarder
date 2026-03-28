package com.arif.notifforwarder;

import android.app.Notification;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificationForwarderService extends NotificationListenerService {

    private static final String TAG = "NotifForwarder";

    private static final Set<String> IGNORED = new HashSet<>(Arrays.asList(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.arif.notifforwarder"
    ));

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        SharedPreferences prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", true)) return;

        final String botToken = prefs.getString("bot_token", "");
        final String chatId   = prefs.getString("chat_id", "");
        final String prefix   = prefs.getString("prefix", "Notifikasi dari HP:");

        if (botToken == null || botToken.isEmpty()) return;
        if (chatId   == null || chatId.isEmpty())   return;
        if (IGNORED.contains(sbn.getPackageName()))  return;
        if (sbn.isOngoing()) return;

        // Filter app
        if (prefs.getBoolean("filter_enabled", false)) {
            Set<String> allowed = prefs.getStringSet("allowed_packages", new HashSet<>());
            if (allowed == null || allowed.isEmpty()) return;
            if (!allowed.contains(sbn.getPackageName())) return;
        }

        Bundle extras  = sbn.getNotification().extras;
        String title   = extras.getString(Notification.EXTRA_TITLE, "");
        CharSequence cs = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (cs == null) cs = extras.getCharSequence(Notification.EXTRA_TEXT);
        final String body = (cs != null) ? cs.toString() : "";

        if (title.isEmpty() && body.isEmpty()) return;

        String appName;
        try {
            appName = getPackageManager()
                    .getApplicationLabel(getPackageManager()
                            .getApplicationInfo(sbn.getPackageName(), 0))
                    .toString();
        } catch (Exception e) {
            appName = sbn.getPackageName();
        }

        final String fTitle   = title;
        final String fApp     = appName;
        final String fToken   = botToken;
        final String fChat    = chatId;
        final String time     = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        final String message = prefix + "\n\n"
                + "*App:* "    + TelegramSender.escape(appName) + "\n"
                + (title.isEmpty() ? "" : "*Judul:* " + TelegramSender.escape(title) + "\n")
                + (body.isEmpty()  ? "" : "*Pesan:* " + TelegramSender.escape(body)  + "\n")
                + "*Waktu:* " + time;

        new Thread(new Runnable() {
            @Override
            public void run() {
                NotifQueueDbHelper db = NotifQueueDbHelper.getInstance(getApplicationContext());
                db.insert(message, fToken, fChat);

                if (NetworkChangeReceiver.isConnected(getApplicationContext())) {
                    List<NotifQueueDbHelper.QueueItem> all = db.getAll();
                    for (int i = all.size() - 1; i >= 0; i--) {
                        NotifQueueDbHelper.QueueItem q = all.get(i);
                        if (q.message.equals(message) && q.botToken.equals(fToken)) {
                            boolean sent = TelegramSender.send(fToken, fChat, message);
                            if (sent) {
                                db.deleteById(q.id);
                                Log.d(TAG, "Terkirim: " + fApp);
                            } else {
                                Log.w(TAG, "Gagal, masuk antrian: " + fApp);
                            }
                            break;
                        }
                    }
                } else {
                    Log.w(TAG, "Offline, masuk antrian: " + fApp);
                }
            }
        }).start();
    }
}
