package io.github.ptimulka.miecz.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Refresh
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.fillMaxHeight
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText
import io.github.ptimulka.miecz.repositories.ChosenPicture
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageImportEditScreen(
    sectionId: Int,
    verseIndex: Int,
    verse: Verse,
    repository: MnemonicPicturesRepository,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sigla = "${verse.book} ${verse.chapter},${verse.number}"

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var displayCanvasWidth by remember { mutableFloatStateOf(0f) }
    var displayCanvasHeight by remember { mutableFloatStateOf(0f) }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bitmap: Bitmap? = withContext(Dispatchers.IO) {
                    loadBitmapFromUri(context, uri)
                }
                if (bitmap == null) {
                    // Decode failed (too large, corrupt, or unsupported) — show a message and exit
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.image_import_load_failed),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    onCancel()
                } else {
                    selectedBitmap = bitmap
                    offsetX = 0f
                    offsetY = 0f
                    scale = 1f
                    rotation = 0f
                }
            }
        } else {
            // User cancelled the image picker
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
        imageLauncher.launch("image/*")
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    BackHandler {
        onCancel()
    }

    if (selectedBitmap != null) {
        // Show image editor
        if (!isLandscape) {
            // Portrait mode
            Column(modifier = Modifier.fillMaxSize()) {
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
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
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
                            text = buildAnnotatedVerseText(verse.text),
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, gestureZoom, gestureRotation ->
                                    offsetX += pan.x
                                    offsetY += pan.y
                                    scale *= gestureZoom
                                    scale = scale.coerceIn(0.3f, 5f)
                                    rotation += gestureRotation
                                }
                            }
                    ) {
                        selectedBitmap?.let { bitmap ->
                            displayCanvasWidth = size.width
                            displayCanvasHeight = size.height
                            drawRect(Color.White)

                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val imageBitmap = bitmap.asImageBitmap()

                            withTransform({
                                translate(centerX + offsetX, centerY + offsetY)
                                rotate(rotation, androidx.compose.ui.geometry.Offset(0f, 0f))
                                scale(scale, scale, androidx.compose.ui.geometry.Offset(0f, 0f))
                                translate(-bitmap.width / 2f, -bitmap.height / 2f)
                            }) {
                                drawImage(imageBitmap)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { rotation = (rotation - 90f) % 360f },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.game_button_yellow_dark)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.graphicsLayer { scaleX = -1f }
                        )
                    }
                    Button(
                        onClick = { rotation = (rotation + 90f) % 360f },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.game_button_yellow_dark)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val croppedBitmap: Bitmap = withContext(Dispatchers.Default) {
                                    cropImageTo34Ratio(
                                        selectedBitmap!!,
                                        offsetX,
                                        offsetY,
                                        scale,
                                        rotation,
                                        displayCanvasWidth.toInt(),
                                        displayCanvasHeight.toInt()
                                    )
                                }
                                withContext(Dispatchers.IO) {
                                    repository.saveImportedPicture(sectionId, verseIndex, croppedBitmap)
                                    repository.saveChoice(sectionId, verseIndex, ChosenPicture.IMPORTED)
                                }
                                onSaved()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.game_button_yellow_dark)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.image_import_save), color = Color.White)
                    }
                }
            }
        } else {
            // Landscape mode
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, gestureZoom, gestureRotation ->
                                    offsetX += pan.x
                                    offsetY += pan.y
                                    scale *= gestureZoom
                                    scale = scale.coerceIn(0.3f, 5f)
                                    rotation += gestureRotation
                                }
                            }
                    ) {
                        selectedBitmap?.let { bitmap ->
                            displayCanvasWidth = size.width
                            displayCanvasHeight = size.height
                            drawRect(Color.White)
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val imageBitmap = bitmap.asImageBitmap()
                            withTransform({
                                translate(centerX + offsetX, centerY + offsetY)
                                rotate(rotation, androidx.compose.ui.geometry.Offset(0f, 0f))
                                scale(scale, scale, androidx.compose.ui.geometry.Offset(0f, 0f))
                                translate(-bitmap.width / 2f, -bitmap.height / 2f)
                            }) {
                                drawImage(imageBitmap)
                            }
                        }
                    }
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onCancel, modifier = Modifier.height(32.dp)) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.height(18.dp)
                                )
                            }
                            Text(
                                text = sigla,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        text = buildAnnotatedVerseText(verse.text),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { rotation = (rotation - 90f) % 360f },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.game_button_yellow_dark)
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.graphicsLayer { scaleX = -1f }
                            )
                        }
                        Button(
                            onClick = { rotation = (rotation + 90f) % 360f },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.game_button_yellow_dark)
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val croppedBitmap: Bitmap = withContext(Dispatchers.Default) {
                                    cropImageTo34Ratio(
                                        selectedBitmap!!,
                                        offsetX,
                                        offsetY,
                                        scale,
                                        rotation,
                                        displayCanvasWidth.toInt(),
                                        displayCanvasHeight.toInt()
                                    )
                                }
                                withContext(Dispatchers.IO) {
                                    repository.saveImportedPicture(sectionId, verseIndex, croppedBitmap)
                                    repository.saveChoice(sectionId, verseIndex, ChosenPicture.IMPORTED)
                                }
                                onSaved()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.game_button_yellow_dark)
                        ),
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

/** Largest dimension (px) we keep when loading an imported image, to bound memory use. */
private const val MAX_IMPORT_DIMENSION = 2048

fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        // First pass: read only the image bounds, without allocating pixels.
        val bounds = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Compute an inSampleSize that keeps the largest side <= MAX_IMPORT_DIMENSION.
        var sampleSize = 1
        val largestSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (largestSide / sampleSize > MAX_IMPORT_DIMENSION) {
            sampleSize *= 2
        }

        // Second pass: actually decode the downsampled bitmap.
        val decodeOptions = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    } catch (t: Throwable) {
        // Catches OutOfMemoryError (an Error, not an Exception) as well as decode failures.
        t.printStackTrace()
        null
    }
}

fun cropImageTo34Ratio(
    bitmap: Bitmap,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    rotation: Float,
    displayCanvasWidth: Int,
    displayCanvasHeight: Int
): Bitmap {
    val targetWidth = 1200
    val targetHeight = 1600

    val workBitmap = Bitmap.createBitmap(displayCanvasWidth, displayCanvasHeight, Bitmap.Config.ARGB_8888)
    val workCanvas = android.graphics.Canvas(workBitmap)

    workCanvas.drawColor(android.graphics.Color.WHITE)
    workCanvas.save()

    val centerX = displayCanvasWidth / 2f
    val centerY = displayCanvasHeight / 2f
    val imageWidth = bitmap.width
    val imageHeight = bitmap.height

    workCanvas.translate(centerX + offsetX, centerY + offsetY)
    workCanvas.rotate(rotation, 0f, 0f)
    workCanvas.scale(scale, scale, 0f, 0f)
    workCanvas.translate(-imageWidth / 2f, -imageHeight / 2f)

    workCanvas.drawBitmap(bitmap, 0f, 0f, null)
    workCanvas.restore()

    val resultBitmap = Bitmap.createScaledBitmap(workBitmap, targetWidth, targetHeight, true)
    return resultBitmap
}
