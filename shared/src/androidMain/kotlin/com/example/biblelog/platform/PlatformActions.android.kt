package com.example.biblelog.platform

import android.content.Intent
import android.net.Uri

actual fun platformKey(): String = "android"

actual fun openExternalUrl(url: String) {
    val context = com.example.biblelog.PlatformContextHolder.context
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
