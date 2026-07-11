package com.example.biblelog.data.auth

import com.example.biblelog.data.mapper.toDomain
import com.example.biblelog.data.remote.ApiAuthTokenResponseDto
import com.example.biblelog.data.remote.ApiConfig
import com.example.biblelog.data.remote.AuthTokenHolder
import com.example.biblelog.data.remote.BibleLogApiClient
import com.example.biblelog.domain.model.UserProfile
import com.example.biblelog.platform.openExternalUrl
import com.example.biblelog.platform.platformKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OAuthProvider(val apiName: String, val label: String) {
    Google("google", "Google"),
    Facebook("facebook", "Facebook"),
}

data class AuthSession(
    val user: UserProfile,
    val accessToken: String,
    val refreshToken: String,
)

class AuthRepository(
    private val apiClient: BibleLogApiClient,
    private val tokenStorage: TokenStorage = TokenStorage(),
    private val tokenHolder: AuthTokenHolder,
) {
    private val _session = MutableStateFlow<AuthSession?>(null)
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    val isLoggedIn: Boolean get() = _session.value != null

    init {
        tokenHolder.onUnauthorized = { refreshAccessToken() }
    }

    suspend fun restoreSession(): Boolean {
        val access = tokenStorage.getAccessToken() ?: return false
        val refresh = tokenStorage.getRefreshToken() ?: return false
        tokenHolder.accessToken = access
        return runCatching {
            val user = apiClient.getCurrentUser().toDomain()
            _session.value = AuthSession(user, access, refresh)
            true
        }.getOrElse {
            if (refreshAccessToken()) {
                _session.value != null
            } else {
                clearSession()
                false
            }
        }
    }

    suspend fun refreshAccessToken(): Boolean {
        val refresh = tokenStorage.getRefreshToken() ?: return false
        return applyTokenResponse(runCatching { apiClient.refreshAuthToken(refresh) }).isSuccess
    }

    suspend fun devLogin(email: String = "demo@biblelog.app"): Result<AuthSession> =
        applyTokenResponse(runCatching { apiClient.devLogin(email) })

    suspend fun startOAuth(provider: OAuthProvider): Result<Unit> = runCatching {
        val redirectUri = ApiConfig.oauthRedirectUri(platformKey())
        val response = apiClient.getOAuthAuthorizeUrl(provider.apiName, redirectUri)
        openExternalUrl(response.authorizationUrl)
    }

    suspend fun completeOAuthFromCallback(fragment: String): Result<AuthSession> {
        val params = parseFragment(fragment)
        val access = params["access_token"] ?: error("access_token missing")
        val refresh = params["refresh_token"] ?: error("refresh_token missing")
        tokenStorage.saveTokens(access, refresh)
        tokenHolder.accessToken = access
        val user = apiClient.getCurrentUser().toDomain()
        val session = AuthSession(user, access, refresh)
        _session.value = session
        return Result.success(session)
    }

    fun applyTokens(accessToken: String, refreshToken: String, user: UserProfile) {
        tokenStorage.saveTokens(accessToken, refreshToken)
        tokenHolder.accessToken = accessToken
        _session.value = AuthSession(user, accessToken, refreshToken)
    }

    suspend fun logout() {
        runCatching { apiClient.logout() }
        clearSession()
    }

    private fun clearSession() {
        tokenStorage.clear()
        tokenHolder.accessToken = null
        _session.value = null
    }

    private suspend fun applyTokenResponse(result: Result<ApiAuthTokenResponseDto>): Result<AuthSession> =
        result.mapCatching { response ->
            tokenStorage.saveTokens(response.accessToken, response.refreshToken)
            tokenHolder.accessToken = response.accessToken
            val session = AuthSession(
                user = response.user.toDomain(),
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
            )
            _session.value = session
            session
        }

    private fun parseFragment(fragment: String): Map<String, String> {
        val raw = fragment.removePrefix("#")
        if (raw.isBlank()) return emptyMap()
        return raw.split("&").mapNotNull { part ->
            val pieces = part.split("=", limit = 2)
            if (pieces.size == 2) pieces[0] to pieces[1] else null
        }.toMap()
    }
}
