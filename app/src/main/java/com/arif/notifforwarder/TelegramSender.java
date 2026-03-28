package com.arif.notifforwarder;

import android.util.Log;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TelegramSender {

    private static final String TAG = "TelegramSender";

    public static boolean send(String botToken, String chatId, String message) {
        try {
            URL url = new URL("https://api.telegram.org/bot" + botToken + "/sendMessage");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            String params = "chat_id=" + URLEncoder.encode(chatId, "UTF-8")
                    + "&text=" + URLEncoder.encode(message, "UTF-8")
                    + "&parse_mode=Markdown";

            OutputStream os = conn.getOutputStream();
            os.write(params.getBytes("UTF-8"));
            os.close();

            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            Log.e(TAG, "Gagal kirim: " + e.getMessage());
            return false;
        }
    }

    public static String escape(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                   .replace("*", "\\*")
                   .replace("[", "\\[")
                   .replace("`", "\\`");
    }
}
