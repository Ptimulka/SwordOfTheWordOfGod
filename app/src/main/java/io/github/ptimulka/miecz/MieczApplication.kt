package io.github.ptimulka.miecz

import android.app.Application
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import dagger.hilt.android.HiltAndroidApp

@OptIn(ExperimentalComposeUiApi::class)
@HiltAndroidApp
class MieczApplication : Application() {
    override fun onCreate() {
        AndroidComposeUiFlags
            .isOutOfFrameSchedulerForTextInputEventsEnabled = false
        super.onCreate()
    }
}
