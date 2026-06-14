package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val configId: Int, // The configuration this session belongs to
    val title: String, // E.g. "New Chat" or generated from the first message
    val timestamp: Long = System.currentTimeMillis()
)
