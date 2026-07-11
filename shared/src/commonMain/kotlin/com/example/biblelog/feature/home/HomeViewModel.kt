package com.example.biblelog.feature.home

import androidx.lifecycle.ViewModel
import com.example.biblelog.domain.model.FeedItem
import com.example.biblelog.domain.model.NotificationItem
import com.example.biblelog.domain.model.ReadingProgress
import com.example.biblelog.domain.model.ReadingStats
import com.example.biblelog.domain.model.UserProfile
import com.example.biblelog.domain.repository.BibleLogRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

class HomeViewModel(
    private val repository: BibleLogRepository,
) : ViewModel() {
    val currentUser: StateFlow<UserProfile> = repository.currentUser
    val readingProgress: StateFlow<ReadingProgress> = repository.readingProgress
    val readingStats: StateFlow<ReadingStats> = repository.readingStats
    val feed: StateFlow<List<FeedItem>> = repository.feed
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications

    fun getReadingDates(): Set<LocalDate> = repository.getReadingDates()
}
