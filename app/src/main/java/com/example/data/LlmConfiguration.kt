package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "llm_configurations")
data class LlmConfiguration(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val providerId: String = "",
    val modelOfferingId: String = "",
    val apiType: String, // "OPENAI", "ANTHROPIC"
    val maxTokens: Int = 1024,
    val temperature: Double = 1.0,
    val topP: Double = 1.0,
    val stream: Boolean = true,
    val reasoningMode: String = "",
    val toolsEnabled: Boolean = false,
    val toolDefinitionsJson: String = "",
    val toolChoice: String = "auto",
    val mediaInputUris: String = "",
    val mediaInputType: String = "auto",
    val accessTier: String = "UNKNOWN",
    val extraBodyJson: String = "",
    val extraHeadersJson: String = "",
    val timeoutSeconds: Int = 180,
    val systemPrompt: String = "",
    val isActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
