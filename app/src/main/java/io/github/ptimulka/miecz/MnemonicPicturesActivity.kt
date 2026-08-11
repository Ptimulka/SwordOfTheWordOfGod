package io.github.ptimulka.miecz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.repositories.UserMnemonicPicturesRepository
import io.github.ptimulka.miecz.screens.mnemonic.DrawingScreen
import io.github.ptimulka.miecz.screens.mnemonic.ImageImportEditScreen
import io.github.ptimulka.miecz.screens.mnemonic.MnemonicEffect
import io.github.ptimulka.miecz.screens.mnemonic.MnemonicEvent
import io.github.ptimulka.miecz.screens.mnemonic.MnemonicPicturesListScreen
import io.github.ptimulka.miecz.screens.mnemonic.MnemonicScreen
import io.github.ptimulka.miecz.screens.mnemonic.MnemonicViewModel
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
        val verses = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(ARG_VERSES, Verse::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(ARG_VERSES)
        } ?: arrayListOf()
        val assetNames = intent.getStringArrayListExtra(ARG_ASSET_NAMES) ?: arrayListOf()

        setContent {
            SwordOfTheWordOfGodTheme {
                val context = LocalContext.current
                val vm: MnemonicViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return MnemonicViewModel(
                                sectionId, sectionName, verses, assetNames,
                                UserMnemonicPicturesRepository(context)
                            ) as T
                        }
                    }
                )

                val state by vm.state.collectAsStateWithLifecycle()

                val imageLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        vm.onEvent(MnemonicEvent.ImageSelected(uri), context)
                    } else {
                        vm.onEvent(MnemonicEvent.CancelImport)
                    }
                }

                LaunchedEffect(vm.effects) {
                    vm.effects.collect { effect ->
                        when (effect) {
                            MnemonicEffect.FinishActivity -> finish()
                            is MnemonicEffect.ShowToast -> {
                                val message = if (effect.message.formatArgs.isEmpty()) {
                                    context.getString(effect.message.resId)
                                } else {
                                    context.getString(effect.message.resId, *effect.message.formatArgs.toTypedArray())
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                            MnemonicEffect.LaunchImagePicker -> {
                                imageLauncher.launch("image/*")
                            }
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (state.currentScreen) {
                            MnemonicScreen.LIST -> MnemonicPicturesListScreen(state, vm::onEvent)
                            MnemonicScreen.DRAWING -> DrawingScreen(state, vm::onEvent)
                            MnemonicScreen.IMPORT -> ImageImportEditScreen(state, vm::onEvent)
                        }
                    }
                }
            }
        }
    }
}
