package com.example.biblelog.feature.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblelog.domain.model.BibleReference
import com.example.biblelog.domain.model.ReadingProgress
import com.example.biblelog.domain.model.ReadingRecord
import com.example.biblelog.domain.model.ReadingStats
import com.example.biblelog.domain.repository.BibleLogRepository
import com.example.biblelog.util.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class BibleViewModel(
    private val repository: BibleLogRepository,
) : ViewModel() {
    val readingRecords: StateFlow<List<ReadingRecord>> = repository.readingRecords
    val readingProgress: StateFlow<ReadingProgress> = repository.readingProgress
    val readingStats: StateFlow<ReadingStats> = repository.readingStats

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun getReadingDates(): Set<LocalDate> = repository.getReadingDates()

    fun addReadingRecord(reference: BibleReference, minutesRead: Int, date: LocalDate = today()) {
        viewModelScope.launch {
            repository.addReadingRecord(reference, minutesRead, date).fold(
                onSuccess = {
                    _successMessage.value = "읽기 기록이 저장되었습니다."
                    _errorMessage.value = null
                },
                onFailure = { e ->
                    _errorMessage.value = e.message
                    _successMessage.value = null
                },
            )
        }
    }
}
