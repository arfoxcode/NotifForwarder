package com.notifforwarder.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_queue")
data class QueuedNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val message: String,
    val botToken: String,
    val chatId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
