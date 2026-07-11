package com.example.biblelog.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblelog.domain.model.FaithReaction
import com.example.biblelog.domain.model.FeedFilter
import com.example.biblelog.domain.model.FeedItem
import com.example.biblelog.domain.model.FeedSort
import com.example.biblelog.domain.repository.BibleLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommunityViewModel(
    private val repository: BibleLogRepository,
) : ViewModel() {
    val feed: StateFlow<List<FeedItem>> = repository.feed
    val feedHasMore: StateFlow<Boolean> = repository.feedHasMore

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentFilter = FeedFilter.ALL
    private var currentSort = FeedSort.LATEST

    fun loadFeed(filter: FeedFilter, sort: FeedSort) {
        currentFilter = filter
        currentSort = sort
        viewModelScope.launch {
            repository.loadFeed(filter, sort, reset = true)
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !feedHasMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            repository.loadFeed(currentFilter, currentSort, reset = false)
            _isLoadingMore.value = false
        }
    }

    fun toggleReaction(noteId: String, reaction: FaithReaction) {
        viewModelScope.launch {
            repository.toggleReaction(noteId, reaction)
        }
    }
}
