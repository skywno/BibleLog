package com.example.biblelog.domain.repository

import com.example.biblelog.domain.model.BibleReference
import com.example.biblelog.domain.model.Emotion
import com.example.biblelog.domain.model.MeditationNote
import com.example.biblelog.domain.model.NoteVisibility
import kotlinx.coroutines.flow.StateFlow

interface JournalRepository {
    val notes: StateFlow<List<MeditationNote>>

    suspend fun saveNote(
        content: String,
        prayerTopic: String?,
        emotion: Emotion?,
        reference: BibleReference?,
        visibility: NoteVisibility,
        noteId: String? = null,
    ): Result<MeditationNote>

    suspend fun deleteNote(noteId: String): Result<Unit>

    suspend fun refreshJournalNotes(): Result<Unit>
}
