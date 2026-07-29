package io.github.ptimulka.miecz.components.game

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository

/**
 * Loads the active mnemonic picture for a verse (user-drawn/imported, falling back to the
 * bundled default), or null when there is none. Shared by the riddle screens so the
 * null-guard and repository plumbing live in one place.
 */
@Composable
fun rememberMnemonicPicture(
    sectionId: Int,
    verseIndex: Int,
    assetName: String? = null
): Bitmap? {
    val context = LocalContext.current
    return remember(sectionId, verseIndex) {
        if (assetName != null || (sectionId > 0 && verseIndex >= 0))
            MnemonicPicturesRepository(context).loadActivePicture(sectionId, verseIndex, assetName)
        else null
    }
}
