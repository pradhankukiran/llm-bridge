package com.example.data

import com.example.api.adapter.ProviderIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LlmRepository(
    private val llmDao: LlmDao,
    private val apiKeyCipher: ApiKeyCipher
) {

    val allConfigurations: Flow<List<LlmConfiguration>> =
        llmDao.getAllConfigurations().map { configs -> configs.map { decryptConfig(it) } }

    val activeConfiguration: Flow<LlmConfiguration?> =
        llmDao.getActiveConfiguration().map { config -> config?.let { decryptConfig(it) } }

    val recentLogs: Flow<List<ApiLog>> = llmDao.getRecentLogs()

    suspend fun getActiveConfigurationOneShot(): LlmConfiguration? =
        llmDao.getActiveConfigurationOneShot()?.let { decryptConfig(it) }

    suspend fun getConfigurationSnapshot(id: Int): LlmConfiguration? =
        llmDao.getAllConfigurationsOneShot().firstOrNull { it.id == id }?.let { decryptConfig(it) }

    suspend fun insertConfiguration(config: LlmConfiguration): Long {
        return llmDao.insertConfiguration(encryptConfig(config))
    }

    suspend fun deleteConfiguration(id: Int) {
        llmDao.deleteConfigurationAndMessages(id)
    }

    suspend fun getSessionsForConfigOneShot(configId: Int): List<ChatSession> {
        return llmDao.getSessionsForConfigOneShot(configId)
    }

    suspend fun getMessagesForConfigOneShot(configId: Int): List<ChatMessage> {
        return llmDao.getMessagesForConfigOneShot(configId)
    }

    suspend fun restoreConfigurationSnapshot(
        config: LlmConfiguration,
        sessions: List<ChatSession>,
        messages: List<ChatMessage>
    ) {
        llmDao.insertConfiguration(encryptConfig(config))
        if (sessions.isNotEmpty()) {
            llmDao.insertSessions(sessions)
        }
        if (messages.isNotEmpty()) {
            llmDao.insertMessages(messages)
        }
    }

    suspend fun setActiveConfiguration(id: Int) {
        llmDao.setActiveConfiguration(id)
    }

    fun getSessionsForConfig(configId: Int): Flow<List<ChatSession>> {
        return llmDao.getSessionsForConfig(configId)
    }

    suspend fun insertSession(session: ChatSession): Long {
        return llmDao.insertSession(session)
    }

    suspend fun getSessionById(sessionId: Int): ChatSession? {
        return llmDao.getSessionById(sessionId)
    }

    suspend fun deleteSession(sessionId: Int) {
        llmDao.deleteSessionAndMessages(sessionId)
    }

    suspend fun updateSessionTitle(sessionId: Int, title: String) {
        llmDao.updateSessionTitle(sessionId, title)
    }

    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessage>> {
        return llmDao.getMessagesForSession(sessionId)
    }

    suspend fun getMessagesForSessionOneShot(sessionId: Int): List<ChatMessage> {
        return llmDao.getMessagesForSessionOneShot(sessionId)
    }

    suspend fun insertMessage(message: ChatMessage): Long {
        return llmDao.insertMessage(message)
    }

    suspend fun restoreSessionSnapshot(session: ChatSession, messages: List<ChatMessage>) {
        llmDao.insertSession(session)
        if (messages.isNotEmpty()) {
            llmDao.insertMessages(messages)
        }
    }

    suspend fun restoreMessages(messages: List<ChatMessage>) {
        if (messages.isNotEmpty()) {
            llmDao.insertMessages(messages)
        }
    }

    suspend fun clearMessagesForSession(sessionId: Int) {
        llmDao.clearMessagesForSession(sessionId)
    }

    suspend fun insertLog(log: ApiLog) {
        llmDao.insertLog(log)
    }

    suspend fun clearLogs() {
        llmDao.clearLogs()
    }

    suspend fun getAllLogsOneShot(): List<ApiLog> {
        return llmDao.getAllLogsOneShot()
    }

    suspend fun restoreLogs(logs: List<ApiLog>) {
        if (logs.isNotEmpty()) {
            llmDao.insertLogs(logs)
        }
    }

    suspend fun seedDefaultConfigurationsIfEmpty(configurationsList: List<LlmConfiguration>) {
        // No pre-seeded default configurations
    }

    private fun encryptConfig(config: LlmConfiguration): LlmConfiguration {
        return config.copy(apiKey = apiKeyCipher.encrypt(config.apiKey))
    }

    private fun decryptConfig(config: LlmConfiguration): LlmConfiguration {
        return config.copy(apiKey = apiKeyCipher.decrypt(config.apiKey))
    }

}
