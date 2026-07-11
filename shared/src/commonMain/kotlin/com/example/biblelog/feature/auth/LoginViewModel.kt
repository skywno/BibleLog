package com.example.biblelog.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblelog.data.auth.AuthRepository
import com.example.biblelog.data.auth.OAuthProvider
import com.example.biblelog.di.SessionCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val sessionCoordinator: SessionCoordinator,
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startOAuth(provider: OAuthProvider) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.startOAuth(provider).fold(
                onSuccess = { },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "OAuth 시작에 실패했습니다."
                },
            )
            _isLoading.value = false
        }
    }

    fun devLogin() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.devLogin().fold(
                onSuccess = { session -> sessionCoordinator.onLogin(session) },
                onFailure = { error ->
                    _errorMessage.value = error.message
                        ?: "로그인에 실패했습니다. FastAPI 서버가 실행 중인지 확인해 주세요."
                },
            )
            _isLoading.value = false
        }
    }
}
