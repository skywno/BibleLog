package com.example.biblelog.platform

import kotlinx.browser.window

actual fun platformKey(): String = "web"

actual fun openExternalUrl(url: String) {
    window.open(url, "_blank")
}
