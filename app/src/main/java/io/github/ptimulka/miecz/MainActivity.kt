package io.github.ptimulka.miecz

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.content.res.Configuration
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.ptimulka.miecz.helpers.NotificationHelper
import io.github.ptimulka.miecz.repositories.SettingsRepository
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import io.github.ptimulka.miecz.screens.GameLevelScreen
import io.github.ptimulka.miecz.screens.RandomVerseScreen
import io.github.ptimulka.miecz.screens.ReviewVersesScreen
import io.github.ptimulka.miecz.screens.SettingsScreen
import io.github.ptimulka.miecz.screens.VerseGroupsScreen
import io.github.ptimulka.miecz.ui.theme.SwordOfTheWordOfGodTheme
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class Screen(val route: String, val resourceId: Int) : Parcelable {
    object Levels : Screen("levels", R.string.levels_tab_caption)
    object Random : Screen("random", R.string.random_tab_caption)
    object Review : Screen("review", R.string.review_tab_caption)
    object VerseGroups : Screen("verse_groups", R.string.verse_groups_tab_caption)
    object Settings : Screen("settings", R.string.settings_tab_caption)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
    val context = LocalContext.current
    val userProgressRepository = remember { UserProgressRepository(context) }
    val currentSection = userProgressRepository.getCurrentSection()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var selectedScreen by rememberSaveable { mutableStateOf<Screen>(Screen.Levels) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = if (isLandscape) Modifier.height(64.dp) else Modifier
            ) {
                val items = mutableListOf(Screen.Levels, Screen.Random)
                if (currentSection >= 3) {
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
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen },
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
        when (selectedScreen) {
            Screen.Levels -> GameLevelScreen(innerPadding)
            Screen.Random -> RandomVerseScreen(innerPadding)
            Screen.Review -> ReviewVersesScreen(innerPadding)
            Screen.VerseGroups -> VerseGroupsScreen(innerPadding)
            Screen.Settings -> SettingsScreen(innerPadding)
        }
    }
}
