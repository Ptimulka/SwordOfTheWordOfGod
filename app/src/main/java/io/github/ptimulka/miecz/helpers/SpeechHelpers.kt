package io.github.ptimulka.miecz.helpers

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Remembers an activity-result launcher for the system speech-recognition dialog. On a successful
 * result the top recognition candidate (or an empty string) is delivered to [onResult]. Launch it
 * with an intent from [createPolishSpeechIntent].
 */
@Composable
fun rememberSpeechLauncher(
    onResult: (String) -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            onResult(recognized)
        }
    }

/**
 * Builds the Polish speech-recognition intent shared by the riddle screens.
 *
 * [preferOffline] asks the recognizer to use an on-device language pack, so recognition also works
 * without internet. Devices without a Polish offline pack may fail instead of falling back, so
 * callers that can detect the failure should retry with `preferOffline = false`.
 */
fun createPolishSpeechIntent(
    preferOffline: Boolean = true,
    maxResults: Int? = null,
    partialResults: Boolean = false,
    prompt: String? = null
): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL")
    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
    maxResults?.let { putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, it) }
    if (partialResults) putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    prompt?.let { putExtra(RecognizerIntent.EXTRA_PROMPT, it) }
}
