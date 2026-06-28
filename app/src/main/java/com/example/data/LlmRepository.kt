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

    fun getLogsForSession(sessionId: Int): Flow<List<ApiLog>> {
        return llmDao.getLogsForSession(sessionId)
    }

    suspend fun getActiveConfigurationOneShot(): LlmConfiguration? =
        llmDao.getActiveConfigurationOneShot()?.let { decryptConfig(it) }

    suspend fun getAllConfigurationsOneShot(): List<LlmConfiguration> =
        llmDao.getAllConfigurationsOneShot().map { decryptConfig(it) }

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

    suspend fun getLogsForConfigOneShot(configId: Int): List<ApiLog> {
        return llmDao.getLogsForConfigOneShot(configId)
    }

    suspend fun restoreConfigurationSnapshot(
        config: LlmConfiguration,
        sessions: List<ChatSession>,
        messages: List<ChatMessage>,
        logs: List<ApiLog>
    ) {
        llmDao.insertConfiguration(encryptConfig(config))
        if (sessions.isNotEmpty()) {
            llmDao.insertSessions(sessions)
        }
        if (messages.isNotEmpty()) {
            llmDao.insertMessages(messages)
        }
        if (logs.isNotEmpty()) {
            llmDao.insertLogs(logs)
        }
    }

    suspend fun setActiveConfiguration(id: Int) {
        llmDao.setActiveConfiguration(id)
    }

    suspend fun upsertSyncedConfigurations(configs: List<LlmConfiguration>): Int {
        if (configs.isEmpty()) return 0

        val existingConfigs = getAllConfigurationsOneShot()
        val existingByRouteKey = existingConfigs.associateBy { it.routeKey() }
        val shouldActivateFirst = existingConfigs.none { it.isActive }
        var savedCount = 0

        for ((index, remoteConfig) in configs.withIndex()) {
            val existing = existingByRouteKey[remoteConfig.routeKey()]
            val configToSave = if (existing != null) {
                remoteConfig.copy(
                    id = existing.id,
                    isActive = existing.isActive,
                    timestamp = existing.timestamp
                )
            } else {
                remoteConfig.copy(
                    id = 0,
                    isActive = shouldActivateFirst && index == 0
                )
            }
            llmDao.insertConfiguration(encryptConfig(configToSave))
            savedCount += 1
        }

        return savedCount
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

    suspend fun restoreSessionSnapshot(session: ChatSession, messages: List<ChatMessage>, logs: List<ApiLog>) {
        llmDao.insertSession(session)
        if (messages.isNotEmpty()) {
            llmDao.insertMessages(messages)
        }
        if (logs.isNotEmpty()) {
            llmDao.insertLogs(logs)
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

    suspend fun clearLogsForSession(sessionId: Int) {
        llmDao.clearLogsForSession(sessionId)
    }

    suspend fun getAllLogsOneShot(): List<ApiLog> {
        return llmDao.getAllLogsOneShot()
    }

    suspend fun getLogsForSessionOneShot(sessionId: Int): List<ApiLog> {
        return llmDao.getLogsForSessionOneShot(sessionId)
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

    private fun LlmConfiguration.routeKey(): String {
        return "${apiType.trim().uppercase()}|${baseUrl.trim().trimEnd('/')}|${modelName.trim()}"
    }

}
