package com.example.biblelog.domain.repository

import com.example.biblelog.domain.model.MeditationNote
import com.example.biblelog.domain.model.ProfileVisibility
import com.example.biblelog.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val currentUser: StateFlow<UserProfile>

    suspend fun updateProfile(
        nickname: String? = null,
        bio: String? = null,
        photoUrl: String? = null,
        profileVisibility: ProfileVisibility? = null,
    ): Result<Unit>

    suspend fun getUserProfile(userId: String): Result<UserProfile>

    suspend fun getUserNotes(userId: String): Result<List<MeditationNote>>
}
