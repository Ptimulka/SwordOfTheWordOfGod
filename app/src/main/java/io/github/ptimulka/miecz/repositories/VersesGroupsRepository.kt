package io.github.ptimulka.miecz.repositories

import android.content.Context
import io.github.ptimulka.miecz.data.VerseGroup
import io.github.ptimulka.miecz.helpers.parseVerse
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

interface VersesGroupsRepository {
    fun loadVerseGroups(): List<VerseGroup>
}

class UserVersesGroupsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : VersesGroupsRepository {

    override fun loadVerseGroups(): List<VerseGroup> {
        val verseGroups = mutableListOf<VerseGroup>()
        try {
            val assetManager = context.assets
            val path = "verses/"
            val fileNames = assetManager.list(path)?.filter {
                it.endsWith(".txt") } ?: emptyList()

            for (fileName in fileNames) {
                // Extract ID from filename (e.g., group001-Miłość Boża.txt -> 1)
                val id = Regex("group(\\d+)-").find(fileName)?.groupValues?.get(1)?.toIntOrNull() ?: 0

                assetManager.open(path + fileName).use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val groupName = reader.readLine()
                    if (groupName != null) {
                        val verses = reader.readLines().mapNotNull { parseVerse(it) }
                        if (verses.isNotEmpty()) {
                            verseGroups.add(VerseGroup(id, groupName, verses))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return verseGroups.sortedBy { it.id }
    }
}
