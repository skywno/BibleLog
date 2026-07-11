package com.example.biblelog.data.remote

class AuthTokenHolder {
    var accessToken: String? = null
    var onUnauthorized: (suspend () -> Boolean)? = null
}
