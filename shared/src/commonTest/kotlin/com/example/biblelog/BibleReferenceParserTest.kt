package com.example.biblelog

import com.example.biblelog.data.BibleReferenceParser
import com.example.biblelog.data.ParseError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BibleReferenceParserTest {

    @Test
    fun parse_colon_with_space() {
        assertPoint("창 1:30", 1, 1, 30)
        assertPoint("창 1:1", 1, 1, 1)
        assertPoint("시 23:1", 19, 23, 1)
    }

    @Test
    fun parse_colon_without_space() {
        assertPoint("창1:30", 1, 1, 30)
        assertPoint("시23:1", 19, 23, 1)
        assertPoint("gen1:1", 1, 1, 1)
    }

    @Test
    fun parse_korean_full_with_jang_jul() {
        assertPoint("창세기 1장 30절", 1, 1, 30)
        assertPoint("창세기 1장 30", 1, 1, 30)
        assertPoint("창 1장 30절", 1, 1, 30)
        assertPoint("창1장30절", 1, 1, 30)
    }

    @Test
    fun parse_space_separated() {
        assertPoint("창세기 1 30", 1, 1, 30)
        assertPoint("창 1 30", 1, 1, 30)
    }

    @Test
    fun parse_english() {
        assertPoint("Genesis 1:30", 1, 1, 30)
        assertPoint("Gen 1:30", 1, 1, 30)
        assertPoint("Matt 5:3", 40, 5, 3)
        assertPoint("John 3:16", 43, 3, 16)
    }

    @Test
    fun parse_numbered_books() {
        assertPoint("1코 13:4", 46, 13, 4)
        assertPoint("1co 13:4", 46, 13, 4)
        assertPoint("고전 13:4", 46, 13, 4)
        assertPoint("삼상 17:45", 9, 17, 45)
        assertPoint("2ki 2:11", 12, 2, 11)
    }

    @Test
    fun parse_catholic_comma_style() {
        assertPoint("창세 1,30", 1, 1, 30)
    }

    @Test
    fun parse_fullwidth_digits() {
        assertPoint("창 １:３０", 1, 1, 30)
    }

    @Test
    fun parse_extra_whitespace() {
        assertPoint("  창   1  :  30  ", 1, 1, 30)
    }

    @Test
    fun parse_invalid_chapter_returns_out_of_range() {
        val result = BibleReferenceParser.parse("창 999:1")
        assertEquals(ParseError.OutOfRange, result.error)
        assertNull(result.point)
    }

    @Test
    fun parse_invalid_verse_returns_out_of_range() {
        val result = BibleReferenceParser.parse("창 1:999")
        assertEquals(ParseError.OutOfRange, result.error)
    }

    @Test
    fun parse_unknown_book_returns_error() {
        val result = BibleReferenceParser.parse("없는책 1:1")
        assertEquals(ParseError.UnknownBook, result.error)
    }

    @Test
    fun parse_invalid_format_returns_error() {
        val result = BibleReferenceParser.parse("창세기")
        assertEquals(ParseError.InvalidFormat, result.error)
    }

    @Test
    fun parse_empty_returns_error() {
        val result = BibleReferenceParser.parse("")
        assertEquals(ParseError.EmptyInput, result.error)
    }

    @Test
    fun parseOrNull_returns_null_on_failure() {
        assertNull(BibleReferenceParser.parseOrNull("invalid"))
    }

    @Test
    fun parse_psalms_aliases() {
        assertPoint("시편 119:105", 19, 119, 105)
        assertPoint("psa 23:1", 19, 23, 1)
    }

    @Test
    fun parse_revelation() {
        assertPoint("계 22:21", 66, 22, 21)
        assertPoint("rev 22:21", 66, 22, 21)
    }

    @Test
    fun parse_john_epistles() {
        assertPoint("요일 1:1", 62, 1, 1)
        assertPoint("1jn 4:8", 62, 4, 8)
    }

    @Test
    fun parse_isaiah() {
        assertPoint("사 53:5", 23, 53, 5)
        assertPoint("isaiah 53:5", 23, 53, 5)
    }

    @Test
    fun parse_romans() {
        assertPoint("롬 8:28", 45, 8, 28)
        assertPoint("rom 8:28", 45, 8, 28)
    }

    @Test
    fun parse_acts() {
        assertPoint("행 2:38", 44, 2, 38)
        assertPoint("acts 2:38", 44, 2, 38)
    }

    @Test
    fun parse_proverbs() {
        assertPoint("잠 3:5", 20, 3, 5)
    }

    @Test
    fun parse_ecclesiastes() {
        assertPoint("전 3:1", 21, 3, 1)
    }

    @Test
    fun parse_exodus() {
        assertPoint("출 14:21", 2, 14, 21)
        assertPoint("exodus 14:21", 2, 14, 21)
    }

    private fun assertPoint(input: String, bookId: Int, chapter: Int, verse: Int) {
        val result = BibleReferenceParser.parse(input)
        assertNull(result.error, "Unexpected error for '$input': ${result.error}")
        val point = assertNotNull(result.point, "Expected point for '$input'")
        assertEquals(bookId, point.bookId, "bookId for '$input'")
        assertEquals(chapter, point.chapter, "chapter for '$input'")
        assertEquals(verse, point.verse, "verse for '$input'")
    }
}
