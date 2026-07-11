package com.example.biblelog.data

data class ParsedBiblePoint(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
)

enum class ParseError {
    EmptyInput,
    UnknownBook,
    InvalidFormat,
    OutOfRange,
}

data class ParseResult(
    val point: ParsedBiblePoint? = null,
    val error: ParseError? = null,
)

object BibleReferenceParser {

    fun parse(input: String): ParseResult {
        val normalized = normalize(input)
        if (normalized.isEmpty()) {
            return ParseResult(error = ParseError.EmptyInput)
        }

        val (bookId, rest) = BibleBookAliases.matchBookToken(normalized)
            ?: return ParseResult(error = ParseError.UnknownBook)

        val (chapter, verse) = parseChapterVerse(rest.trim())
            ?: return ParseResult(error = ParseError.InvalidFormat)

        if (!BibleCatalog.isValidPoint(bookId, chapter, verse)) {
            return ParseResult(error = ParseError.OutOfRange)
        }

        return ParseResult(point = ParsedBiblePoint(bookId, chapter, verse))
    }

    fun parseOrNull(input: String): ParsedBiblePoint? = parse(input).point

    internal fun normalize(input: String): String {
        var text = input.trim()
        text = text.map { char ->
            when (char) {
                '０' -> '0'; '１' -> '1'; '２' -> '2'; '３' -> '3'; '４' -> '4'
                '５' -> '5'; '６' -> '6'; '７' -> '7'; '８' -> '8'; '９' -> '9'
                '：' -> ':'; '，' -> ','
                else -> char
            }
        }.joinToString("")
        text = text.replace(Regex("\\s+"), " ")
        text = commonAbbreviations(text)
        return text.trim()
    }

    private fun commonAbbreviations(text: String): String = text
        .replace("창세 ", "창세기 ", ignoreCase = true)
        .replace("창세기", "창세기", ignoreCase = true)

    internal fun parseChapterVerse(rest: String): Pair<Int, Int>? {
        if (rest.isEmpty()) return null
        var r = rest.trim()
        r = r.trimStart(' ', ':', ',', '.', '-')

        val patterns = listOf(
            Regex("""^(\d+)\s*:\s*(\d+)"""),
            Regex("""^(\d+)\s*장\s*(\d+)\s*절?"""),
            Regex("""^(\d+)장(\d+)절?"""),
            Regex("""^(\d+)장\s*(\d+)"""),
            Regex("""^(\d+)\s*장\s*(\d+)"""),
            Regex("""^(\d+)\s*,\s*(\d+)"""),
            Regex("""^(\d+)\s+(\d+)"""),
        )

        for (pattern in patterns) {
            val match = pattern.find(r) ?: continue
            val chapter = match.groupValues[1].toIntOrNull() ?: continue
            val verse = match.groupValues[2].toIntOrNull() ?: continue
            return chapter to verse
        }
        return null
    }
}
