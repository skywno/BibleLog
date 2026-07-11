package com.example.biblelog.data

data class ParsedBiblePoint(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
)

object BibleReferenceParser {
    private val aliases: Map<String, Int> = buildMap {
        BibleCatalog.books.forEach { book ->
            put(book.nameKo.lowercase(), book.id)
            put(book.nameEn.lowercase(), book.id)
            val shortKo = book.nameKo
                .removePrefix("1 ")
                .removePrefix("2 ")
                .removePrefix("3 ")
                .take(2)
                .lowercase()
            if (shortKo.isNotEmpty()) put(shortKo, book.id)
            put(book.nameEn.split(' ').first().lowercase(), book.id)
        }
        put("창", 1)
        put("출", 2)
        put("레", 3)
        put("민", 4)
        put("신", 5)
        put("수", 6)
        put("삿", 7)
        put("룻", 8)
        put("삼상", 9)
        put("삼하", 10)
        put("왕상", 11)
        put("왕하", 12)
        put("대상", 13)
        put("대하", 14)
        put("스", 19)
        put("시", 19)
        put("잠", 20)
        put("전", 21)
        put("아", 22)
        put("사", 23)
        put("렘", 24)
        put("애", 25)
        put("겔", 26)
        put("단", 27)
        put("마", 40)
        put("막", 41)
        put("눅", 42)
        put("요", 43)
        put("행", 44)
        put("롬", 45)
        put("고전", 46)
        put("고후", 47)
        put("갈", 48)
        put("엡", 49)
        put("빌", 50)
        put("골", 51)
        put("살전", 52)
        put("살후", 53)
        put("딤전", 54)
        put("딤후", 55)
        put("딛", 56)
        put("몬", 57)
        put("히", 58)
        put("약", 59)
        put("벧전", 60)
        put("벧후", 61)
        put("요일", 62)
        put("요이", 63)
        put("요삼", 64)
        put("유", 65)
        put("계", 66)
        put("gen", 1)
        put("exo", 2)
        put("psa", 19)
        put("ps", 19)
        put("mat", 40)
        put("mrk", 41)
        put("luk", 42)
        put("jhn", 43)
        put("rev", 66)
    }

    fun parse(input: String): ParsedBiblePoint? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val match = Regex(
            """^(?:(\d)\s*)?([^\d\s:]+)\s*(\d+)\s*:\s*(\d+)$""",
            RegexOption.IGNORE_CASE,
        ).find(trimmed) ?: return null

        val prefix = match.groupValues[1]
        val bookToken = match.groupValues[2].lowercase()
        val chapter = match.groupValues[3].toIntOrNull() ?: return null
        val verse = match.groupValues[4].toIntOrNull() ?: return null

        val bookId = resolveBookId(bookToken, prefix) ?: return null
        if (!BibleCatalog.isValidPoint(bookId, chapter, verse)) return null
        return ParsedBiblePoint(bookId, chapter, verse)
    }

    private fun resolveBookId(token: String, prefix: String): Int? {
        val direct = aliases[token]
        if (direct != null) {
            if (prefix.isNotEmpty()) {
                val numbered = when (prefix) {
                    "1" -> BibleCatalog.books.find { it.nameEn.startsWith("1 ") && it.id == direct }?.id
                        ?: BibleCatalog.books.find { it.nameKo.startsWith("1") && aliases[token] == it.id }?.id
                    "2" -> BibleCatalog.books.find { it.nameEn.startsWith("2 ") && it.nameEn.contains(token, ignoreCase = true) }?.id
                    "3" -> BibleCatalog.books.find { it.nameEn.startsWith("3 ") && it.nameEn.contains(token, ignoreCase = true) }?.id
                    else -> direct
                }
                if (numbered != null) return numbered
            }
            return direct
        }

        val numberedMatch = Regex("""^(\d)(.+)$""").find(token)
        if (numberedMatch != null) {
            val num = numberedMatch.groupValues[1]
            val rest = numberedMatch.groupValues[2]
            val base = aliases[rest] ?: return null
            return when (num) {
                "1" -> BibleCatalog.books.find { it.nameKo.startsWith("1") && it.id == base }?.id
                    ?: BibleCatalog.books.find { it.nameEn.startsWith("1 ") && it.nameEn.lowercase().contains(rest) }?.id
                "2" -> BibleCatalog.books.find { it.nameKo.startsWith("2") && it.nameKo.contains(rest, ignoreCase = true) }?.id
                    ?: BibleCatalog.books.find { it.nameEn.startsWith("2 ") && it.nameEn.lowercase().contains(rest) }?.id
                "3" -> BibleCatalog.books.find { it.nameKo.startsWith("3") && it.nameKo.contains(rest, ignoreCase = true) }?.id
                    ?: BibleCatalog.books.find { it.nameEn.startsWith("3 ") && it.nameEn.lowercase().contains(rest) }?.id
                else -> base
            }
        }

        return BibleCatalog.books.find {
            it.nameKo.equals(token, ignoreCase = true) ||
                it.nameEn.equals(token, ignoreCase = true) ||
                it.nameKo.contains(token, ignoreCase = true) ||
                it.nameEn.contains(token, ignoreCase = true)
        }?.id
    }
}
