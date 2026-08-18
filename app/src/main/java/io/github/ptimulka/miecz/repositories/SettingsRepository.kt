package io.github.ptimulka.miecz.repositories

interface SettingsRepository {
    fun isNotificationsEnabled(): Boolean
    fun setNotificationsEnabled(enabled: Boolean)
    fun getNotificationHour(): Int
    fun getNotificationMinute(): Int
    fun setNotificationTime(hour: Int, minute: Int)
}
