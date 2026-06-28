package com.example.sync

import com.example.data.LlmConfiguration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class SyncUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)

class FirebaseRouteSyncService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser: SyncUser?
        get() = auth.currentUser?.let { user ->
            SyncUser(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName
            )
        }

    suspend fun signInWithGoogleIdToken(idToken: String): SyncUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = checkNotNull(result.user) { "Google sign-in completed without a Firebase user." }
        return SyncUser(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName
        )
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun backupRoutes(userId: String, routes: List<LlmConfiguration>) {
        val routeCollection = firestore.collection("users").document(userId).collection("routes")
        val existing = routeCollection.get().await()
        val batch = firestore.batch()

        for (document in existing.documents) {
            batch.delete(document.reference)
        }

        for ((index, route) in routes.withIndex()) {
            val documentId = route.syncDocumentId(index)
            batch.set(routeCollection.document(documentId), FirebaseRouteDocument.fromConfig(route).toMap())
        }

        batch.commit().await()
    }

    suspend fun restoreRoutes(userId: String): List<LlmConfiguration> {
        val snapshot = firestore.collection("users").document(userId).collection("routes").get().await()
        return snapshot.documents
            .mapNotNull { document -> FirebaseRouteDocument.fromMap(document.data.orEmpty())?.toConfig() }
            .sortedByDescending { it.timestamp }
    }

    private fun LlmConfiguration.syncDocumentId(index: Int): String {
        return "route-${id.takeIf { it > 0 } ?: index}"
    }
}

data class FirebaseRouteDocument(
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val providerId: String,
    val modelOfferingId: String,
    val apiType: String,
    val maxTokens: Int,
    val temperature: Double,
    val topP: Double,
    val stream: Boolean,
    val reasoningMode: String,
    val toolsEnabled: Boolean,
    val toolDefinitionsJson: String,
    val toolChoice: String,
    val mediaInputType: String,
    val accessTier: String,
    val extraBodyJson: String,
    val extraHeadersJson: String,
    val timeoutSeconds: Int,
    val systemPrompt: String,
    val timestamp: Long
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "baseUrl" to baseUrl,
        "apiKey" to apiKey,
        "modelName" to modelName,
        "providerId" to providerId,
        "modelOfferingId" to modelOfferingId,
        "apiType" to apiType,
        "maxTokens" to maxTokens,
        "temperature" to temperature,
        "topP" to topP,
        "stream" to stream,
        "reasoningMode" to reasoningMode,
        "toolsEnabled" to toolsEnabled,
        "toolDefinitionsJson" to toolDefinitionsJson,
        "toolChoice" to toolChoice,
        "mediaInputType" to mediaInputType,
        "accessTier" to accessTier,
        "extraBodyJson" to extraBodyJson,
        "extraHeadersJson" to extraHeadersJson,
        "timeoutSeconds" to timeoutSeconds,
        "systemPrompt" to systemPrompt,
        "timestamp" to timestamp
    )

    fun toConfig(): LlmConfiguration {
        return LlmConfiguration(
            name = name,
            baseUrl = baseUrl,
            apiKey = apiKey,
            modelName = modelName,
            providerId = providerId,
            modelOfferingId = modelOfferingId,
            apiType = apiType,
            maxTokens = maxTokens,
            temperature = temperature,
            topP = topP,
            stream = stream,
            reasoningMode = reasoningMode,
            toolsEnabled = toolsEnabled,
            toolDefinitionsJson = toolDefinitionsJson,
            toolChoice = toolChoice,
            mediaInputType = mediaInputType,
            accessTier = accessTier,
            extraBodyJson = extraBodyJson,
            extraHeadersJson = extraHeadersJson,
            timeoutSeconds = timeoutSeconds,
            systemPrompt = systemPrompt,
            isActive = false,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromConfig(config: LlmConfiguration): FirebaseRouteDocument {
            return FirebaseRouteDocument(
                name = config.name,
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                modelName = config.modelName,
                providerId = config.providerId,
                modelOfferingId = config.modelOfferingId,
                apiType = config.apiType,
                maxTokens = config.maxTokens,
                temperature = config.temperature,
                topP = config.topP,
                stream = config.stream,
                reasoningMode = config.reasoningMode,
                toolsEnabled = config.toolsEnabled,
                toolDefinitionsJson = config.toolDefinitionsJson,
                toolChoice = config.toolChoice,
                mediaInputType = config.mediaInputType,
                accessTier = config.accessTier,
                extraBodyJson = config.extraBodyJson,
                extraHeadersJson = config.extraHeadersJson,
                timeoutSeconds = config.timeoutSeconds,
                systemPrompt = config.systemPrompt,
                timestamp = config.timestamp
            )
        }

        fun fromMap(data: Map<String, Any?>): FirebaseRouteDocument? {
            val name = data.stringValue("name") ?: return null
            val baseUrl = data.stringValue("baseUrl") ?: return null
            val apiKey = data.stringValue("apiKey") ?: ""
            val modelName = data.stringValue("modelName") ?: return null
            val apiType = data.stringValue("apiType") ?: return null

            return FirebaseRouteDocument(
                name = name,
                baseUrl = baseUrl,
                apiKey = apiKey,
                modelName = modelName,
                providerId = data.stringValue("providerId").orEmpty(),
                modelOfferingId = data.stringValue("modelOfferingId").orEmpty(),
                apiType = apiType,
                maxTokens = data.intValue("maxTokens") ?: 1024,
                temperature = data.doubleValue("temperature") ?: 1.0,
                topP = data.doubleValue("topP") ?: 1.0,
                stream = data.booleanValue("stream") ?: true,
                reasoningMode = data.stringValue("reasoningMode").orEmpty(),
                toolsEnabled = data.booleanValue("toolsEnabled") ?: false,
                toolDefinitionsJson = data.stringValue("toolDefinitionsJson").orEmpty(),
                toolChoice = data.stringValue("toolChoice") ?: "auto",
                mediaInputType = data.stringValue("mediaInputType") ?: "auto",
                accessTier = data.stringValue("accessTier") ?: "UNKNOWN",
                extraBodyJson = data.stringValue("extraBodyJson").orEmpty(),
                extraHeadersJson = data.stringValue("extraHeadersJson").orEmpty(),
                timeoutSeconds = data.intValue("timeoutSeconds") ?: 180,
                systemPrompt = data.stringValue("systemPrompt").orEmpty(),
                timestamp = data.longValue("timestamp") ?: System.currentTimeMillis()
            )
        }
    }
}

private fun Map<String, Any?>.stringValue(key: String): String? = this[key] as? String

private fun Map<String, Any?>.booleanValue(key: String): Boolean? = this[key] as? Boolean

private fun Map<String, Any?>.intValue(key: String): Int? {
    return when (val value = this[key]) {
        is Int -> value
        is Long -> value.toInt()
        is Double -> value.toInt()
        is Number -> value.toInt()
        else -> null
    }
}

private fun Map<String, Any?>.longValue(key: String): Long? {
    return when (val value = this[key]) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Number -> value.toLong()
        else -> null
    }
}

private fun Map<String, Any?>.doubleValue(key: String): Double? {
    return when (val value = this[key]) {
        is Double -> value
        is Long -> value.toDouble()
        is Int -> value.toDouble()
        is Number -> value.toDouble()
        else -> null
    }
}
