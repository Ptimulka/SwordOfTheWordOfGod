package io.github.ptimulka.miecz.helpers

import android.content.Context
import android.content.Intent
import io.github.ptimulka.miecz.GameActivity
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Section

fun launchGame(
    context: Context,
    section: Section,
    riddleTypes: List<RiddleType>,
    levelNumber: Int = 0
) {
    val intent = Intent(context, GameActivity::class.java).apply {
        putStringArrayListExtra(GameActivity.ARG_LEVEL_RIDDLE_TYPES, ArrayList(riddleTypes.map { it.name }))
        putExtra(GameActivity.ARG_SECTION_ID, section.id)
        putExtra(GameActivity.ARG_SECTION_NAME, section.name)
        putExtra(GameActivity.ARG_LEVEL_NUMBER, levelNumber)
        putParcelableArrayListExtra(GameActivity.ARG_SECTION_VERSES, ArrayList(section.verses))
        putStringArrayListExtra(GameActivity.ARG_ASSET_NAMES, ArrayList(section.assetNames))
    }
    context.startActivity(intent)
}
