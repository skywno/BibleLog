package com.example.biblelog.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblelog.domain.model.BibleReference
import com.example.biblelog.domain.model.Emotion
import com.example.biblelog.domain.model.MeditationNote
import com.example.biblelog.domain.model.NoteVisibility
import com.example.biblelog.domain.repository.BibleLogRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JournalViewModel(
    private val repository: BibleLogRepository,
) : ViewModel() {
    val notes: StateFlow<List<MeditationNote>> = repository.notes

    fun saveNote(
        content: String,
        prayerTopic: String?,
        emotion: Emotion?,
        reference: BibleReference?,
        visibility: NoteVisibility,
        noteId: String?,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.saveNote(content, prayerTopic, emotion, reference, visibility, noteId)
            onComplete()
        }
    }

    fun deleteNote(noteId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            onComplete()
        }
    }
}
