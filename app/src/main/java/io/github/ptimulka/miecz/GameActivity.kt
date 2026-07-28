package io.github.ptimulka.miecz

import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.components.game.ConnectSuccessDialog
import io.github.ptimulka.miecz.components.game.GameExitDialog
import io.github.ptimulka.miecz.components.game.GameSuccessDialog
import io.github.ptimulka.miecz.components.game.GameSuccessForShieldsDialog
import io.github.ptimulka.miecz.components.game.NewRecordDialog
import io.github.ptimulka.miecz.components.game.NewStreakRecordDialog
import io.github.ptimulka.miecz.components.game.NoPlayingForShieldsDialog
import io.github.ptimulka.miecz.components.game.NoShieldsDialog
import io.github.ptimulka.miecz.components.game.RetentionGainedDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.ptimulka.miecz.GameActivity.Companion.SECTION_ID_REPEAT_FOR_SHIELDS
import io.github.ptimulka.miecz.components.game.GameTopBar
import io.github.ptimulka.miecz.components.game.RiddleRouter
import io.github.ptimulka.miecz.data.Riddle
import io.github.ptimulka.miecz.data.RiddleType
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.helpers.updateProgress
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import io.github.ptimulka.miecz.ui.theme.SwordOfTheWordOfGodTheme
import kotlinx.coroutines.delay

class GameActivity : ComponentActivity() {

    companion object {
        const val ARG_LEVEL_RIDDLE_TYPES = "arg_level_riddle_types"
        const val ARG_SECTION_ID = "arg_section_id"
        const val ARG_SECTION_NAME = "arg_section_name"
        const val ARG_LEVEL_NUMBER = "arg_level_number"
        const val ARG_SECTION_VERSES = "arg_section_verses"
        const val ARG_ASSET_NAMES = "arg_asset_names"
        const val SECTION_ID_REPEAT_NORMAL = -1
        const val SECTION_ID_REPEAT_FOR_SHIELDS = -2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val levelRiddleTypes = intent.getStringArrayListExtra(ARG_LEVEL_RIDDLE_TYPES) ?: arrayListOf()
        val sectionId = intent.getIntExtra(ARG_SECTION_ID, 0)
        val sectionName = intent.getStringExtra(ARG_SECTION_NAME) ?: ""
        val levelNumber = intent.getIntExtra(ARG_LEVEL_NUMBER, 0)
        val sectionVerses = intent.getParcelableArrayListExtra<Verse>(ARG_SECTION_VERSES) ?: arrayListOf()
        val assetNames = intent.getStringArrayListExtra(ARG_ASSET_NAMES) ?: arrayListOf()

        // Hide system bars
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            SwordOfTheWordOfGodTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        levelRiddleTypeNames = levelRiddleTypes,
                        sectionId = sectionId,
                        sectionName = sectionName,
                        levelNumber = levelNumber,
                        sectionVerses = sectionVerses,
                        assetNames = assetNames,
                        modifier = Modifier.padding(innerPadding),
                        onFinish = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    levelRiddleTypeNames: ArrayList<String>,
    sectionId: Int,
    sectionName: String,
    levelNumber: Int,
    sectionVerses: ArrayList<Verse>,
    assetNames: ArrayList<String> = arrayListOf(),
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProgressRepository = remember { UserProgressRepository(context) }

    val riddles = rememberSaveable(levelRiddleTypeNames, sectionVerses) {
        val riddleTypes = levelRiddleTypeNames.mapNotNull { name ->
            try {
                RiddleType.valueOf(name)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        riddleTypes.mapIndexed { index, riddleType ->
            val verse = sectionVerses.getOrElse(index) { sectionVerses.last() } // Fallback to last verse
            Riddle(riddleType, verse)
        }.shuffled()
    }
    
    var currentRiddleIndex by rememberSaveable { mutableStateOf(0) }
    var shieldsCount by rememberSaveable { mutableStateOf(userProgressRepository.getShieldsCount()) }

    var showBigSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var connectSuccessTimeMs by rememberSaveable { mutableStateOf(-1L) }
    var showBigSuccessForShieldsDialog by rememberSaveable { mutableStateOf(false) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showNoShieldsDialog by rememberSaveable { mutableStateOf(false) }
    var showNoMorePlayingForShieldsDialog by rememberSaveable { mutableStateOf(false) }
    var newRecordTimeMs by rememberSaveable { mutableStateOf(-1L) }
    var newStreakRecord by rememberSaveable { mutableStateOf(-1) }
    var shieldLostThisLevel by rememberSaveable { mutableStateOf(false) }
    // >= 0 means: show the "retention gained" dialog (after any records dialog). -1 = don't show.
    var retentionGainedToShow by rememberSaveable { mutableStateOf(-1) }

    val startTimeMs = rememberSaveable { System.currentTimeMillis() }

    // Handle shields refresh
    LaunchedEffect(Unit) {
        while(true) {
            val shields = userProgressRepository.refreshShields()
            if (shields != shieldsCount) {
                shieldsCount = shields
            }
            delay(60000) // Wait 1 minute
        }
    }

    // After a records/success dialog is confirmed, either finish or show the retention dialog next.
    fun proceedOrShowRetention() { if (retentionGainedToShow < 0) onFinish() }

    if (showExitDialog) GameExitDialog(onDismiss = { showExitDialog = false }, onConfirm = onFinish)
    if (newRecordTimeMs >= 0) NewRecordDialog(timeMs = newRecordTimeMs, onConfirm = { newRecordTimeMs = -1L; proceedOrShowRetention() })
    if (newStreakRecord >= 0) NewStreakRecordDialog(streak = newStreakRecord, onConfirm = { newStreakRecord = -1; proceedOrShowRetention() })
    if (connectSuccessTimeMs >= 0) {
        val msgRes = if (riddles.any { it.type == RiddleType.CONNECT_PARTS }) R.string.connect_parts_success_message else R.string.connect_pairs_success_message
        ConnectSuccessDialog(timeMs = connectSuccessTimeMs, messageRes = msgRes, onConfirm = { connectSuccessTimeMs = -1L; proceedOrShowRetention() })
    }
    if (showBigSuccessDialog) GameSuccessDialog(onConfirm = { showBigSuccessDialog = false; proceedOrShowRetention() })

    // Retention dialog — shown once every preceding records/success dialog is dismissed
    if (retentionGainedToShow >= 0 &&
        newRecordTimeMs < 0 && newStreakRecord < 0 && connectSuccessTimeMs < 0 &&
        !showBigSuccessDialog && !showBigSuccessForShieldsDialog
    ) {
        RetentionGainedDialog(gained = retentionGainedToShow, onConfirm = { retentionGainedToShow = -1; onFinish() })
    }

    val shieldsToReceive = if (riddles.size == 10) 2 else 1
    if (showBigSuccessForShieldsDialog) GameSuccessForShieldsDialog(shieldsToReceive, onConfirm = onFinish)
    if (showNoShieldsDialog) NoShieldsDialog(onConfirm = onFinish)
    if (showNoMorePlayingForShieldsDialog) NoPlayingForShieldsDialog(onConfirm = onFinish)

    fun handleSuccess(riddleElapsedMs: Long? = null) {
        if (currentRiddleIndex < riddles.size - 1) {
            currentRiddleIndex++
        } else {
            userProgressRepository.updateDayStreak()

            val singleType = riddles.map { it.type }.toSet().singleOrNull()
            val isConnect = singleType == RiddleType.CONNECT_PARTS || singleType == RiddleType.CONNECT_PAIRS

            // Award retention and remember how much, to show a dialog (only when >0) after any
            // records dialog. Must run before updateProgress, which marks the level finished.
            if (sectionId >= 1) {
                val gained = when {
                    // First connect completion per day grants +2% retention for the section
                    isConnect -> userProgressRepository.awardRetentionForConnectLevel(sectionId, singleType!!.name)
                    // Only the very first completion of a standard level grants +3% retention
                    levelNumber in 1..12 -> userProgressRepository.awardRetentionForStandardLevel(sectionId, levelNumber)
                    else -> 0
                }
                if (gained > 0) retentionGainedToShow = gained
            }

            // Mark progress before the streak logic, so beating the record can't skip it via early return
            updateProgress(sectionId, levelNumber, userProgressRepository, riddles)

            // Perfect levels streak logic
            val excludedFromStreak = setOf(RiddleType.CONNECT_PARTS, RiddleType.CONNECT_PAIRS, RiddleType.REPEAT_VERSE)
            val countsForStreak = sectionId >= 1 && !shieldLostThisLevel && riddles.none { it.type in excludedFromStreak }
            if (countsForStreak) {
                userProgressRepository.incrementLevelStreak()
                val current = userProgressRepository.getLevelStreak()
                if (userProgressRepository.updateBestLevelStreak(current)) {
                    newStreakRecord = current
                    return
                }
            }

            if (isConnect) {
                val elapsed = riddleElapsedMs ?: (System.currentTimeMillis() - startTimeMs)
                val isNewRecord = userProgressRepository.updateBestTime(sectionId, singleType!!.name, elapsed)
                if (isNewRecord) {
                    newRecordTimeMs = elapsed
                } else {
                    connectSuccessTimeMs = elapsed
                }
                return
            }

            if (riddles.size > 1) {
                if(sectionId == SECTION_ID_REPEAT_FOR_SHIELDS) {
                    showBigSuccessForShieldsDialog = true
                }
                else showBigSuccessDialog = true
            } else {
                // Single-riddle level: no big-success dialog, but still show retention if gained
                proceedOrShowRetention()
            }
        }
    }

    // Handle shield loss, returns true if user is out of shields
    fun handleShieldLoss(): Boolean {
        // Immediate defeat if playing repeat mode for shields
        if(sectionId == SECTION_ID_REPEAT_FOR_SHIELDS) {
            showNoMorePlayingForShieldsDialog = true
            return true
        }

        // If repeat normal mode or random mode then no shields are lost
        if(sectionId < 1) return false

        userProgressRepository.refreshShields()
        userProgressRepository.decreaseShields()
        userProgressRepository.resetLevelStreak()
        shieldLostThisLevel = true
        shieldsCount = userProgressRepository.getShieldsCount()
        if (shieldsCount <= 0) {
            userProgressRepository.setPendingRepeatHint()
            showNoShieldsDialog = true
            return true
        } else {
            return false
        }
    }

    BackHandler(enabled = currentRiddleIndex > 0) { showExitDialog = true }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar styled like MainActivity
        GameTopBar(
            sectionId = sectionId,
            sectionName = sectionName,
            levelNumber = levelNumber,
            currentRiddleIndex = currentRiddleIndex,
            totalRiddles = riddles.size,
            shieldsCount = shieldsCount,
            showShields = riddles.none { it.type == RiddleType.REPEAT_VERSE },
            onExitClick = {
                if (currentRiddleIndex > 0) showExitDialog = true else onFinish()
            }
        )

        // Content Area - Displays the current riddle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (currentRiddleIndex < riddles.size) {
                RiddleRouter(
                    riddle = riddles[currentRiddleIndex],
                    sectionId = sectionId,
                    sectionVerses = sectionVerses,
                    assetNames = assetNames,
                    onSuccess = { elapsedMs -> handleSuccess(elapsedMs) },
                    onShieldLoss = { handleShieldLoss() }
                )
            }
        }
    }
}
