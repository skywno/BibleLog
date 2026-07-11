package com.example.biblelog.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblelog.di.SessionCoordinator
import com.example.biblelog.domain.model.ChurchSummary
import com.example.biblelog.domain.model.MeditationNote
import com.example.biblelog.domain.model.NotificationItem
import com.example.biblelog.domain.model.ProfileVisibility
import com.example.biblelog.domain.model.SmallGroupSummary
import com.example.biblelog.domain.model.UserProfile
import com.example.biblelog.domain.model.UserSearchResult
import com.example.biblelog.domain.repository.BibleLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileCommunityState(
    val church: ChurchSummary? = null,
    val smallGroups: List<SmallGroupSummary> = emptyList(),
    val friends: List<UserSearchResult> = emptyList(),
    val followers: List<UserSearchResult> = emptyList(),
    val following: List<UserSearchResult> = emptyList(),
    val isLoading: Boolean = true,
)

class ProfileViewModel(
    private val repository: BibleLogRepository,
    private val sessionCoordinator: SessionCoordinator,
) : ViewModel() {
    val currentUser: StateFlow<UserProfile> = repository.currentUser
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
    val notes: StateFlow<List<MeditationNote>> = repository.notes

    private val _communityState = MutableStateFlow(ProfileCommunityState())
    val communityState: StateFlow<ProfileCommunityState> = _communityState.asStateFlow()

    init {
        loadProfileContent()
    }

    fun loadProfileContent() {
        viewModelScope.launch {
            repository.refreshJournalNotes()
        }
        loadCommunityInfo()
    }

    fun loadCommunityInfo() {
        viewModelScope.launch {
            _communityState.update { it.copy(isLoading = true) }
            val memberships = repository.getMemberships().getOrNull()
            val church = memberships?.churchId?.let { churchId ->
                repository.getChurch(churchId).getOrNull()
            }
            val smallGroups = memberships?.groupIds?.mapNotNull { groupId ->
                repository.getSmallGroup(groupId).getOrNull()
            }.orEmpty()
            val friends = repository.listFriends().getOrDefault(emptyList())
            val followers = repository.listFollowers().getOrDefault(emptyList())
            val following = repository.listFollowingUsers().getOrDefault(emptyList())
            _communityState.update {
                it.copy(
                    church = church,
                    smallGroups = smallGroups,
                    friends = friends,
                    followers = followers,
                    following = following,
                    isLoading = false,
                )
            }
        }
    }

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
            loadCommunityInfo()
        }
    }

    fun acceptFollowRequest(requestId: String) {
        viewModelScope.launch {
            repository.acceptFollowRequest(requestId)
            loadCommunityInfo()
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
