package io.github.ptimulka.miecz.helpers

import io.github.ptimulka.miecz.data.Verse

fun parseVerse(line: String): Verse? {
    if (line.isBlank()) return null

    val parts = line.split(";")
    return if (parts.size >= 4) {
        val book = parts[0].trim()
        val chapter = parts[1].trim().toIntOrNull() ?: 0
        val number = parts[2].trim()
        // The text might contain semicolons, so we join the rest back
        val text = parts.drop(3).joinToString(";").trim()
        Verse(book, chapter, number, text)
    } else {
        null
    }
}
