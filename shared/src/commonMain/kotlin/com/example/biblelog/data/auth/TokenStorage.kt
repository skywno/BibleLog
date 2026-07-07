package com.example.biblelog.data.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class TokenStorage(
    private val settings: Settings = Settings(),
) {
    fun getAccessToken(): String? = settings.getStringOrNull(KEY_ACCESS)

    fun getRefreshToken(): String? = settings.getStringOrNull(KEY_REFRESH)

    fun saveTokens(accessToken: String, refreshToken: String) {
        settings[KEY_ACCESS] = accessToken
        settings[KEY_REFRESH] = refreshToken
    }

    fun clear() {
        settings.remove(KEY_ACCESS)
        settings.remove(KEY_REFRESH)
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
