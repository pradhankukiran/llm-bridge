package com.example.data

interface ApiKeyCipher {
    fun encrypt(value: String): String
    fun decrypt(value: String): String
}