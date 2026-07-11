package com.example.biblelog.domain.repository

import com.example.biblelog.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val currentUser: StateFlow<UserProfile>

    suspend fun updateProfile(nickname: String, bio: String): Result<Unit>
}
