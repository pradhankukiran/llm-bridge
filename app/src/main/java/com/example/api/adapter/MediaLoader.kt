package com.example.api.adapter

interface MediaLoader {
    fun loadAsDataUrl(uriString: String): String
}