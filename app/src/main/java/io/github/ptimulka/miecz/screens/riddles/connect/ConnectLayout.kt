package io.github.ptimulka.miecz.screens.riddles.connect

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.helpers.buildAnnotatedVerseText

@Composable
fun ConnectLayout(
    captionRes: Int,
    state: ConnectUiState,
    onEvent: (ConnectEvent) -> Unit,
    rightColumnWeight: Float = 1f,
    showImagesOnButtons: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val buttonHeight = if (isLandscape) 80.dp else 180.dp
    val buttonPadding = PaddingValues(horizontal = 8.dp)

    val scale by animateFloatAsState(
        targetValue = if (state.justMatchedId != null) 0f else 1f,
        animationSpec = tween(300)
    )

    val redOverlayAlpha = remember { Animatable(0f) }
    LaunchedEffect(state.isLocked) {
        if (state.isLocked) {
            redOverlayAlpha.snapTo(0.35f)
            redOverlayAlpha.animateTo(0f, animationSpec = tween(2000))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = captionRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Column
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.leftItems.take(3).forEach { item ->
                        val isSelected = state.selectedLeft == item
                        val isWrong = state.wrongPair?.first == item
                        val isMatched = state.justMatchedId == item.id

                        Button(
                            onClick = { onEvent(ConnectEvent.SelectLeft(item)) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isWrong -> colorResource(R.color.wrong_answer_highlight)
                                    isSelected -> colorResource(id = R.color.game_button_yellow_dark)
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                },
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = buttonPadding,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight)
                                .graphicsLayer(scaleX = if (isMatched) scale else 1f, scaleY = if (isMatched) scale else 1f)
                        ) {
                            val bitmap = state.buttonBitmaps[item.id]
                            if (showImagesOnButtons) {
                                // Pairs style: Always large font, image if available
                                if (isLandscape) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(item.text, textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        if (bitmap != null) {
                                            Spacer(Modifier.width(6.dp))
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.height(56.dp).width(42.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(item.text, textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        if (bitmap != null) {
                                            Spacer(Modifier.height(6.dp))
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxWidth().height(100.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Parts style: standard font size
                                Text(buildAnnotatedVerseText(item.text), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right Column
                Column(modifier = Modifier.weight(rightColumnWeight), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.rightItems.take(3).forEach { item ->
                        val isSelected = state.selectedRight == item
                        val isWrong = state.wrongPair?.second == item
                        val isMatched = state.justMatchedId == item.id

                        Button(
                            onClick = { onEvent(ConnectEvent.SelectRight(item)) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isWrong -> colorResource(R.color.wrong_answer_highlight)
                                    isSelected -> colorResource(id = R.color.game_button_yellow_dark)
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                },
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = buttonPadding,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight)
                                .graphicsLayer(scaleX = if (isMatched) scale else 1f, scaleY = if (isMatched) scale else 1f)
                        ) {
                            Text(buildAnnotatedVerseText(item.text), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        
        if (redOverlayAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = redOverlayAlpha.value))
            )
        }
    }
}
