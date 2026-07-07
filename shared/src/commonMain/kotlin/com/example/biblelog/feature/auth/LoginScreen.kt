package com.example.biblelog.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.biblelog.data.auth.AuthRepository
import com.example.biblelog.data.auth.OAuthProvider
import com.example.biblelog.di.LocalAuthRepository
import com.example.biblelog.ui.components.WantedButton
import com.example.biblelog.ui.components.WantedButtonVariant
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedSpacing
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoggedIn: () -> Unit = {},
) {
    val authRepository = LocalAuthRepository.current
    val session by authRepository.session.collectAsState()
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    if (session != null) {
        onLoggedIn()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(WantedSpacing.Xl.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "BibleLog",
            style = MaterialTheme.typography.displayLarge,
            color = WantedColors.Primary,
        )
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        Text(
            text = "성경 읽기 습관과\n신앙 공동체를 함께",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(WantedSpacing.Section.dp))

        OAuthProvider.entries.forEach { provider ->
            WantedButton(
                text = "${provider.label}로 계속하기",
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        authRepository.startOAuth(provider).fold(
                            onSuccess = { },
                            onFailure = { error ->
                                errorMessage = error.message ?: "OAuth 시작에 실패했습니다."
                            },
                        )
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                variant = if (provider == OAuthProvider.Google) {
                    WantedButtonVariant.Primary
                } else {
                    WantedButtonVariant.Outlined
                },
            )
            Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        WantedButton(
            text = "개발 서버로 로그인",
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    authRepository.devLogin().fold(
                        onSuccess = { onLoggedIn() },
                        onFailure = { error ->
                            errorMessage = error.message ?: "로그인에 실패했습니다. FastAPI 서버가 실행 중인지 확인해 주세요."
                        },
                    )
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            variant = WantedButtonVariant.Secondary,
        )

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = WantedColors.Error,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Xl.dp))
        Text(
            text = "로그인하면 읽기 기록·묵상·AI 대화가 서버와 동기화됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = WantedColors.Secondary,
            textAlign = TextAlign.Center,
        )
    }
}
