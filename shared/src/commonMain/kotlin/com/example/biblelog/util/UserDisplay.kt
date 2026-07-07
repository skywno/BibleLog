package com.example.biblelog.util

import com.example.biblelog.domain.model.UserProfile

fun UserProfile.avatarInitial(): String = nickname.firstOrNull()?.toString() ?: "?"
