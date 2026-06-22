package com.example

import android.content.Context
import com.example.api.LlmClient
import com.example.data.AppDatabase
import com.example.data.LlmRepository

interface AppContainer {
    val repository: LlmRepository
    val llmClient: LlmClient
}

class AppContainerImpl(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val repository: LlmRepository by lazy {
        LlmRepository(database.llmDao())
    }

    override val llmClient: LlmClient by lazy {
        LlmClient()
    }
}
