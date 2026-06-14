package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LlmDao {

    // --- LLM Configurations ---
    @Query("SELECT * FROM llm_configurations ORDER BY timestamp DESC")
    fun getAllConfigurations(): Flow<List<LlmConfiguration>>

    @Query("SELECT * FROM llm_configurations WHERE isActive = 1 LIMIT 1")
    fun getActiveConfiguration(): Flow<LlmConfiguration?>

    @Query("SELECT * FROM llm_configurations WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveConfigurationOneShot(): LlmConfiguration?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfiguration(config: LlmConfiguration): Long

    @Query("DELETE FROM llm_configurations WHERE id = :id")
    suspend fun deleteConfigurationById(id: Int)

    @Query("DELETE FROM chat_sessions WHERE configId = :configId")
    suspend fun deleteSessionsForConfig(configId: Int)

    @Query("DELETE FROM chat_messages WHERE sessionId IN (SELECT id FROM chat_sessions WHERE configId = :configId)")
    suspend fun deleteMessagesForConfig(configId: Int)

    @Transaction
    suspend fun deleteConfigurationAndMessages(id: Int) {
        deleteConfigurationById(id)
        deleteMessagesForConfig(id)
        deleteSessionsForConfig(id)
    }

    @Query("UPDATE llm_configurations SET isActive = 0")
    suspend fun clearActiveFlag()

    @Query("UPDATE llm_configurations SET isActive = 1 WHERE id = :id")
    suspend fun setActiveFlag(id: Int)

    @Transaction
    suspend fun setActiveConfiguration(id: Int) {
        clearActiveFlag()
        setActiveFlag(id)
    }

    // --- Chat Sessions ---
    @Query("SELECT * FROM chat_sessions WHERE configId = :configId ORDER BY timestamp DESC")
    fun getSessionsForConfig(configId: Int): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Int)

    @Query("UPDATE chat_sessions SET title = :title WHERE id = :id")
    suspend fun updateSessionTitle(id: Int, title: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Int)

    @Transaction
    suspend fun deleteSessionAndMessages(sessionId: Int) {
        deleteSessionById(sessionId)
        deleteMessagesForSession(sessionId)
    }

    // --- Chat Messages ---
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionOneShot(sessionId: Int): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearMessagesForSession(sessionId: Int)

    // --- API Logs ---
    @Query("SELECT * FROM api_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<ApiLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ApiLog)

    @Query("DELETE FROM api_logs")
    suspend fun clearLogs()
}
