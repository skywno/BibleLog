package com.example.biblelog.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.biblelog.data.ApiBackedBibleLogRepository
import com.example.biblelog.data.auth.AuthRepository
import com.example.biblelog.data.auth.TokenStorage
import com.example.biblelog.data.remote.AuthTokenHolder
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
    val tokenStorage: TokenStorage = TokenStorage()
    val tokenHolder: AuthTokenHolder = AuthTokenHolder()
    private val httpClient = createJsonHttpClient(tokenHolder = tokenHolder)
    val apiClient: BibleLogApiClient = BibleLogApiClient(httpClient, tokenHolder)
    val authRepository: AuthRepository = AuthRepository(apiClient, tokenStorage, tokenHolder)
    val repository: ApiBackedBibleLogRepository = ApiBackedBibleLogRepository(apiClient)
    val sessionCoordinator: SessionCoordinator = SessionCoordinator(authRepository, repository)
}

@Composable
fun ProvideAppDependencies(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAuthRepository provides AppContainer.authRepository,
        LocalBibleLogRepository provides AppContainer.repository,
        content = content,
    )
}
