package io.github.ptimulka.miecz.screens.mnemonic

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText

@Composable
fun ImageImportEditScreen(
    state: MnemonicUiState,
    onEvent: (MnemonicEvent) -> Unit
) {
    val imp = state.importState
    val verseData = state.verseData[imp.verseIndex]
    val sigla = "${verseData.verse.book} ${verseData.verse.chapter},${verseData.verse.number}"
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    BackHandler {
        onEvent(MnemonicEvent.CancelImport)
    }

    if (imp.selectedBitmap != null) {
        if (!isLandscape) {
            Column(modifier = Modifier.fillMaxSize()) {
                ImportTopBar(sigla, verseData.verse.text) { onEvent(MnemonicEvent.CancelImport) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ImportCanvas(state, onEvent) { canvasSize = it }
                }

                ImportActions(
                    rotation = imp.rotation,
                    onRotate = { onEvent(MnemonicEvent.SetRotation(it)) },
                    onConfirm = { onEvent(MnemonicEvent.ConfirmImport(canvasSize)) }
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ImportCanvas(state, onEvent) { canvasSize = it }
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(8.dp)
                        .width(120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = colorResource(id = R.color.game_button_yellow_dark),
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onEvent(MnemonicEvent.CancelImport) }, modifier = Modifier.height(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.height(18.dp))
                            }
                            Text(text = sigla, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    Text(
                        text = buildAnnotatedVerseText(verseData.verse.text),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onEvent(MnemonicEvent.SetRotation((imp.rotation - 90f) % 360f)) },
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.game_button_yellow_dark)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.graphicsLayer { scaleX = -1f })
                        }
                        Button(
                            onClick = { onEvent(MnemonicEvent.SetRotation((imp.rotation + 90f) % 360f)) },
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.game_button_yellow_dark)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        }
                    }
                    Button(
                        onClick = { onEvent(MnemonicEvent.ConfirmImport(canvasSize)) },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.game_button_yellow_dark)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.image_import_save), color = Color.White)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ImportTopBar(sigla: String, verseText: String, onBack: () -> Unit) {
    Surface(
        color = colorResource(id = R.color.game_button_yellow_dark),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    text = sigla,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = buildAnnotatedVerseText(verseText),
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ImportCanvas(
    state: MnemonicUiState,
    onEvent: (MnemonicEvent) -> Unit,
    onSizeChanged: (IntSize) -> Unit
) {
    val imp = state.importState
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(8.dp))
            .onSizeChanged { onSizeChanged(it) }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, gestureRotation ->
                    onEvent(MnemonicEvent.UpdateTransform(pan, gestureZoom, gestureRotation))
                }
            }
    ) {
        imp.selectedBitmap?.let { bitmap ->
            drawRect(Color.White)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val imageBitmap = bitmap.asImageBitmap()

            withTransform({
                translate(centerX + imp.offsetX, centerY + imp.offsetY)
                rotate(imp.rotation, androidx.compose.ui.geometry.Offset(0f, 0f))
                scale(imp.scale, imp.scale, androidx.compose.ui.geometry.Offset(0f, 0f))
                translate(-bitmap.width / 2f, -bitmap.height / 2f)
            }) {
                drawImage(imageBitmap)
            }
        }
    }
}

@Composable
private fun ImportActions(
    rotation: Float,
    onRotate: (Float) -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onRotate((rotation - 90f) % 360f) },
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.game_button_yellow_dark)),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.graphicsLayer { scaleX = -1f })
        }
        Button(
            onClick = { onRotate((rotation + 90f) % 360f) },
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.game_button_yellow_dark)),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
        }
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.game_button_yellow_dark)),
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.image_import_save), color = Color.White)
        }
    }
}
