package com.notifforwarder.app

import androidx.room.*

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(notification: QueuedNotification)

    @Query("SELECT * FROM notification_queue ORDER BY timestamp ASC")
    suspend fun getAll(): List<QueuedNotification>

    @Query("SELECT COUNT(*) FROM notification_queue")
    suspend fun getCount(): Int

    @Delete
    suspend fun delete(notification: QueuedNotification)

    @Query("DELETE FROM notification_queue WHERE id = :id")
    suspend fun deleteById(id: Int)

    // Update retry count jika gagal
    @Query("UPDATE notification_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Int)

    // Hapus yang sudah terlalu banyak retry (maks 10x) agar tidak numpuk selamanya
    @Query("DELETE FROM notification_queue WHERE retryCount >= 10")
    suspend fun deleteExpired()
}
