package com.example.biblelog.domain.repository

import com.example.biblelog.domain.model.BibleReference
import com.example.biblelog.domain.model.ReadingProgress
import com.example.biblelog.domain.model.ReadingRecord
import com.example.biblelog.domain.model.ReadingStats
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

interface ReadingRepository {
    val readingRecords: StateFlow<List<ReadingRecord>>
    val readingProgress: StateFlow<ReadingProgress>
    val readingStats: StateFlow<ReadingStats>

    fun getReadingDates(): Set<LocalDate>

    suspend fun addReadingRecord(
        reference: BibleReference,
        minutesRead: Int,
        date: LocalDate,
    ): Result<Unit>
}
