package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LlmRepositorySessionLogsTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: LlmRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LlmRepository(database.llmDao(), PlainTextCipher)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun logsAreScopedToTheirSession() = runTest {
        val configId = insertConfig()
        val firstSessionId = repository.insertSession(ChatSession(configId = configId, title = "First")).toInt()
        val secondSessionId = repository.insertSession(ChatSession(configId = configId, title = "Second")).toInt()

        repository.insertLog(apiLog(sessionId = firstSessionId, result = "first"))
        repository.insertLog(apiLog(sessionId = secondSessionId, result = "second"))

        val firstLogs = repository.getLogsForSession(firstSessionId).first()
        val secondLogs = repository.getLogsForSession(secondSessionId).first()

        assertEquals(listOf("first"), firstLogs.map { it.resultSnippet })
        assertEquals(listOf("second"), secondLogs.map { it.resultSnippet })
    }

    @Test
    fun clearingSessionLogsLeavesOtherSessionLogs() = runTest {
        val configId = insertConfig()
        val firstSessionId = repository.insertSession(ChatSession(configId = configId, title = "First")).toInt()
        val secondSessionId = repository.insertSession(ChatSession(configId = configId, title = "Second")).toInt()

        repository.insertLog(apiLog(sessionId = firstSessionId, result = "first"))
        repository.insertLog(apiLog(sessionId = secondSessionId, result = "second"))

        repository.clearLogsForSession(firstSessionId)

        assertEquals(emptyList<ApiLog>(), repository.getLogsForSessionOneShot(firstSessionId))
        assertEquals(listOf("second"), repository.getLogsForSessionOneShot(secondSessionId).map { it.resultSnippet })
    }

    @Test
    fun deletingSessionDeletesItsLogs() = runTest {
        val configId = insertConfig()
        val sessionId = repository.insertSession(ChatSession(configId = configId, title = "Chat")).toInt()

        repository.insertLog(apiLog(sessionId = sessionId, result = "deleted"))

        repository.deleteSession(sessionId)

        assertEquals(emptyList<ApiLog>(), repository.getLogsForSessionOneShot(sessionId))
    }

    private suspend fun insertConfig(): Int {
        return repository.insertConfiguration(
            LlmConfiguration(
                name = "example.com - model",
                baseUrl = "https://example.com/v1",
                apiKey = "key",
                modelName = "provider/model",
                apiType = "OPENAI",
                isActive = true
            )
        ).toInt()
    }

    private fun apiLog(sessionId: Int, result: String): ApiLog {
        return ApiLog(
            sessionId = sessionId,
            endpointName = "Route",
            requestUrl = "https://example.com/v1",
            payloadSnippet = "payload",
            resultSnippet = result,
            durationMs = 10,
            responseCode = 200
        )
    }

    private object PlainTextCipher : ApiKeyCipher {
        override fun encrypt(value: String): String = value
        override fun decrypt(value: String): String = value
    }
}
