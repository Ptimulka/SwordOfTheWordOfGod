package io.github.ptimulka.miecz.components.game

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.GameActivity.Companion.SECTION_ID_REPEAT_FOR_SHIELDS
import io.github.ptimulka.miecz.R

@Composable
fun GameTopBar(
    sectionId: Int,
    sectionName: String,
    levelNumber: Int,
    currentRiddleIndex: Int,
    totalRiddles: Int,
    shieldsCount: Int,
    onExitClick: () -> Unit,
    showShields: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorResource(id = R.color.game_button_yellow_dark),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // 1. Exit icon button
            IconButton(onClick = onExitClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = stringResource(R.string.close_button),
                    tint = Color.White,
                    modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                )
            }

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            // 2. Caption with number and title of section
            val sectionIdWithDot = if (sectionId <= 0) "" else "$sectionId. "
            Text(
                text = if (isLandscape || (levelNumber == 0 && totalRiddles == 1)) {
                    "$sectionIdWithDot$sectionName"
                } else {
                    "$sectionIdWithDot${truncate(sectionName, 13, 10)}"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))

            // 3. Shields icon and number (if not random, repeat not for shields, connect pairs/parts)
            if (showShields && ((totalRiddles > 1 && sectionId > 0) || sectionId == SECTION_ID_REPEAT_FOR_SHIELDS)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    val shieldIconRes = if (sectionId == SECTION_ID_REPEAT_FOR_SHIELDS) {
                        R.drawable.buttonshieldred
                    } else {
                        R.drawable.buttonshield
                    }
                    Image(
                        painter = painterResource(id = shieldIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = "$shieldsCount",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.size(4.dp))
            }

            // 4. Level icon and number (if not random, repeat, connect pairs/parts, special challenge)
            if (levelNumber != 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.buttonhigh),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = "$levelNumber",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.size(4.dp))
            }

            // 5. Riddle Infoe (e.g. 1/10)
            if (totalRiddles > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.buttonquestion),
                        contentDescription = stringResource(id = R.string.riddle_info),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = "${currentRiddleIndex + 1}/$totalRiddles",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun truncate(text: String, maxLength: Int, truncateAt: Int): String {
    return if (text.length > maxLength) {
        text.substring(0, truncateAt) + "..."
    } else {
        text
    }
}