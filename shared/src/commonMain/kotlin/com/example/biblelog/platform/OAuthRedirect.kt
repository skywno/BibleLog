package com.example.biblelog.platform

import com.example.biblelog.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val oauthScope = CoroutineScope(Dispatchers.Main)

fun handleOAuthRedirect(url: String) {
    val fragment = url.substringAfter('#', "")
    if (fragment.isBlank() && !url.contains("access_token=")) return
    val callbackFragment = fragment.ifBlank { url.substringAfter('?', "") }
    oauthScope.launch {
        AppContainer.sessionCoordinator.completeOAuthFromCallback(callbackFragment)
    }
}
