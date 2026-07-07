package com.example.biblelog.data.remote

expect fun apiBaseUrl(): String

object ApiConfig {
    val BASE_URL: String get() = apiBaseUrl()

    fun oauthRedirectUri(platform: String): String = when (platform) {
        "web" -> "http://localhost:8080/auth/callback"
        "desktop" -> "http://localhost:8765/auth/callback"
        else -> "biblelog://auth/callback"
    }
}
