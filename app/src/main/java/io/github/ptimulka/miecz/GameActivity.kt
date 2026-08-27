package io.github.ptimulka.miecz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.screens.game.GameEffect
import io.github.ptimulka.miecz.screens.game.GameScreen
import io.github.ptimulka.miecz.screens.game.GameViewModel
import io.github.ptimulka.miecz.ui.theme.SwordOfTheWordOfGodTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameActivity : ComponentActivity() {

    companion object {
        const val ARG_LEVEL_RIDDLE_TYPES = "arg_level_riddle_types"
        const val ARG_SECTION_ID = "arg_section_id"
        const val ARG_SECTION_NAME = "arg_section_name"
        const val ARG_LEVEL_NUMBER = "arg_level_number"
        const val ARG_SECTION_VERSES = "arg_section_verses"
        const val ARG_ASSET_NAMES = "arg_asset_names"
        const val SECTION_ID_REPEAT_NORMAL = -1
        const val SECTION_ID_REPEAT_FOR_SHIELDS = -2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val levelRiddleTypes = intent.getStringArrayListExtra(ARG_LEVEL_RIDDLE_TYPES) ?: arrayListOf()
        val sectionId = intent.getIntExtra(ARG_SECTION_ID, 0)
        val sectionName = intent.getStringExtra(ARG_SECTION_NAME) ?: ""
        val levelNumber = intent.getIntExtra(ARG_LEVEL_NUMBER, 0)
        val sectionVerses = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(ARG_SECTION_VERSES, Verse::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(ARG_SECTION_VERSES)
        } ?: arrayListOf()
        val assetNames = intent.getStringArrayListExtra(ARG_ASSET_NAMES) ?: arrayListOf()

        // Hide system bars
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            SwordOfTheWordOfGodTheme {
                val vm: GameViewModel = hiltViewModel<GameViewModel, GameViewModel.Factory> { factory ->
                    factory.create(
                        sectionId = sectionId,
                        sectionName = sectionName,
                        levelNumber = levelNumber,
                        sectionVerses = sectionVerses,
                        assetNames = assetNames,
                        levelRiddleTypeNames = levelRiddleTypes
                    )
                }

                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(vm.effects) {
                    vm.effects.collect { effect ->
                        when (effect) {
                            GameEffect.FinishGame -> finish()
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        GameScreen(
                            state = state,
                            onEvent = vm::onEvent
                        )
                    }
                }
            }
        }
    }
}
