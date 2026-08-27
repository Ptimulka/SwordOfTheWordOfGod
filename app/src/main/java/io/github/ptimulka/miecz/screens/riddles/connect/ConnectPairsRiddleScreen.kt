package io.github.ptimulka.miecz.screens.riddles.connect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.FullscreenImageOverlay
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.screens.riddles.base.RiddleEffect

@Composable
fun ConnectPairsRiddleScreen(
    sectionVerses: List<Verse>,
    sectionId: Int,
    riddleIndex: Int = 0,
    assetNames: List<String> = emptyList(),
    onSuccess: (elapsedMs: Long) -> Unit
) {
    val vm: ConnectPairsViewModel = hiltViewModel<ConnectPairsViewModel, ConnectPairsViewModel.Factory>(
        key = "ConnectPairsVM_${sectionId}_${riddleIndex}"
    ) { factory ->
        factory.create(sectionVerses, sectionId, assetNames)
    }

    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(vm.effects) {
        vm.effects.collect { effect ->
            when (effect) {
                is RiddleEffect.Success -> onSuccess(effect.elapsedMs ?: 0L)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ConnectLayout(
            captionRes = R.string.connect_pairs_caption,
            state = state,
            onEvent = vm::onEvent,
            rightColumnWeight = 3f,
            showImagesOnButtons = true
        )

        state.previewBitmap?.let { preview ->
            FullscreenImageOverlay(preview) {
                vm.onEvent(ConnectEvent.DismissHint)
            }
        }
    }
}
