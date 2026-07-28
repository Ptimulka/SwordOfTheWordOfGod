package io.github.ptimulka.miecz.repositories

import android.content.Context
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.RiddleType
import java.io.BufferedReader
import java.io.InputStreamReader

class RiddlesOrderRepository(private val context: Context) {

    fun getRiddlesOrder(): List<List<RiddleType>> {
        val riddlesOrder = mutableListOf<List<RiddleType>>()
        try {
            val inputStream = context.resources.openRawResource(R.raw.riddles_order)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.forEachLine { line ->
                val riddleTypes = line.split(',').mapNotNull {
                    try {
                        RiddleType.valueOf(it.trim().uppercase())
                    } catch (e: IllegalArgumentException) {
                        null // Ignore invalid or misspelled enum names
                    }
                }
                if (riddleTypes.isNotEmpty()) {
                    riddlesOrder.add(riddleTypes)
                }
            }
            reader.close()
        } catch (e: Exception) {
            // If the file can't be read, return a default order to prevent crashes
            e.printStackTrace()
            return getDefaultOrder()
        }
        return riddlesOrder
    }

    private fun getDefaultOrder(): List<List<RiddleType>> {
        // This default order will be used if riddles_order.txt is missing or invalid.
        return listOf(
            listOf(
                RiddleType.QUIZ_EASY,
                RiddleType.FILL_WORDS_EASY,
                RiddleType.WORD_SCRAMBLE_EASY,
                RiddleType.MULTI_QUIZ,
                RiddleType.FILL_SIGLA_BOOK,
                RiddleType.FILL_MORE_WORDS_EASY,
                RiddleType.FILL_WORDS_NORMAL,
                RiddleType.FILL_WORDS_NORMAL,
                RiddleType.QUIZ_NORMAL,
                RiddleType.WORD_SCRAMBLE_NORMAL,
            )
        )
    }
}
