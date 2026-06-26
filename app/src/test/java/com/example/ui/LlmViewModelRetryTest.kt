package com.example.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.api.LlmClient
import com.example.api.adapter.AdapterChatRequest
import com.example.api.adapter.AdapterChatResult
import com.example.api.adapter.LlmAdapter
import com.example.data.ApiKeyCipher
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.ChatSession
import com.example.data.LlmConfiguration
import com.example.data.LlmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LlmViewModelRetryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun retryLastUserMessageUsesCurrentRouteAndSavedMedia() = runTest {
        val configId = repository.insertConfiguration(
            LlmConfiguration(
                name = "example.com - provider/model",
                baseUrl = "https://example.com/v1",
                apiKey = "key",
                modelName = "provider/model",
                apiType = "OPENAI",
                isActive = true
            )
        ).toInt()
        val sessionId = repository.insertSession(ChatSession(configId = configId, title = "Chat")).toInt()
        repository.insertMessage(
            ChatMessage(
                sessionId = sessionId,
                role = "user",
                content = "Explain this image",
                mediaUri = "content://image/1",
                mediaDisplayName = "diagram.png",
                mediaInputType = "image"
            )
        )
        repository.insertMessage(
            ChatMessage(
                sessionId = sessionId,
                role = "assistant",
                content = "Failure: bad gateway",
                isError = true
            )
        )

        val adapter = RecordingAdapter()
        val viewModel = LlmViewModel(repository, LlmClient(adapter, adapter))

        viewModel.selectSession(sessionId)
        viewModel.activeSession.filterNotNull().first()
        viewModel.retryLastUserMessage()
        withTimeout(5_000) {
            while (adapter.requests.isEmpty()) {
                delay(10)
            }
        }

        val retriedUserMessage = adapter.requests.single().chatHistory.last { it.role == "user" }
        assertEquals("provider/model", adapter.requests.single().config.modelName)
        assertEquals("Explain this image", retriedUserMessage.content)
        assertEquals("content://image/1", retriedUserMessage.mediaUri)
        assertEquals("diagram.png", retriedUserMessage.mediaDisplayName)
        assertEquals("image", retriedUserMessage.mediaInputType)
    }

    private class RecordingAdapter : LlmAdapter {
        val requests = mutableListOf<AdapterChatRequest>()

        override suspend fun chat(
            request: AdapterChatRequest,
            onChunkReceived: (String) -> Unit
        ): AdapterChatResult {
            requests += request
            return AdapterChatResult(
                text = "Retried response",
                rawResponse = "Retried response",
                responseCode = 200
            )
        }
    }

    private object PlainTextCipher : ApiKeyCipher {
        override fun encrypt(value: String): String = value
        override fun decrypt(value: String): String = value
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
