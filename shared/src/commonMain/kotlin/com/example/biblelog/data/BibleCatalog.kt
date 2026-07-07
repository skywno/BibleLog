package com.example.biblelog.data

import com.example.biblelog.domain.model.BibleBook
import com.example.biblelog.domain.model.Testament

object BibleCatalog {
    val books: List<BibleBook> = listOf(
        // 구약 (39권)
        BibleBook(1, "창세기", "Genesis", Testament.OLD, 50, 1533),
        BibleBook(2, "출애굽기", "Exodus", Testament.OLD, 40, 1213),
        BibleBook(3, "레위기", "Leviticus", Testament.OLD, 27, 859),
        BibleBook(4, "민수기", "Numbers", Testament.OLD, 36, 1288),
        BibleBook(5, "신명기", "Deuteronomy", Testament.OLD, 34, 959),
        BibleBook(6, "여호수아", "Joshua", Testament.OLD, 24, 658),
        BibleBook(7, "사사기", "Judges", Testament.OLD, 21, 618),
        BibleBook(8, "룻기", "Ruth", Testament.OLD, 4, 85),
        BibleBook(9, "사무엘상", "1 Samuel", Testament.OLD, 31, 810),
        BibleBook(10, "사무엘하", "2 Samuel", Testament.OLD, 24, 695),
        BibleBook(11, "열왕기상", "1 Kings", Testament.OLD, 22, 816),
        BibleBook(12, "열왕기하", "2 Kings", Testament.OLD, 25, 719),
        BibleBook(13, "역대상", "1 Chronicles", Testament.OLD, 29, 942),
        BibleBook(14, "역대하", "2 Chronicles", Testament.OLD, 36, 822),
        BibleBook(15, "에스라", "Ezra", Testament.OLD, 10, 280),
        BibleBook(16, "느헤미야", "Nehemiah", Testament.OLD, 13, 406),
        BibleBook(17, "에스더", "Esther", Testament.OLD, 10, 167),
        BibleBook(18, "욥기", "Job", Testament.OLD, 42, 1070),
        BibleBook(19, "시편", "Psalms", Testament.OLD, 150, 2461),
        BibleBook(20, "잠언", "Proverbs", Testament.OLD, 31, 915),
        BibleBook(21, "전도서", "Ecclesiastes", Testament.OLD, 12, 222),
        BibleBook(22, "아가", "Song of Solomon", Testament.OLD, 8, 117),
        BibleBook(23, "이사야", "Isaiah", Testament.OLD, 66, 1292),
        BibleBook(24, "예레미야", "Jeremiah", Testament.OLD, 52, 1364),
        BibleBook(25, "예레미야애가", "Lamentations", Testament.OLD, 5, 154),
        BibleBook(26, "에스겔", "Ezekiel", Testament.OLD, 48, 1273),
        BibleBook(27, "다니엘", "Daniel", Testament.OLD, 12, 357),
        BibleBook(28, "호세아", "Hosea", Testament.OLD, 14, 197),
        BibleBook(29, "요엘", "Joel", Testament.OLD, 3, 73),
        BibleBook(30, "아모스", "Amos", Testament.OLD, 9, 146),
        BibleBook(31, "오바댜", "Obadiah", Testament.OLD, 1, 21),
        BibleBook(32, "요나", "Jonah", Testament.OLD, 4, 48),
        BibleBook(33, "미가", "Micah", Testament.OLD, 7, 105),
        BibleBook(34, "나훔", "Nahum", Testament.OLD, 3, 47),
        BibleBook(35, "하박국", "Habakkuk", Testament.OLD, 3, 56),
        BibleBook(36, "스바냐", "Zephaniah", Testament.OLD, 3, 53),
        BibleBook(37, "학개", "Haggai", Testament.OLD, 2, 38),
        BibleBook(38, "스가랴", "Zechariah", Testament.OLD, 14, 211),
        BibleBook(39, "말라기", "Malachi", Testament.OLD, 4, 55),
        // 신약 (27권)
        BibleBook(40, "마태복음", "Matthew", Testament.NEW, 28, 1071),
        BibleBook(41, "마가복음", "Mark", Testament.NEW, 16, 678),
        BibleBook(42, "누가복음", "Luke", Testament.NEW, 24, 1151),
        BibleBook(43, "요한복음", "John", Testament.NEW, 21, 879),
        BibleBook(44, "사도행전", "Acts", Testament.NEW, 28, 1007),
        BibleBook(45, "로마서", "Romans", Testament.NEW, 16, 433),
        BibleBook(46, "고린도전서", "1 Corinthians", Testament.NEW, 16, 437),
        BibleBook(47, "고린도후서", "2 Corinthians", Testament.NEW, 13, 257),
        BibleBook(48, "갈라디아서", "Galatians", Testament.NEW, 6, 149),
        BibleBook(49, "에베소서", "Ephesians", Testament.NEW, 6, 155),
        BibleBook(50, "빌립보서", "Philippians", Testament.NEW, 4, 104),
        BibleBook(51, "골로새서", "Colossians", Testament.NEW, 4, 95),
        BibleBook(52, "데살로니가전서", "1 Thessalonians", Testament.NEW, 5, 89),
        BibleBook(53, "데살로니가후서", "2 Thessalonians", Testament.NEW, 3, 47),
        BibleBook(54, "디모데전서", "1 Timothy", Testament.NEW, 6, 113),
        BibleBook(55, "디모데후서", "2 Timothy", Testament.NEW, 4, 83),
        BibleBook(56, "디도서", "Titus", Testament.NEW, 3, 46),
        BibleBook(57, "빌레몬서", "Philemon", Testament.NEW, 1, 25),
        BibleBook(58, "히브리서", "Hebrews", Testament.NEW, 13, 303),
        BibleBook(59, "야고보서", "James", Testament.NEW, 5, 108),
        BibleBook(60, "베드로전서", "1 Peter", Testament.NEW, 5, 105),
        BibleBook(61, "베드로후서", "2 Peter", Testament.NEW, 3, 61),
        BibleBook(62, "요한일서", "1 John", Testament.NEW, 5, 105),
        BibleBook(63, "요한이서", "2 John", Testament.NEW, 1, 13),
        BibleBook(64, "요한삼서", "3 John", Testament.NEW, 1, 15),
        BibleBook(65, "유다서", "Jude", Testament.NEW, 1, 25),
        BibleBook(66, "요한계시록", "Revelation", Testament.NEW, 22, 404),
    )

    val bookMap: Map<Int, BibleBook> = books.associateBy { it.id }

    val totalVerses: Int = books.sumOf { it.totalVerses }

    val oldTestamentVerses: Int = books.filter { it.testament == Testament.OLD }.sumOf { it.totalVerses }

    val newTestamentVerses: Int = books.filter { it.testament == Testament.NEW }.sumOf { it.totalVerses }
}
