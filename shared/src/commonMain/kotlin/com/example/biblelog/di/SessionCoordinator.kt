package com.example.biblelog.di

import com.example.biblelog.data.ApiBackedBibleLogRepository
import com.example.biblelog.data.auth.AuthRepository
import com.example.biblelog.data.auth.AuthSession
import com.example.biblelog.data.auth.TokenStorage

class SessionCoordinator(
    private val authRepository: AuthRepository,
    private val repository: ApiBackedBibleLogRepository,
) {
    suspend fun restoreSession(): Boolean {
        val restored = authRepository.restoreSession()
        if (restored) {
            runCatching { repository.refreshAll() }
        }
        return restored
    }

    suspend fun onLogin(session: AuthSession) {
        authRepository.applyTokens(session.accessToken, session.refreshToken, session.user)
        repository.clearState()
        runCatching { repository.refreshAll() }
    }

    suspend fun completeOAuthFromCallback(fragment: String): Result<AuthSession> =
        authRepository.completeOAuthFromCallback(fragment).onSuccess { session ->
            repository.clearState()
            runCatching { repository.refreshAll() }
        }

    suspend fun onLogout() {
        authRepository.logout()
        repository.clearState()
    }
}
