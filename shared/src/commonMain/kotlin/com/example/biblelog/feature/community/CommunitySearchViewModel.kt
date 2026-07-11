package com.example.biblelog.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblelog.domain.model.ChurchSummary
import com.example.biblelog.domain.model.CommunitySearchCategory
import com.example.biblelog.domain.model.SmallGroupSummary
import com.example.biblelog.domain.model.UserSearchResult
import com.example.biblelog.domain.repository.BibleLogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunitySearchUiState(
    val query: String = "",
    val category: CommunitySearchCategory = CommunitySearchCategory.USERS,
    val users: List<UserSearchResult> = emptyList(),
    val churches: List<ChurchSummary> = emptyList(),
    val smallGroups: List<SmallGroupSummary> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val friendIds: Set<String> = emptySet(),
    val followingIds: Set<String> = emptySet(),
    val pendingFriendRequestIds: Set<String> = emptySet(),
    val joinedChurchId: String? = null,
    val joinedGroupIds: Set<String> = emptySet(),
    val actionMessage: String? = null,
)

class CommunitySearchViewModel(
    private val repository: BibleLogRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CommunitySearchUiState())
    val uiState: StateFlow<CommunitySearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadRelationshipContext()
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query, actionMessage = null) }
        scheduleSearch()
    }

    fun updateCategory(category: CommunitySearchCategory) {
        _uiState.update { it.copy(category = category, actionMessage = null) }
        scheduleSearch()
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            repository.sendFriendRequest(userId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            pendingFriendRequestIds = it.pendingFriendRequestIds + userId,
                            actionMessage = "친구 요청을 보냈습니다.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(actionMessage = error.message ?: "친구 요청에 실패했습니다.") }
                }
        }
    }

    fun followUser(userId: String) {
        viewModelScope.launch {
            repository.followUser(userId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            followingIds = it.followingIds + userId,
                            actionMessage = "팔로우했습니다.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(actionMessage = error.message ?: "팔로우에 실패했습니다.") }
                }
        }
    }

    fun joinChurch(churchId: String) {
        viewModelScope.launch {
            repository.joinChurch(churchId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            joinedChurchId = churchId,
                            actionMessage = "교회에 가입했습니다.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(actionMessage = error.message ?: "교회 가입에 실패했습니다.") }
                }
        }
    }

    fun joinSmallGroup(groupId: String) {
        viewModelScope.launch {
            repository.joinSmallGroup(groupId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            joinedGroupIds = it.joinedGroupIds + groupId,
                            actionMessage = "소그룹에 가입했습니다.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(actionMessage = error.message ?: "소그룹 가입에 실패했습니다.") }
                }
        }
    }

    private fun loadRelationshipContext() {
        viewModelScope.launch {
            repository.getFriendIds().onSuccess { friendIds ->
                _uiState.update { it.copy(friendIds = friendIds) }
            }
            repository.getFollowingIds().onSuccess { followingIds ->
                _uiState.update { it.copy(followingIds = followingIds) }
            }
            repository.getMemberships().onSuccess { memberships ->
                _uiState.update {
                    it.copy(
                        joinedChurchId = memberships.churchId,
                        joinedGroupIds = memberships.groupIds,
                    )
                }
            }
        }
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val query = _uiState.value.query.trim()
            if (query.isEmpty()) {
                _uiState.update {
                    it.copy(
                        users = emptyList(),
                        churches = emptyList(),
                        smallGroups = emptyList(),
                        isSearching = false,
                        hasSearched = false,
                    )
                }
                return@launch
            }
            delay(300)
            _uiState.update { it.copy(isSearching = true) }
            val category = _uiState.value.category
            when (category) {
                CommunitySearchCategory.USERS -> {
                    repository.searchUsers(query)
                        .onSuccess { users ->
                            _uiState.update { it.copy(users = users, isSearching = false, hasSearched = true) }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    users = emptyList(),
                                    isSearching = false,
                                    hasSearched = true,
                                    actionMessage = error.message ?: "검색에 실패했습니다.",
                                )
                            }
                        }
                }
                CommunitySearchCategory.CHURCHES -> {
                    repository.searchChurches(query)
                        .onSuccess { churches ->
                            _uiState.update { it.copy(churches = churches, isSearching = false, hasSearched = true) }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    churches = emptyList(),
                                    isSearching = false,
                                    hasSearched = true,
                                    actionMessage = error.message ?: "검색에 실패했습니다.",
                                )
                            }
                        }
                }
                CommunitySearchCategory.SMALL_GROUPS -> {
                    repository.searchSmallGroups(query)
                        .onSuccess { smallGroups ->
                            _uiState.update {
                                it.copy(smallGroups = smallGroups, isSearching = false, hasSearched = true)
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    smallGroups = emptyList(),
                                    isSearching = false,
                                    hasSearched = true,
                                    actionMessage = error.message ?: "검색에 실패했습니다.",
                                )
                            }
                        }
                }
            }
        }
    }
}
