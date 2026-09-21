package io.github.ptimulka.miecz.screens.mnemonic

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import io.github.ptimulka.miecz.repositories.ChosenPicture

@Composable
fun MnemonicPicturesListScreen(
    state: MnemonicUiState,
    listState: LazyListState,
    onEvent: (MnemonicEvent) -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onEvent(MnemonicEvent.DownloadAll) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = colorResource(id = R.color.game_button_yellow_dark),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onEvent(MnemonicEvent.BackFromList) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                    Text(
                        text = stringResource(R.string.mnemonic_pictures_title, state.sectionName),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                item {
                    Text(
                        text = stringResource(R.string.mnemonic_pictures_info),
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                    HorizontalDivider()
                }
                itemsIndexed(
                    items = state.verseData,
                    key = { _, data -> "${data.verse.book}_${data.verse.chapter}_${data.verse.number}" }
                ) { index, data ->
                    val verse = data.verse
                    val sigla = "${verse.book} ${verse.chapter},${verse.number}"
                    val effectiveChosen = data.effectiveChosen

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(text = sigla, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = buildAnnotatedVerseText(verse.text),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PictureFrame(
                                label = stringResource(R.string.picture_none_caption),
                                isChosen = effectiveChosen == ChosenPicture.NONE,
                                modifier = Modifier.weight(1f),
                                onClick = { onEvent(MnemonicEvent.SelectChoice(index, ChosenPicture.NONE)) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "—", fontSize = 24.sp, color = Color.Gray)
                                }
                            }

                            if (data.defaultBitmap != null) {
                                PictureFrame(
                                    label = stringResource(R.string.picture_chosen_default),
                                    isChosen = effectiveChosen == ChosenPicture.DEFAULT,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onEvent(MnemonicEvent.SelectChoice(index, ChosenPicture.DEFAULT)) }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Image(
                                            bitmap = data.defaultBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(28.dp)
                                                .background(colorResource(R.color.game_button_yellow_dark), CircleShape)
                                                .clickable { onEvent(MnemonicEvent.ShowZoom(data.defaultBitmap)) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            if (data.userBitmap != null) {
                                PictureFrame(
                                    label = stringResource(R.string.picture_chosen_user),
                                    isChosen = effectiveChosen == ChosenPicture.USER,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onEvent(MnemonicEvent.SelectChoice(index, ChosenPicture.USER)) }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Image(
                                            bitmap = data.userBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(28.dp)
                                                .background(colorResource(R.color.game_button_yellow_dark), CircleShape)
                                                .clickable { onEvent(MnemonicEvent.EnterDrawing(index)) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.Create, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            } else {
                                DrawFrame(
                                    modifier = Modifier.weight(1f),
                                    onClick = { onEvent(MnemonicEvent.EnterDrawing(index)) }
                                )
                            }

                            if (data.importedBitmap != null) {
                                PictureFrame(
                                    label = stringResource(R.string.picture_chosen_imported),
                                    isChosen = effectiveChosen == ChosenPicture.IMPORTED,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onEvent(MnemonicEvent.SelectChoice(index, ChosenPicture.IMPORTED)) }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Image(
                                            bitmap = data.importedBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(28.dp)
                                                .background(colorResource(R.color.game_button_yellow_dark), CircleShape)
                                                .clickable { onEvent(MnemonicEvent.EnterImport(index)) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.Create, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            } else {
                                DrawFrame(
                                    label = stringResource(R.string.picture_import_caption),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onEvent(MnemonicEvent.EnterImport(index)) }
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }

                item {
                    DownloadButton(
                        isDownloading = state.isDownloading,
                        message = state.downloadMessage,
                        onClick = {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {
                                onEvent(MnemonicEvent.DownloadAll)
                            }
                        }
                    )
                }
            }
        }

        ZoomOverlay(
            bitmap = state.zoomedBitmap,
            onDismiss = { onEvent(MnemonicEvent.ShowZoom(null)) }
        )
    }
}

@Composable
private fun DownloadButton(
    isDownloading: Boolean,
    message: MnemonicMessage?,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (isDownloading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .size(20.dp)
                    .align(Alignment.CenterHorizontally),
                strokeWidth = 2.dp,
                color = colorResource(R.color.game_button_yellow_dark)
            )
        } else {
            message?.let { msg ->
                val resolvedMessage = if (msg.formatArgs.isEmpty()) {
                    stringResource(id = msg.resId)
                } else {
                    stringResource(id = msg.resId, *msg.formatArgs.toTypedArray())
                }
                Text(
                    text = resolvedMessage,
                    fontSize = 12.sp,
                    color = colorResource(R.color.game_button_yellow_dark),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        Button(
            onClick = onClick,
            enabled = !isDownloading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.game_button_yellow_dark))
        ) {
            Text(text = stringResource(R.string.download_all_images), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ZoomOverlay(
    bitmap: Bitmap?,
    onDismiss: () -> Unit
) {
    bitmap?.let { b ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f)
            )
        }
    }
}

@Composable
private fun PictureFrame(
    label: String,
    isChosen: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val accentColor = colorResource(R.color.game_button_yellow_dark)
    val borderColor = if (isChosen) accentColor else Color.LightGray
    val borderWidth = if (isChosen) 2.dp else 1.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(6.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
        ) {
            content()
            if (isChosen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(18.dp)
                        .background(accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isChosen) accentColor else Color.Gray,
            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DrawFrame(
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                .background(Color(0xFFF8F8F8))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", fontSize = 28.sp, color = Color.LightGray)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label ?: stringResource(R.string.picture_draw_caption),
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
