package com.example.sync

import com.example.data.LlmConfiguration
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseRouteDocumentTest {

    @Test
    fun mapsConfigurationToRawFirestoreDocument() {
        val config = LlmConfiguration(
            id = 42,
            name = "OpenRouter - DeepSeek",
            baseUrl = "https://openrouter.ai/api/v1",
            apiKey = "sk-raw-key",
            modelName = "deepseek/deepseek-v4-flash",
            providerId = "openrouter",
            modelOfferingId = "deepseek-v4",
            apiType = "OPENAI",
            maxTokens = 4096,
            temperature = 0.7,
            topP = 0.95,
            stream = true,
            reasoningMode = "show-thinking",
            toolsEnabled = true,
            toolDefinitionsJson = "[]",
            toolChoice = "auto",
            mediaInputType = "image",
            accessTier = "PAID",
            extraBodyJson = "{}",
            extraHeadersJson = "{}",
            timeoutSeconds = 120,
            systemPrompt = "Be direct.",
            timestamp = 1234L
        )

        val map = FirebaseRouteDocument.fromConfig(config).toMap()

        assertEquals("sk-raw-key", map["apiKey"])
        assertEquals("deepseek/deepseek-v4-flash", map["modelName"])
        assertEquals(4096, map["maxTokens"])
        assertEquals(0.7, map["temperature"])
    }

    @Test
    fun restoresConfigurationFromFirestoreDocument() {
        val config = FirebaseRouteDocument.fromMap(
            mapOf(
                "name" to "OpenRouter - DeepSeek",
                "baseUrl" to "https://openrouter.ai/api/v1",
                "apiKey" to "sk-raw-key",
                "modelName" to "deepseek/deepseek-v4-flash",
                "apiType" to "OPENAI",
                "maxTokens" to 4096L,
                "temperature" to 0.7,
                "stream" to true,
                "timestamp" to 1234L
            )
        )?.toConfig()

        requireNotNull(config)
        assertEquals(0, config.id)
        assertEquals(false, config.isActive)
        assertEquals("sk-raw-key", config.apiKey)
        assertEquals("OPENAI", config.apiType)
        assertEquals(4096, config.maxTokens)
        assertEquals(1234L, config.timestamp)
    }
}
