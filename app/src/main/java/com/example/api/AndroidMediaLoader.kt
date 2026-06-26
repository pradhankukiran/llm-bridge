package com.example.api

import android.content.Context
import com.example.api.adapter.MediaLoader

class AndroidMediaLoader(private val context: Context) : MediaLoader {
    override fun loadAsDataUrl(uriString: String): String {
        if (!uriString.startsWith("content://") && !uriString.startsWith("file://")) {
            return uriString
        }
        return try {
            val uri = android.net.Uri.parse(uriString)
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                "data:$mimeType;base64,$base64"
            } ?: uriString
        } catch (e: Exception) {
            android.util.Log.e("AndroidMediaLoader", "Failed to convert Uri to data URL: $uriString", e)
            uriString
        }
    }
}