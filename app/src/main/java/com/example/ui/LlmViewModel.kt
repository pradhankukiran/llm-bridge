package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.LlmBridgeApplication
import com.example.api.LlmClient
import com.example.api.LlmResponse
import com.example.api.adapter.ModelOffering
import com.example.api.adapter.InferenceDefaults
import com.example.api.adapter.ModelCapability
import com.example.data.ApiLog
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.LlmConfiguration
import com.example.data.ChatSession
import com.example.data.LlmRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class LlmViewModel(
    private val repository: LlmRepository,
    private val llmClient: LlmClient
) : ViewModel() {

    // Loaded configurations
    val configurations: StateFlow<List<LlmConfiguration>>
    
    // Active configuration
    val activeConfig: StateFlow<LlmConfiguration?>

    // Recent execution logs
    val recentLogs: StateFlow<List<ApiLog>>

    // Chat sessions and active session
    val sessions: StateFlow<List<ChatSession>>
    val activeSession: StateFlow<ChatSession?>

    // Current screen context
    // 0 = Overview & Configurations, 1 = Chat, 2 = Logs Details
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Generation state indicator
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingMessage = MutableStateFlow<ChatMessage?>(null)
    val streamingMessage: StateFlow<ChatMessage?> = _streamingMessage.asStateFlow()

    // Current active session ID
    private val _activeSessionId = MutableStateFlow<Int?>(null)
    val activeSessionId: StateFlow<Int?> = _activeSessionId.asStateFlow()

    private var generationJob: Job? = null

    data class DeletedSessionSnapshot(
        val session: ChatSession,
        val messages: List<ChatMessage>
    )

    data class DeletedConfigurationSnapshot(
        val configuration: LlmConfiguration,
        val sessions: List<ChatSession>,
        val messages: List<ChatMessage>
    )

    private val _isWaitingForFirstChunk = MutableStateFlow(false)
    val isWaitingForFirstChunk: StateFlow<Boolean> = _isWaitingForFirstChunk.asStateFlow()

    private val _isSwitchingSession = MutableStateFlow(false)
    val isSwitchingSession: StateFlow<Boolean> = _isSwitchingSession.asStateFlow()

    init {

        configurations = repository.allConfigurations
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        activeConfig = repository.activeConfiguration
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

        recentLogs = repository.recentLogs
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        sessions = activeConfig
            .flatMapLatest { config ->
                if (config != null) {
                    repository.getSessionsForConfig(config.id)
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        activeSession = combine(sessions, _activeSessionId) { sessList, activeId ->
            if (activeId != null) {
                sessList.firstOrNull { it.id == activeId }
            } else {
                sessList.firstOrNull()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        // Seed default config if empty, without destroying existing configurations
        viewModelScope.launch {
            val updatedList = repository.allConfigurations.first()
            if (updatedList.isEmpty()) {
                repository.seedDefaultConfigurationsIfEmpty(updatedList)
            } else {
                val active = updatedList.firstOrNull { it.isActive } ?: updatedList.first()
                if (!active.isActive) {
                    repository.setActiveConfiguration(active.id)
                }
            }
        }

        // Manage active session on configuration changes
        viewModelScope.launch {
            activeConfig.collect { config ->
                if (config != null) {
                    val sessList = repository.getSessionsForConfig(config.id).first()
                    if (sessList.isEmpty()) {
                        val newId = repository.insertSession(ChatSession(configId = config.id, title = "New Chat"))
                        _activeSessionId.value = newId.toInt()
                    } else {
                        if (_activeSessionId.value == null || sessList.none { it.id == _activeSessionId.value }) {
                            _activeSessionId.value = sessList.first().id
                        }
                    }
                } else {
                    _activeSessionId.value = null
                }
            }
        }
    }

    // Reactively observe chat messages belonging ONLY to the selected session
    @OptIn(ExperimentalCoroutinesApi::class)
    val chatHistory: StateFlow<List<ChatMessage>> = activeSession
        .flatMapLatest { session ->
            if (session != null) {
                repository.getMessagesForSession(session.id)
            } else {
                flowOf(emptyList())
            }
        }
        .combine(_streamingMessage) { history, streaming ->
            if (streaming != null && streaming.sessionId == activeSession.value?.id) {
                history + streaming
            } else {
                history
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun changeTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun selectActiveConfiguration(id: Int) {
        viewModelScope.launch {
            repository.setActiveConfiguration(id)
        }
    }

    fun addOrUpdateConfiguration(config: LlmConfiguration) {
        viewModelScope.launch {
            val existing = repository.allConfigurations.first()
            val makeActive = existing.isEmpty() || config.isActive
            val newId = repository.insertConfiguration(config.copy(isActive = makeActive))
            if (makeActive) {
                repository.setActiveConfiguration(newId.toInt())
            }
        }
    }



    fun deleteConfiguration(id: Int, onDeleted: ((DeletedConfigurationSnapshot) -> Unit)? = null) {
        viewModelScope.launch {
            val config = repository.getConfigurationSnapshot(id)
            val sessions = repository.getSessionsForConfigOneShot(id)
            val messages = repository.getMessagesForConfigOneShot(id)
            repository.deleteConfiguration(id)
            if (config != null) {
                onDeleted?.invoke(DeletedConfigurationSnapshot(config, sessions, messages))
            }
        }
    }

    fun restoreDeletedConfiguration(snapshot: DeletedConfigurationSnapshot) {
        viewModelScope.launch {
            repository.restoreConfigurationSnapshot(
                snapshot.configuration,
                snapshot.sessions,
                snapshot.messages
            )
        }
    }

    fun createNewSession() {
        val config = activeConfig.value ?: return
        viewModelScope.launch {
            val newId = repository.insertSession(ChatSession(configId = config.id, title = "New Chat"))
            _activeSessionId.value = newId.toInt()
        }
    }

    fun selectSession(sessionId: Int) {
        if (_activeSessionId.value == sessionId) return
        _activeSessionId.value = sessionId
        viewModelScope.launch {
            _isSwitchingSession.value = true
            delay(250)
            _isSwitchingSession.value = false
        }
    }

    fun deleteSession(sessionId: Int, onDeleted: ((DeletedSessionSnapshot) -> Unit)? = null) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            val messages = repository.getMessagesForSessionOneShot(sessionId)
            repository.deleteSession(sessionId)
            if (session != null) {
                onDeleted?.invoke(DeletedSessionSnapshot(session, messages))
            }
            val config = activeConfig.value
            if (config != null) {
                val sessList = repository.getSessionsForConfig(config.id).first()
                if (sessList.isEmpty()) {
                    val newId = repository.insertSession(ChatSession(configId = config.id, title = "New Chat"))
                    _activeSessionId.value = newId.toInt()
                } else if (_activeSessionId.value == sessionId) {
                    _activeSessionId.value = sessList.first().id
                }
            }
        }
    }

    fun restoreDeletedSession(snapshot: DeletedSessionSnapshot) {
        viewModelScope.launch {
            repository.restoreSessionSnapshot(snapshot.session, snapshot.messages)
            _activeSessionId.value = snapshot.session.id
        }
    }

    fun renameSession(sessionId: Int, title: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, title)
        }
    }

    fun sendChatMessage(
        text: String,
        mediaUris: String = "",
        mediaDisplayName: String = "",
        mediaInputType: String = "auto"
    ) {
        if (text.isBlank() || _isGenerating.value) return

        val config = activeConfig.value ?: return
        val currentSession = activeSession.value
        val requestConfig = if (mediaUris.isNotBlank()) {
            config.copy(
                mediaInputUris = mediaUris,
                mediaInputType = mediaInputType.ifBlank { "auto" }
            )
        } else {
            config
        }

        generationJob = viewModelScope.launch {
            val sessionToUse = if (currentSession == null) {
                val title = if (text.length > 25) text.take(25) + "..." else text
                val newId = repository.insertSession(ChatSession(configId = config.id, title = title))
                val sess = ChatSession(id = newId.toInt(), configId = config.id, title = title)
                _activeSessionId.value = newId.toInt()
                sess
            } else {
                if (currentSession.title == "New Chat") {
                    val title = if (text.length > 25) text.take(25) + "..." else text
                    repository.updateSessionTitle(currentSession.id, title)
                }
                currentSession
            }

            _isGenerating.value = true
            _isWaitingForFirstChunk.value = true
            val assistantResponseContent = java.lang.StringBuilder()

            try {
                // 1. Save user message to DB
                val userMsg = ChatMessage(
                    sessionId = sessionToUse.id,
                    role = "user",
                    content = text,
                    mediaUri = mediaUris,
                    mediaDisplayName = mediaDisplayName,
                    mediaInputType = mediaInputType.ifBlank { "auto" }
                )
                repository.insertMessage(userMsg)

                // 2. Fetch history from DB (guaranteed to include userMsg) to feed the model
                val dbHistory = repository.getMessagesForSessionOneShot(sessionToUse.id)

                val tempAssistantMsg = ChatMessage(
                    sessionId = sessionToUse.id,
                    role = "assistant",
                    content = ""
                )

                if (config.stream) {
                    _streamingMessage.value = tempAssistantMsg
                }

                // 3. Initiate API Call
                val response = llmClient.executeChatCall(requestConfig, dbHistory) { chunk ->
                    if (chunk.isNotEmpty()) {
                        _isWaitingForFirstChunk.value = false
                    }
                    if (config.stream) {
                        assistantResponseContent.append(chunk)
                        _streamingMessage.value = tempAssistantMsg.copy(content = assistantResponseContent.toString())
                    }
                }

                _isWaitingForFirstChunk.value = false
                _streamingMessage.value = null

                // 4. Save results & logs
                when (response) {
                    is LlmResponse.Success -> {
                        val assistantMsg = ChatMessage(
                            sessionId = sessionToUse.id,
                            role = "assistant",
                            content = response.text
                        )
                        repository.insertMessage(assistantMsg)

                        // Store details in Room logs
                        repository.insertLog(
                            ApiLog(
                                endpointName = config.name,
                                requestUrl = config.baseUrl,
                                payloadSnippet = "Model: ${config.modelName}. Prompt: \"$text\"",
                                resultSnippet = response.text.take(200),
                                durationMs = response.durationMs,
                                responseCode = 200
                            )
                        )
                    }
                    is LlmResponse.Error -> {
                        val partial = assistantResponseContent.toString().trim()
                        val errorBody = if (partial.isNotEmpty()) {
                            "$partial\n\n_[stream interrupted: ${response.message}]_"
                        } else {
                            "Failure: ${response.message}"
                        }
                        val errorMsg = ChatMessage(
                            sessionId = sessionToUse.id,
                            role = "assistant",
                            content = errorBody,
                            isError = true
                        )
                        repository.insertMessage(errorMsg)

                        // Store failure logs
                        repository.insertLog(
                            ApiLog(
                                endpointName = config.name,
                                requestUrl = config.baseUrl,
                                payloadSnippet = "Model: ${config.modelName}. Prompt: \"$text\"",
                                resultSnippet = response.rawResponse.take(300),
                                durationMs = response.durationMs,
                                responseCode = response.code
                            )
                        )
                    }
                }
            } catch (e: CancellationException) {
                val partial = assistantResponseContent.toString().trim()
                if (partial.isNotEmpty()) {
                    repository.insertMessage(
                        ChatMessage(
                            sessionId = sessionToUse.id,
                            role = "assistant",
                            content = "$partial\n\n_[generation stopped]_"
                        )
                    )
                }
                throw e
            } finally {
                _isGenerating.value = false
                _isWaitingForFirstChunk.value = false
                _streamingMessage.value = null
            }
        }
        generationJob?.invokeOnCompletion {
            generationJob = null
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
    }

    fun retryLastUserMessage() {
        val session = activeSession.value ?: return
        if (_isGenerating.value) return

        viewModelScope.launch {
            val lastUserMessage = repository.getMessagesForSessionOneShot(session.id)
                .lastOrNull { it.role == "user" }
                ?: return@launch

            sendChatMessage(
                text = lastUserMessage.content,
                mediaUris = lastUserMessage.mediaUri,
                mediaDisplayName = lastUserMessage.mediaDisplayName,
                mediaInputType = lastUserMessage.mediaInputType
            )
        }
    }

    fun clearChatHistory(onCleared: ((List<ChatMessage>) -> Unit)? = null) {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            val messages = repository.getMessagesForSessionOneShot(session.id)
            repository.clearMessagesForSession(session.id)
            if (messages.isNotEmpty()) {
                onCleared?.invoke(messages)
            }
        }
    }

    fun restoreMessages(messages: List<ChatMessage>) {
        viewModelScope.launch {
            repository.restoreMessages(messages)
        }
    }

    fun clearAllLogs(onCleared: ((List<ApiLog>) -> Unit)? = null) {
        viewModelScope.launch {
            val logs = repository.getAllLogsOneShot()
            repository.clearLogs()
            if (logs.isNotEmpty()) {
                onCleared?.invoke(logs)
            }
        }
    }

    fun restoreLogs(logs: List<ApiLog>) {
        viewModelScope.launch {
            repository.restoreLogs(logs)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as LlmBridgeApplication
                return LlmViewModel(
                    application.container.repository,
                    application.container.llmClient
                ) as T
            }
        }
    }
}
