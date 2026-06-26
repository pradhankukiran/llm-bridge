package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "api_logs",
    indices = [Index("sessionId")]
)
data class ApiLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int? = null,
    val endpointName: String,
    val requestUrl: String,
    val payloadSnippet: String,
    val resultSnippet: String,
    val durationMs: Long,
    val responseCode: Int, // e.g., 200, 401, 404, 500
    val timestamp: Long = System.currentTimeMillis()
)
