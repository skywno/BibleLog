package com.example.biblelog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.biblelog.platform.handleOAuthRedirect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        PlatformContextHolder.context = applicationContext

        handleOAuthIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme == "biblelog") {
            handleOAuthRedirect(uri.toString())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
