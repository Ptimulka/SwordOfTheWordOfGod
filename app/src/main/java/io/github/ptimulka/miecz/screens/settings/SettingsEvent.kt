package io.github.ptimulka.miecz.screens.settings

sealed interface SettingsEvent {
    data class ToggleNotifications(val enabled: Boolean) : SettingsEvent
    data class UpdateNotificationTime(val hour: Int, val minute: Int) : SettingsEvent
    data object RequestReset : SettingsEvent
    data object ConfirmReset : SettingsEvent
    data object FinalConfirmReset : SettingsEvent
    data object CancelReset : SettingsEvent
    data object OnResume : SettingsEvent
}
