package com.example.biblelog.data.remote

/**
 * 10.0.2.2 is the Android emulator's alias for the host machine's localhost.
 *
 * Physical device: set your PC's LAN IP here (e.g. http://192.168.0.10:8000), or run
 * `adb reverse tcp:8000 tcp:8000` and use http://127.0.0.1:8000.
 */
actual fun apiBaseUrl(): String = "http://10.0.2.2:8000"
