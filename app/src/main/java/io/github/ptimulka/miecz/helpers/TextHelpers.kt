package io.github.ptimulka.miecz.helpers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

fun formatTime(ms: Long): String {
    val totalSeconds = (ms + 500) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "%d:%02d".format(minutes, seconds)
    else "${seconds}s"
}

fun normalizeVerseText(text: String): String =
    foldPolishChars(text.replace(Regex("[^A-Za-z0-9ĄąĆćĘęŁłŃńÓóŚśŹźŻż ]"), "").trim().lowercase())

/** Replaces Polish diacritics with their base Latin letters (ś→s, ł→l, …), so answers can be compared accent-insensitively. */
fun foldPolishChars(text: String): String {
    val from = "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ"
    val to = "acelnoszzACELNOSZZ"
    val sb = StringBuilder(text.length)
    for (c in text) {
        val idx = from.indexOf(c)
        sb.append(if (idx >= 0) to[idx] else c)
    }
    return sb.toString()
}

fun calculateWordSimilarity(list1: List<String>, list2: List<String>): Float {
    if (list1.isEmpty() && list2.isEmpty()) return 100f
    if (list1.isEmpty() || list2.isEmpty()) return 0f
    val n = list1.size
    val m = list2.size
    val lcs = Array(n + 1) { IntArray(m + 1) }
    for (i in 1..n) for (j in 1..m) {
        lcs[i][j] = if (list1[i - 1] == list2[j - 1]) lcs[i - 1][j - 1] + 1
                    else maxOf(lcs[i - 1][j], lcs[i][j - 1])
    }
    return (2f * lcs[n][m]) / (n + m) * 100f
}

fun buildAnnotatedVerseText(verseText: String) = buildAnnotatedString {
    val cleanText = verseText.replace("_", " ")
    val parts = cleanText.split("*")
    parts.forEachIndexed { i, part ->
        if (i % 2 == 0) append(part)
        else withStyle(style = SpanStyle(color = Color.Gray)) { append(part) }
    }
}