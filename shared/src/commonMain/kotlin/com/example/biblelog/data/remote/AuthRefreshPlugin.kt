package com.example.biblelog.data.remote

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class AuthRefreshConfig {
    var onUnauthorized: (suspend () -> Boolean)? = null
    var authorizationHeader: (() -> String?)? = null
}

val AuthRefreshPlugin = createClientPlugin("AuthRefresh", ::AuthRefreshConfig) {
    on(Send) { request ->
        val call = proceed(request)
        if (
            call.response.status == HttpStatusCode.Unauthorized &&
            request.headers.contains(HttpHeaders.Authorization)
        ) {
            val refreshed = pluginConfig.onUnauthorized?.invoke() == true
            if (refreshed) {
                request.headers.remove(HttpHeaders.Authorization)
                pluginConfig.authorizationHeader?.invoke()?.let { token ->
                    request.headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
                proceed(request)
            } else {
                call
            }
        } else {
            call
        }
    }
}
