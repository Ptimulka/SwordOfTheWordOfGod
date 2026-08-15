package io.github.ptimulka.miecz

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.ptimulka.miecz.helpers.NotificationHelper
import io.github.ptimulka.miecz.repositories.SettingsRepository
import io.github.ptimulka.miecz.screens.groups.VerseGroupsScreen
import io.github.ptimulka.miecz.screens.main.GameLevelScreen
import io.github.ptimulka.miecz.screens.main.MainEvent
import io.github.ptimulka.miecz.screens.main.MainViewModel
import io.github.ptimulka.miecz.screens.main.Screen
import io.github.ptimulka.miecz.screens.random.RandomVerseScreen
import io.github.ptimulka.miecz.screens.review.ReviewVersesScreen
import io.github.ptimulka.miecz.screens.settings.SettingsScreen
import io.github.ptimulka.miecz.ui.theme.SwordOfTheWordOfGodTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        NotificationHelper.createNotificationChannel(this)
        val settingsRepo = SettingsRepository(this)
        if (settingsRepo.isNotificationsEnabled()) {
            NotificationHelper.scheduleDailyNotification(
                this,
                settingsRepo.getNotificationHour(),
                settingsRepo.getNotificationMinute()
            )
        }
        setContent {
            SwordOfTheWordOfGodTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val vm: MainViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    
    LaunchedEffect(Unit) {
        vm.onEvent(MainEvent.RefreshTabVisibility)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = if (isLandscape) Modifier.height(64.dp) else Modifier
            ) {
                val items = mutableListOf(Screen.Levels, Screen.Random)
                if (state.isReviewTabVisible) {
                    items.add(Screen.Review)
                }
                items.add(Screen.VerseGroups)
                items.add(Screen.Settings)

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            when (screen) {
                                Screen.Levels -> Icon(painter = painterResource(id = R.drawable.ic_levels_path), contentDescription = null)
                                Screen.Random -> Icon(painter = painterResource(id = R.drawable.ic_dice), contentDescription = null)
                                Screen.Review -> Icon(Icons.Default.Refresh, contentDescription = null)
                                Screen.VerseGroups -> Icon(Icons.Default.Search, contentDescription = null)
                                Screen.Settings -> Icon(painter = painterResource(id = R.drawable.ic_settings), contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(screen.resourceId), fontSize = 9.sp) },
                        selected = state.selectedScreen == screen,
                        onClick = { vm.onEvent(MainEvent.SelectScreen(screen)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colorResource(id = R.color.game_button_yellow_dark),
                            selectedTextColor = colorResource(id = R.color.game_button_yellow_dark),
                            indicatorColor = colorResource(id = R.color.game_button_yellow_light).copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (state.selectedScreen) {
            Screen.Levels -> GameLevelScreen(innerPadding)
            Screen.Random -> RandomVerseScreen(innerPadding)
            Screen.Review -> ReviewVersesScreen(innerPadding)
            Screen.VerseGroups -> VerseGroupsScreen(innerPadding)
            Screen.Settings -> SettingsScreen(innerPadding)
        }
    }
}
