package com.arif.notifforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import java.util.List;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!isConnected(context)) return;
        Log.d(TAG, "Internet nyala, kirim antrian...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                NotifQueueDbHelper db = NotifQueueDbHelper.getInstance(context);
                db.deleteExpired();
                List<NotifQueueDbHelper.QueueItem> queue = db.getAll();
                for (NotifQueueDbHelper.QueueItem item : queue) {
                    boolean sent = TelegramSender.send(item.botToken, item.chatId, item.message);
                    if (sent) {
                        db.deleteById(item.id);
                        Log.d(TAG, "Terkirim id=" + item.id);
                    } else {
                        db.incrementRetry(item.id);
                    }
                }
            }
        }).start();
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
