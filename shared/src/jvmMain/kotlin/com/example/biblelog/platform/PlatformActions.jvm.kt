package com.example.biblelog.platform

actual fun platformKey(): String = "desktop"

actual fun openExternalUrl(url: String) {
    runCatching {
        val desktop = Class.forName("java.awt.Desktop")
        val desktopInstance = desktop.getMethod("getDesktop").invoke(null)
        desktop.getMethod("browse", java.net.URI::class.java)
            .invoke(desktopInstance, java.net.URI(url))
    }.onFailure {
        println("Open URL manually: $url")
    }
}
