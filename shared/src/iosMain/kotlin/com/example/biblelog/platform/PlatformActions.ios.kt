package com.example.biblelog.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun platformKey(): String = "ios"

actual fun openExternalUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
