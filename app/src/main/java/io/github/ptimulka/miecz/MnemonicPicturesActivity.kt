package io.github.ptimulka.miecz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.repositories.ChosenPicture
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import io.github.ptimulka.miecz.screens.DrawingScreen
import io.github.ptimulka.miecz.screens.ImageImportEditScreen
import io.github.ptimulka.miecz.screens.MnemonicPicturesListScreen
import io.github.ptimulka.miecz.ui.theme.SwordOfTheWordOfGodTheme

class MnemonicPicturesActivity : ComponentActivity() {

    companion object {
        private const val ARG_SECTION_ID = "arg_section_id"
        private const val ARG_SECTION_NAME = "arg_section_name"
        private const val ARG_VERSES = "arg_verses"
        private const val ARG_ASSET_NAMES = "arg_asset_names"

        fun createIntent(
            context: Context,
            sectionId: Int,
            sectionName: String,
            verses: ArrayList<Verse>,
            assetNames: ArrayList<String>
        ): Intent = Intent(context, MnemonicPicturesActivity::class.java).apply {
            putExtra(ARG_SECTION_ID, sectionId)
            putExtra(ARG_SECTION_NAME, sectionName)
            putParcelableArrayListExtra(ARG_VERSES, verses)
            putStringArrayListExtra(ARG_ASSET_NAMES, assetNames)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val sectionId = intent.getIntExtra(ARG_SECTION_ID, 0)
        val sectionName = intent.getStringExtra(ARG_SECTION_NAME) ?: ""
        val verses = intent.getParcelableArrayListExtra<Verse>(ARG_VERSES) ?: arrayListOf()
        val assetNames = intent.getStringArrayListExtra(ARG_ASSET_NAMES) ?: arrayListOf()
        val repository = MnemonicPicturesRepository(this)

        setContent {
            SwordOfTheWordOfGodTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MnemonicPicturesHost(
                            sectionId = sectionId,
                            sectionName = sectionName,
                            verses = verses,
                            assetNames = assetNames,
                            repository = repository,
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }
}

private sealed interface MnemonicScreen {
    data object List : MnemonicScreen
    data class Drawing(val verseIndex: Int) : MnemonicScreen
    data class ImportImage(val verseIndex: Int) : MnemonicScreen
}

@Composable
private fun MnemonicPicturesHost(
    sectionId: Int,
    sectionName: String,
    verses: List<Verse>,
    assetNames: List<String>,
    repository: MnemonicPicturesRepository,
    onBack: () -> Unit
) {
    var screen by remember { mutableStateOf<MnemonicScreen>(MnemonicScreen.List) }

    when (val current = screen) {
        is MnemonicScreen.List -> MnemonicPicturesListScreen(
            sectionId = sectionId,
            sectionName = sectionName,
            verses = verses,
            assetNames = assetNames,
            repository = repository,
            onDrawVerse = { screen = MnemonicScreen.Drawing(it) },
            onImportImage = { screen = MnemonicScreen.ImportImage(it) },
            onBack = onBack
        )
        is MnemonicScreen.Drawing -> DrawingScreen(
            verse = verses[current.verseIndex],
            sectionId = sectionId,
            verseIndex = current.verseIndex,
            repository = repository,
            onBack = {
                // Auto-select user picture after drawing
                repository.saveChoice(sectionId, current.verseIndex, ChosenPicture.USER)
                screen = MnemonicScreen.List
            }
        )
        is MnemonicScreen.ImportImage -> ImageImportEditScreen(
            sectionId = sectionId,
            verseIndex = current.verseIndex,
            verse = verses[current.verseIndex],
            repository = repository,
            onSaved = {
                screen = MnemonicScreen.List
            },
            onCancel = {
                screen = MnemonicScreen.List
            }
        )
    }
}
