package io.github.ptimulka.miecz.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.settings.AchievementsSection
import io.github.ptimulka.miecz.components.settings.InfoSection
import io.github.ptimulka.miecz.components.settings.NotificationSection
import io.github.ptimulka.miecz.components.settings.ResetConfirmDialog
import io.github.ptimulka.miecz.components.settings.ResetFinalConfirmDialog
import io.github.ptimulka.miecz.components.settings.ResetSection
import io.github.ptimulka.miecz.helpers.AndroidNotificationScheduler
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.SettingsRepository
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository

@Composable
fun SettingsScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current
    
    val appVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    val vm: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    context.applicationContext,
                    SettingsRepository(context),
                    UserProgressRepository(context),
                    MnemonicPicturesRepository(context),
                    SectionRepository(context),
                    VersesGroupsRepository(context),
                    AndroidNotificationScheduler(context),
                    appVersion
                ) as T
            }
        }
    )

    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.onEvent(SettingsEvent.OnResume)
    }

    SettingsContent(state, vm::onEvent, innerPadding)
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current

    // Reset Dialogs
    when (state.dialogState) {
        SettingsDialogState.CONFIRM_RESET -> {
            ResetConfirmDialog(
                onConfirm = { onEvent(SettingsEvent.ConfirmReset) },
                onDismiss = { onEvent(SettingsEvent.CancelReset) }
            )
        }
        SettingsDialogState.FINAL_CONFIRM_RESET -> {
            ResetFinalConfirmDialog(
                onConfirm = { 
                    onEvent(SettingsEvent.FinalConfirmReset)
                    Toast.makeText(context, context.getString(R.string.reset_progress_done), Toast.LENGTH_LONG).show()
                },
                onDismiss = { onEvent(SettingsEvent.CancelReset) }
            )
        }
        SettingsDialogState.NONE -> {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorResource(R.color.game_button_yellow_dark),
            shadowElevation = 4.dp
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            NotificationSection(state, onEvent)

            Spacer(modifier = Modifier.height(32.dp))

            AchievementsSection(state.achievements)

            Spacer(modifier = Modifier.height(32.dp))

            InfoSection(state.appVersion)

            Spacer(modifier = Modifier.height(32.dp))

            ResetSection { onEvent(SettingsEvent.RequestReset) }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
