package com.example.biblelog.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblelog.domain.model.MeditationNote
import com.example.biblelog.domain.model.UserProfile
import com.example.biblelog.domain.repository.BibleLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val profile: UserProfile? = null,
    val notes: List<MeditationNote> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isFriend: Boolean = false,
    val isFollowing: Boolean = false,
    val isPendingFriend: Boolean = false,
    val isPendingFollow: Boolean = false,
    val actionMessage: String? = null,
)

class UserProfileViewModel(
    private val repository: BibleLogRepository,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun sendFriendRequest() {
        viewModelScope.launch {
            repository.sendFriendRequest(userId)
                .onSuccess {
                    _uiState.update { it.copy(isPendingFriend = true, actionMessage = "친구 요청을 보냈습니다.") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(actionMessage = error.message ?: "친구 요청에 실패했습니다.") }
                }
        }
    }

    fun followUser() {
        viewModelScope.launch {
            repository.followUser(userId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isFollowing = true,
                            isPendingFollow = false,
                            actionMessage = "팔로우했습니다.",
                        )
                    }
                }
                .onFailure { error ->
                    val message = error.message.orEmpty()
                    if (message.contains("pending", ignoreCase = true) || message.contains("요청", ignoreCase = true)) {
                        _uiState.update { it.copy(isPendingFollow = true, actionMessage = "팔로우 요청을 보냈습니다.") }
                    } else {
                        _uiState.update { it.copy(actionMessage = message.ifBlank { "팔로우에 실패했습니다." }) }
                    }
                }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val profileResult = repository.getUserProfile(userId)
            val notesResult = repository.getUserNotes(userId)
            val friendIds = repository.getFriendIds().getOrDefault(emptySet())
            val followingIds = repository.getFollowingIds().getOrDefault(emptySet())

            profileResult
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            notes = notesResult.getOrDefault(emptyList()),
                            isFriend = userId in friendIds,
                            isFollowing = userId in followingIds,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "프로필을 불러오지 못했습니다.",
                        )
                    }
                }
        }
    }
}
