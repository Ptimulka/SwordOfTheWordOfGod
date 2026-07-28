package io.github.ptimulka.miecz.repositories

import android.content.Context
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.helpers.parseVerse
import java.io.BufferedReader
import java.io.InputStreamReader

class SectionRepository(private val context: Context) {

    fun loadSection(resourceId: Int): Section? {
        val inputStream = context.resources.openRawResource(resourceId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        try {
            val lines = reader.readLines()
            if (lines.size < 2) return null

            // First line: Section ID
            val id = lines[0].trim().toIntOrNull() ?: return null
            
            // Second line: Section Name
            val name = lines[1].trim()

            // Remaining lines: Verses
            val verses = lines.drop(2).mapNotNull { parseVerse(it) }
            
            val assetNames = verses.mapIndexed { index, verse ->
                "section%02d_%02d_%s%d-%s.webp".format(
                    id, index + 1, verse.book, verse.chapter,
                    verse.number.replace(".", "-")
                )
            }
            return Section(id, name, verses, assetNames)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            reader.close()
        }
    }

    fun loadInitialSections(): List<Section> {
        return listOfNotNull(
            loadSection(R.raw.section01),
            loadSection(R.raw.section02),
            loadSection(R.raw.section03),
            loadSection(R.raw.section04)
        )
    }

    fun loadSectionName(sectionId: Int): String? {
        val rawId = when (sectionId) {
            1 -> R.raw.section01
            2 -> R.raw.section02
            3 -> R.raw.section03
            4 -> R.raw.section04
            else -> return null
        }
        return loadSection(rawId)?.name
    }
}
