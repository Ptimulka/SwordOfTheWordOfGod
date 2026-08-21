package io.github.ptimulka.miecz.repositories.content

import android.content.Context
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Section
import io.github.ptimulka.miecz.helpers.parseVerse
import io.github.ptimulka.miecz.repositories.SectionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class UserSectionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SectionRepository {

    override suspend fun loadSection(resourceId: Int): Section? = withContext(ioDispatcher) {
        val inputStream = context.resources.openRawResource(resourceId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        try {
            val lines = reader.readLines()
            if (lines.size < 2) return@withContext null

            val id = lines[0].trim().toIntOrNull() ?: return@withContext null
            val name = lines[1].trim()
            val verses = lines.drop(2).mapNotNull { parseVerse(it) }
            
            val assetNames = verses.mapIndexed { index, verse ->
                "section%02d_%02d_%s%d-%s.webp".format(
                    id, index + 1, verse.book, verse.chapter,
                    verse.number.replace(".", "-")
                )
            }
            Section(id, name, verses, assetNames)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            reader.close()
        }
    }

    override suspend fun loadInitialSections(): List<Section> = withContext(ioDispatcher) {
        listOfNotNull(
            loadSection(R.raw.section01),
            loadSection(R.raw.section02),
            loadSection(R.raw.section03),
            loadSection(R.raw.section04)
        )
    }

    override suspend fun loadSectionName(sectionId: Int): String? = withContext(ioDispatcher) {
        val rawId = when (sectionId) {
            1 -> R.raw.section01
            2 -> R.raw.section02
            3 -> R.raw.section03
            4 -> R.raw.section04
            else -> return@withContext null
        }
        loadSection(rawId)?.name
    }
}
