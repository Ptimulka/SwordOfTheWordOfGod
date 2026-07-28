package io.github.ptimulka.miecz.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import io.github.ptimulka.miecz.repositories.ChosenPicture
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository

@Composable
fun MnemonicPicturesListScreen(
    sectionId: Int,
    sectionName: String,
    verses: List<Verse>,
    assetNames: List<String>,
    repository: MnemonicPicturesRepository,
    onDrawVerse: (verseIndex: Int) -> Unit,
    onImportImage: (verseIndex: Int) -> Unit,
    onBack: () -> Unit
) {
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var zoomedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun doDownload() {
        scope.launch {
            isDownloading = true
            downloadMessage = null
            val count = withContext(Dispatchers.IO) {
                repository.downloadAllImages(sectionId, verses, assetNames)
            }
            isDownloading = false
            downloadMessage = if (count == 0)
                context.getString(R.string.download_images_none)
            else
                context.getString(R.string.download_images_done, count)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) doDownload() }

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
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    text = stringResource(R.string.mnemonic_pictures_title, sectionName),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
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
            itemsIndexed(verses) { index, verse ->
                val sigla = "${verse.book} ${verse.chapter},${verse.number}"

                val defaultAssetName = assetNames.getOrNull(index) ?: ""

                val userBitmap = remember(refreshTrigger, index) {
                    repository.loadPicture(sectionId, index)
                }
                val importedBitmap = remember(refreshTrigger, index) {
                    repository.loadImportedPicture(sectionId, index)
                }
                val defaultBitmap = remember(defaultAssetName) {
                    repository.loadDefaultPicture(defaultAssetName)
                }
                val chosen = remember(refreshTrigger, index) {
                    repository.loadChoice(sectionId, index)
                }
                // Resolve what to visually highlight, handling missing images gracefully
                val effectiveChosen = when (chosen) {
                    // Never saved → auto-select best available
                    null -> when {
                        defaultBitmap != null -> ChosenPicture.DEFAULT
                        importedBitmap != null -> ChosenPicture.IMPORTED
                        userBitmap != null -> ChosenPicture.USER
                        else -> ChosenPicture.NONE
                    }
                    // Saved DEFAULT but image no longer exists → fall back
                    ChosenPicture.DEFAULT -> if (defaultBitmap != null) ChosenPicture.DEFAULT
                        else if (importedBitmap != null) ChosenPicture.IMPORTED
                        else if (userBitmap != null) ChosenPicture.USER
                        else ChosenPicture.NONE
                    // Saved IMPORTED but image deleted → fall back
                    ChosenPicture.IMPORTED -> if (importedBitmap != null) ChosenPicture.IMPORTED
                        else if (userBitmap != null) ChosenPicture.USER
                        else if (defaultBitmap != null) ChosenPicture.DEFAULT
                        else ChosenPicture.NONE
                    // Saved USER but drawing deleted → fall back
                    ChosenPicture.USER -> if (userBitmap != null) ChosenPicture.USER
                        else if (importedBitmap != null) ChosenPicture.IMPORTED
                        else if (defaultBitmap != null) ChosenPicture.DEFAULT
                        else ChosenPicture.NONE
                    // Explicit NONE always respected
                    ChosenPicture.NONE -> ChosenPicture.NONE
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // Sigla + verse text
                    Text(text = sigla, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildAnnotatedVerseText(verse.text),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Picture frames row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Frame 1 — "Brak" (no picture)
                        PictureFrame(
                            label = stringResource(R.string.picture_none_caption),
                            isChosen = effectiveChosen == ChosenPicture.NONE,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                repository.saveChoice(sectionId, index, ChosenPicture.NONE)
                                refreshTrigger++
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFEEEEEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "—",
                                    fontSize = 24.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Frame 2 — Default AI image (only if asset exists)
                        if (defaultBitmap != null) {
                            PictureFrame(
                                label = stringResource(R.string.picture_chosen_default),
                                isChosen = effectiveChosen == ChosenPicture.DEFAULT,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    repository.saveChoice(sectionId, index, ChosenPicture.DEFAULT)
                                    refreshTrigger++
                                }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = defaultBitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Zoom button — same style as pencil on user image
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .size(28.dp)
                                            .background(
                                                colorResource(R.color.game_button_yellow_dark),
                                                CircleShape
                                            )
                                            .clickable { zoomedBitmap = defaultBitmap },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Frame 3 — User picture (if exists) or draw button
                        if (userBitmap != null) {
                            PictureFrame(
                                label = stringResource(R.string.picture_chosen_user),
                                isChosen = effectiveChosen == ChosenPicture.USER,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    repository.saveChoice(sectionId, index, ChosenPicture.USER)
                                    refreshTrigger++
                                }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = userBitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Pencil overlay — opens drawing editor
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .size(28.dp)
                                            .background(
                                                colorResource(R.color.game_button_yellow_dark),
                                                CircleShape
                                            )
                                            .clickable { onDrawVerse(index) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Create,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Draw button frame
                            DrawFrame(
                                modifier = Modifier.weight(1f),
                                onClick = { onDrawVerse(index) }
                            )
                        }

                        // Frame 4 — Imported picture (if exists) or import button
                        if (importedBitmap != null) {
                            PictureFrame(
                                label = stringResource(R.string.picture_chosen_imported),
                                isChosen = effectiveChosen == ChosenPicture.IMPORTED,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    repository.saveChoice(sectionId, index, ChosenPicture.IMPORTED)
                                    refreshTrigger++
                                }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = importedBitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Re-import button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .size(28.dp)
                                            .background(
                                                colorResource(R.color.game_button_yellow_dark),
                                                CircleShape
                                            )
                                            .clickable { onImportImage(index) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Create,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Import button frame
                            DrawFrame(
                                label = stringResource(R.string.picture_import_caption),
                                modifier = Modifier.weight(1f),
                                onClick = { onImportImage(index) }
                            )
                        }
                    }
                }

                HorizontalDivider()
            }
        }

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
            downloadMessage?.let { msg ->
                Text(
                    text = msg,
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
            onClick = {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    doDownload()
                }
            },
            enabled = !isDownloading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.game_button_yellow_dark)
            )
        ) {
            Text(
                text = stringResource(R.string.download_all_images),
                fontWeight = FontWeight.Bold
            )
        }
    } // end Column

    // Fullscreen zoom overlay
    zoomedBitmap?.let { bitmap ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { zoomedBitmap = null },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f)
            )
        }
    }

    } // end Box
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
            // Chosen indicator checkmark badge
            if (isChosen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(18.dp)
                        .background(accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "+", fontSize = 28.sp, color = Color.LightGray)
            }
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
