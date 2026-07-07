package com.example.biblelog.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Js)
