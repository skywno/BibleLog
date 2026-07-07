package com.example.biblelog

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform