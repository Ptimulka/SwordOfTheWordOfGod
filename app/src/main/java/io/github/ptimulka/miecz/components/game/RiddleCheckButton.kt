package io.github.ptimulka.miecz.components.game

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ptimulka.miecz.R

/**
 * The yellow "Sprawdź" check button shared by the riddle screens.
 * Pass a [modifier] to override the default full-width, 50dp-tall sizing
 * (e.g. a shorter landscape height or a weighted width inside a Row).
 */
@Composable
fun RiddleCheckButton(
    enabled: Boolean,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
) {
    Button(
        onClick = onCheck,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.game_button_yellow_dark)
        )
    ) {
        Text(stringResource(id = R.string.check_button), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
