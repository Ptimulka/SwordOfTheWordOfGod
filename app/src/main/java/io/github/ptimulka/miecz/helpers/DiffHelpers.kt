package io.github.ptimulka.miecz.helpers

enum class DiffType { INSERT, DELETE, EQUAL }
data class DiffPart(val type: DiffType, val text: String)

/**
 * Computes the word-level differences between two strings using the Longest Common Subsequence (LCS) algorithm.
 * Returns a list of [DiffPart]s representing words that were added, removed, or remained equal.
 */
fun diffWords(original: String, revised: String): List<DiffPart> {
    val originalWords = original.split(' ').filter { it.isNotEmpty() }
    val revisedWords = revised.split(' ').filter { it.isNotEmpty() }
    val n = originalWords.size
    val m = revisedWords.size

    val lcs = Array(n + 1) { IntArray(m + 1) }
    for (i in 0..n) {
        for (j in 0..m) {
            if (i == 0 || j == 0) {
                lcs[i][j] = 0
            } else if (originalWords[i - 1] == revisedWords[j - 1]) {
                lcs[i][j] = lcs[i - 1][j - 1] + 1
            } else {
                lcs[i][j] = maxOf(lcs[i - 1][j], lcs[i][j - 1])
            }
        }
    }

    val diffs = mutableListOf<DiffPart>()
    var i = n
    var j = m
    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && originalWords[i - 1] == revisedWords[j - 1]) {
            diffs.add(DiffPart(DiffType.EQUAL, originalWords[i - 1]))
            i--
            j--
        } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
            diffs.add(DiffPart(DiffType.INSERT, revisedWords[j - 1]))
            j--
        } else if (i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j])) {
            diffs.add(DiffPart(DiffType.DELETE, originalWords[i - 1]))
            i--
        }
    }
    return diffs.reversed()
}
