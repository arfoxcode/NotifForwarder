package com.arif.notifforwarder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class NotifQueueDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "notif_queue.db";
    private static final int    DB_VERSION = 1;
    private static final String TABLE      = "queue";

    private static volatile NotifQueueDbHelper instance;

    public static NotifQueueDbHelper getInstance(Context ctx) {
        if (instance == null) {
            synchronized (NotifQueueDbHelper.class) {
                if (instance == null) {
                    instance = new NotifQueueDbHelper(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private NotifQueueDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "message TEXT,"
                + "bot_token TEXT,"
                + "chat_id TEXT,"
                + "timestamp INTEGER,"
                + "retry_count INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    // ── Model ─────────────────────────────────────────────────────────────────

    public static class QueueItem {
        public long   id;
        public String message;
        public String botToken;
        public String chatId;
        public int    retryCount;
    }

    // ── Operasi ───────────────────────────────────────────────────────────────

    public synchronized void insert(String message, String botToken, String chatId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("message",     message);
        cv.put("bot_token",   botToken);
        cv.put("chat_id",     chatId);
        cv.put("timestamp",   System.currentTimeMillis());
        cv.put("retry_count", 0);
        db.insert(TABLE, null, cv);
    }

    public synchronized List<QueueItem> getAll() {
        List<QueueItem> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, null, null, null, null, null, "timestamp ASC");
        while (c.moveToNext()) {
            QueueItem item  = new QueueItem();
            item.id         = c.getLong(c.getColumnIndexOrThrow("id"));
            item.message    = c.getString(c.getColumnIndexOrThrow("message"));
            item.botToken   = c.getString(c.getColumnIndexOrThrow("bot_token"));
            item.chatId     = c.getString(c.getColumnIndexOrThrow("chat_id"));
            item.retryCount = c.getInt(c.getColumnIndexOrThrow("retry_count"));
            result.add(item);
        }
        c.close();
        return result;
    }

    public synchronized void deleteById(long id) {
        getWritableDatabase().delete(TABLE, "id=?", new String[]{String.valueOf(id)});
    }

    public synchronized void incrementRetry(long id) {
        getWritableDatabase().execSQL(
                "UPDATE " + TABLE + " SET retry_count = retry_count + 1 WHERE id = " + id);
    }

    public synchronized void deleteExpired() {
        getWritableDatabase().delete(TABLE, "retry_count >= 10", null);
    }
}
