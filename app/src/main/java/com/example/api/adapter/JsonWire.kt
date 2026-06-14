package com.example.api.adapter

import com.example.data.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

internal object JsonWire {
    fun messagesArray(chatHistory: List<ChatMessage>, includeSystem: Boolean = true): JSONArray {
        val messagesArray = JSONArray()
        for (msg in chatHistory) {
            if (!includeSystem && msg.role == "system") continue
            messagesArray.put(JSONObject().apply {
                put("role", msg.role)
                put("content", msg.content)
            })
        }
        return messagesArray
    }

    fun parseOpenAiResponse(rawJson: String): String {
        return try {
            val root = JSONObject(rawJson)
            val choices = root.optJSONArray("choices")
                ?: findArrayValue(root, "choices")
                ?: return root.optString("text", "Error: No response choices found.")
            if (choices.length() == 0) return "Error: No response choices found."

            val firstChoice = choices.getJSONObject(0)
            val messageObj = firstChoice.optJSONObject("message")
                ?: return firstChoice.optString("text", "Error: Empty assistant message.")

            when {
                !messageObj.isNull("content") -> messageObj.optString("content")
                !messageObj.isNull("reasoning_content") -> messageObj.optString("reasoning_content")
                messageObj.has("tool_calls") -> messageObj.getJSONArray("tool_calls").toString(2)
                else -> "Error: Empty assistant message."
            }
        } catch (e: Exception) {
            "Error parsing OpenAI style JSON: ${e.localizedMessage}\nRaw: $rawJson"
        }
    }

    fun parseOpenAiStreamLine(line: String): String? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("data:")) return null
        val payload = trimmed.removePrefix("data:").trim()
        if (payload.isBlank() || payload == "[DONE]") return null
        return try {
            val root = JSONObject(payload)
            val choices = root.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val delta = choices.getJSONObject(0).optJSONObject("delta") ?: return null
            if (!delta.isNull("content")) {
                delta.optString("content")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun parseOpenAiStreamResponse(rawText: String): String {
        val builder = StringBuilder()
        rawText.lineSequence()
            .forEach { line ->
                val chunk = parseOpenAiStreamLine(line)
                if (chunk != null) {
                    builder.append(chunk)
                }
            }
        return builder.toString().ifBlank { "Error: Stream ended without content.\nRaw: $rawText" }
    }

    fun parseAnthropicResponse(rawJson: String): String {
        return try {
            val root = JSONObject(rawJson)
            val contentArray = root.getJSONArray("content")
            if (contentArray.length() == 0) return "Error: No content array elements found."

            val responseText = StringBuilder()
            for (i in 0 until contentArray.length()) {
                val element = contentArray.getJSONObject(i)
                if (element.optString("type") == "text") {
                    responseText.append(element.optString("text"))
                }
            }
            responseText.toString().ifBlank { "Error: Empty text block returned." }
        } catch (e: Exception) {
            "Error parsing Anthropic style JSON: ${e.localizedMessage}\nRaw: $rawJson"
        }
    }

    fun mergeJsonObject(target: JSONObject, extraJson: String) {
        if (extraJson.isBlank()) return
        val extra = JSONObject(extraJson)
        val keys = extra.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            target.put(key, extra.get(key))
        }
    }

    fun mergeHeaders(headers: MutableMap<String, String>, extraHeadersJson: String) {
        if (extraHeadersJson.isBlank()) return
        val extra = JSONObject(extraHeadersJson)
        val keys = extra.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            headers[key] = extra.getString(key)
        }
    }

    fun errorMessageSnippet(rawJson: String): String {
        return try {
            val root = JSONObject(rawJson)
            if (root.has("error")) {
                val errObj = root.optJSONObject("error")
                errObj?.optString("message", rawJson) ?: root.optString("error", rawJson)
            } else {
                rawJson.take(200)
            }
        } catch (_: Exception) {
            rawJson.take(200)
        }
    }

    fun findStringValue(rawJson: String, candidateKeys: Set<String>): String? {
        return try {
            findStringValue(JSONObject(rawJson), candidateKeys)
        } catch (_: Exception) {
            null
        }
    }

    private fun findStringValue(value: Any?, candidateKeys: Set<String>): String? {
        return when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = value.opt(key)
                    if (candidateKeys.contains(key) && child is String && child.isNotBlank()) {
                        return child
                    }
                    findStringValue(child, candidateKeys)?.let { return it }
                }
                null
            }
            is JSONArray -> {
                for (i in 0 until value.length()) {
                    findStringValue(value.opt(i), candidateKeys)?.let { return it }
                }
                null
            }
            else -> null
        }
    }

    private fun findArrayValue(value: Any?, targetKey: String): JSONArray? {
        return when (value) {
            is JSONObject -> {
                val direct = value.optJSONArray(targetKey)
                if (direct != null) return direct

                val keys = value.keys()
                while (keys.hasNext()) {
                    findArrayValue(value.opt(keys.next()), targetKey)?.let { return it }
                }
                null
            }
            is JSONArray -> {
                for (i in 0 until value.length()) {
                    findArrayValue(value.opt(i), targetKey)?.let { return it }
                }
                null
            }
            else -> null
        }
    }
}
