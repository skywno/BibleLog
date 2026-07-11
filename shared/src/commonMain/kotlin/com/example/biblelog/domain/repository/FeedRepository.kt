package com.example.biblelog.domain.repository

import com.example.biblelog.domain.model.FaithReaction
import com.example.biblelog.domain.model.FeedFilter
import com.example.biblelog.domain.model.FeedItem
import com.example.biblelog.domain.model.FeedSort
import kotlinx.coroutines.flow.StateFlow

interface FeedRepository {
    val feed: StateFlow<List<FeedItem>>
    val feedHasMore: StateFlow<Boolean>
    val feedNextCursor: StateFlow<String?>

    suspend fun loadFeed(
        filter: FeedFilter = FeedFilter.ALL,
        sort: FeedSort = FeedSort.LATEST,
        reset: Boolean = true,
    ): Result<Unit>

    suspend fun toggleReaction(noteId: String, reaction: FaithReaction): Result<Unit>
}
