package com.example.biblelog.navigation

import com.example.biblelog.domain.model.BibleReference

internal fun BibleReference?.toSaveableString(): String? = this?.let { ref ->
    listOf(
        ref.bookId,
        ref.startChapter,
        ref.startVerse,
        ref.endBookId,
        ref.endChapter,
        ref.endVerse,
    ).joinToString(",")
}

internal fun parseBibleReferenceSaveable(raw: String?): BibleReference? {
    if (raw.isNullOrBlank()) return null
    val parts = raw.split(",").mapNotNull { it.toIntOrNull() }
    if (parts.size != 6) return null
    return BibleReference(
        bookId = parts[0],
        startChapter = parts[1],
        startVerse = parts[2],
        endBookId = parts[3],
        endChapter = parts[4],
        endVerse = parts[5],
    )
}
