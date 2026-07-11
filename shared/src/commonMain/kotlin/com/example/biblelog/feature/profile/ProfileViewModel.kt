package com.example.biblelog.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblelog.di.SessionCoordinator
import com.example.biblelog.domain.model.NotificationItem
import com.example.biblelog.domain.model.ProfileVisibility
import com.example.biblelog.domain.model.UserProfile
import com.example.biblelog.domain.repository.BibleLogRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: BibleLogRepository,
    private val sessionCoordinator: SessionCoordinator,
) : ViewModel() {
    val currentUser: StateFlow<UserProfile> = repository.currentUser
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications

    fun updateProfile(
        nickname: String,
        bio: String,
        photoUrl: String,
        profileVisibility: ProfileVisibility,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.updateProfile(
                nickname = nickname,
                bio = bio,
                photoUrl = photoUrl,
                profileVisibility = profileVisibility,
            )
            onComplete()
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            repository.acceptFriendRequest(requestId)
        }
    }

    fun acceptFollowRequest(requestId: String) {
        viewModelScope.launch {
            repository.acceptFollowRequest(requestId)
        }
    }

    fun rejectFollowRequest(requestId: String) {
        viewModelScope.launch {
            repository.rejectFollowRequest(requestId)
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionCoordinator.onLogout()
        }
    }
}
