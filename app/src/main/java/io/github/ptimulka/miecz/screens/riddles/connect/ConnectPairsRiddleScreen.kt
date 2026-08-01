package io.github.ptimulka.miecz.screens.riddles.connect

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.FullscreenImageOverlay
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository

@Composable
fun ConnectPairsRiddleScreen(
    sectionVerses: List<Verse>,
    sectionId: Int,
    assetNames: List<String> = emptyList(),
    onSuccess: (elapsedMs: Long) -> Unit
) {
    val context = LocalContext.current
    val hintBitmaps: Map<Int, Bitmap> = remember(sectionId, sectionVerses) {
        val repo = MnemonicPicturesRepository(context)
        sectionVerses.mapIndexed { index, verse ->
            val assetName = assetNames.getOrNull(index)
            verse.hashCode() to repo.loadActivePicture(sectionId, index, assetName)
        }.mapNotNull { (key, bitmap) -> bitmap?.let { key to it } }.toMap()
    }

    val vm: ConnectPairsViewModel = viewModel(
        key = "ConnectPairsVM_${sectionId}_${sectionVerses.hashCode()}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ConnectPairsViewModel(sectionVerses, hintBitmaps) as T
            }
        }
    )

    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(vm.effects) {
        vm.effects.collect { effect ->
            when (effect) {
                is ConnectEffect.Success -> onSuccess(effect.elapsedMs)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ConnectLayout(
            captionRes = R.string.connect_pairs_caption,
            state = state,
            onEvent = vm::onEvent,
            hintBitmaps = hintBitmaps,
            rightColumnWeight = 3f,
            showImagesOnButtons = true
        )

        state.previewBitmap?.let { preview ->
            FullscreenImageOverlay(preview) {
                vm.onEvent(ConnectEvent.DismissImage)
            }
        }
    }
}
