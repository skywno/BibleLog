package com.example.biblelog.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.biblelog.data.ApiBackedBibleLogRepository
import com.example.biblelog.data.auth.AuthRepository
import com.example.biblelog.data.auth.TokenStorage
import com.example.biblelog.data.remote.BibleLogApiClient
import com.example.biblelog.data.remote.createJsonHttpClient
import com.example.biblelog.domain.repository.BibleLogRepository

val LocalBibleLogRepository = staticCompositionLocalOf<BibleLogRepository> {
    error("BibleLogRepository not provided")
}

val LocalAuthRepository = staticCompositionLocalOf<AuthRepository> {
    error("AuthRepository not provided")
}

object AppContainer {
    private val httpClient = createJsonHttpClient()
    val apiClient: BibleLogApiClient = BibleLogApiClient(httpClient)
    val authRepository: AuthRepository = AuthRepository(apiClient, TokenStorage())
    val repository: ApiBackedBibleLogRepository = ApiBackedBibleLogRepository(apiClient)
}

@Composable
fun ProvideAppDependencies(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAuthRepository provides AppContainer.authRepository,
        LocalBibleLogRepository provides AppContainer.repository,
        content = content,
    )
}
