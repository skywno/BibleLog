package com.example.biblelog.data

/**
 * 개역개정 약어 및 한/영 성경 권 이름 별칭.
 * [matchBookToken]은 입력 앞부분에서 **가장 긴 alias**를 greedy 매칭한다.
 */
object BibleBookAliases {
    private data class AliasEntry(val alias: String, val bookId: Int)

    private val sortedEntries: List<AliasEntry> = buildEntries()
        .distinctBy { it.alias.lowercase() }
        .sortedByDescending { it.alias.length }

    private fun buildEntries(): List<AliasEntry> = buildList {
        BibleCatalog.books.forEach { book ->
            add(AliasEntry(book.nameKo.lowercase(), book.id))
            add(AliasEntry(book.nameEn.lowercase(), book.id))
            book.nameEn.split(' ').forEach { part ->
                if (part.isNotBlank()) add(AliasEntry(part.lowercase(), book.id))
            }
        }
        manualEntries().forEach { (alias, id) -> add(AliasEntry(alias.lowercase(), id)) }
    }

    /** (bookId, remainder after matched alias) */
    fun matchBookToken(input: String): Pair<Int, String>? {
        val normalized = input.trim().lowercase()
        if (normalized.isEmpty()) return null

        matchLongestAlias(normalized)?.let { return it }

        val numberedPrefix = Regex("""^([123])\s*(.+)$""").find(normalized) ?: return null
        val prefix = numberedPrefix.groupValues[1].toInt()
        val rest = numberedPrefix.groupValues[2]
        matchLongestAlias(rest)?.let { (bookId, remainder) ->
            val resolved = resolveNumbered(prefix, bookId) ?: bookId
            return resolved to remainder
        }
        return null
    }

    private fun matchLongestAlias(input: String): Pair<Int, String>? {
        for (entry in sortedEntries) {
            if (!input.startsWith(entry.alias)) continue
            val remainder = input.drop(entry.alias.length)
            if (remainder.isEmpty() || !remainder.first().isLetter()) {
                return entry.bookId to remainder
            }
        }
        return null
    }

    private fun resolveNumbered(prefix: Int, matchedBookId: Int): Int? {
        val matched = BibleCatalog.book(matchedBookId) ?: return null
        val prefixStr = prefix.toString()
        val baseKo = matched.nameKo.removePrefix("1 ").removePrefix("2 ").removePrefix("3 ")
        return BibleCatalog.books.find { book ->
            (book.nameKo.startsWith(prefixStr) || book.nameEn.startsWith("$prefixStr ")) &&
                (
                    book.nameKo.endsWith(baseKo) ||
                        book.nameKo.contains(baseKo.take(2)) ||
                        book.nameEn.lowercase().contains(matched.nameEn.split(' ').last().lowercase())
                    )
        }?.id
    }

    @Suppress("LongMethod")
    private fun manualEntries(): List<Pair<String, Int>> = listOf(
        "창세" to 1, "창" to 1,
        "출" to 2, "레" to 3, "민" to 4, "신" to 5,
        "수" to 6, "삿" to 7, "룻" to 8,
        "삼상" to 9, "1삼상" to 9,
        "삼하" to 10, "2삼하" to 10,
        "왕상" to 11, "1왕상" to 11,
        "왕하" to 12, "2왕하" to 12,
        "대상" to 13, "1대상" to 13,
        "대하" to 14, "2대하" to 14,
        "스" to 15, "느" to 16, "에" to 17, "욥" to 18,
        "시" to 19, "시편" to 19,
        "잠" to 20, "전" to 21, "아" to 22, "아가" to 22,
        "사" to 23, "렘" to 24, "애" to 25, "애가" to 25,
        "겔" to 26, "단" to 27,
        "호" to 28, "욜" to 29, "암" to 30, "옵" to 31,
        "욘" to 32, "미" to 33, "나" to 34, "합" to 35,
        "습" to 36, "학" to 37, "슼" to 38, "말" to 39,
        "마" to 40, "마태" to 40,
        "막" to 41, "마가" to 41,
        "눅" to 42, "누가" to 42,
        "요" to 43, "요한" to 43,
        "행" to 44, "롬" to 45,
        "고전" to 46, "1고전" to 46, "1코" to 46,
        "고후" to 47, "2고후" to 47, "2코" to 47,
        "갈" to 48, "엡" to 49, "빌" to 50, "골" to 51,
        "살전" to 52, "1살전" to 52,
        "살후" to 53, "2살후" to 53,
        "딤전" to 54, "1딤전" to 54,
        "딤후" to 55, "2딤후" to 55,
        "딛" to 56, "몬" to 57, "히" to 58, "약" to 59,
        "벧전" to 60, "1벧전" to 60,
        "벧후" to 61, "2벧후" to 61,
        "요일" to 62, "1요일" to 62,
        "요이" to 63, "2요이" to 63,
        "요삼" to 64, "3요삼" to 64,
        "유" to 65, "계" to 66,
        "gen" to 1, "ge" to 1, "exo" to 2, "ex" to 2,
        "lev" to 3, "num" to 4, "deu" to 5, "dt" to 5,
        "jos" to 6, "josh" to 6, "jdg" to 7, "judg" to 7,
        "rut" to 8, "1sa" to 9, "1sam" to 9,
        "2sa" to 10, "2sam" to 10,
        "1ki" to 11, "1kings" to 11, "2ki" to 12, "2kings" to 12,
        "1ch" to 13, "1chr" to 13, "2ch" to 14, "2chr" to 14,
        "ezr" to 15, "ezra" to 15, "neh" to 16, "est" to 17,
        "job" to 18, "psa" to 19, "ps" to 19, "psalm" to 19,
        "pro" to 20, "prov" to 20, "ecc" to 21, "eccl" to 21,
        "sng" to 22, "song" to 22, "isa" to 23, "is" to 23,
        "jer" to 24, "lam" to 25, "eze" to 26, "ezk" to 26,
        "dan" to 27, "hos" to 28, "joe" to 29, "amo" to 30,
        "oba" to 31, "jon" to 32, "mic" to 33, "nah" to 34,
        "hab" to 35, "zep" to 36, "hag" to 37, "zec" to 38,
        "mal" to 39, "mat" to 40, "matt" to 40, "mt" to 40,
        "mrk" to 41, "mk" to 41, "luk" to 42, "lk" to 42,
        "jhn" to 43, "jn" to 43, "john" to 43,
        "act" to 44, "ac" to 44, "rom" to 45, "ro" to 45,
        "1co" to 46, "1cor" to 46, "2co" to 47, "2cor" to 47,
        "gal" to 48, "ga" to 48, "eph" to 49, "ep" to 49,
        "php" to 50, "phil" to 50, "col" to 51, "co" to 51,
        "1th" to 52, "1thess" to 52, "2th" to 53, "2thess" to 53,
        "1ti" to 54, "1tim" to 54, "2ti" to 55, "2tim" to 55,
        "tit" to 56, "ti" to 56, "phm" to 57,
        "heb" to 58, "he" to 58, "jas" to 59, "jam" to 59,
        "1pe" to 60, "1pet" to 60, "2pe" to 61, "2pet" to 61,
        "1jn" to 62, "1john" to 62, "2jn" to 63, "2john" to 63,
        "3jn" to 64, "3john" to 64, "jud" to 65, "jude" to 65,
        "rev" to 66, "re" to 66, "revelation" to 66,
    )
}
