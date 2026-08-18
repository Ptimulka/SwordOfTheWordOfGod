package io.github.ptimulka.miecz.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class UserSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettingsRepository {

    companion object {
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        private val KEY_NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")

        const val DEFAULT_HOUR = 8
        const val DEFAULT_MINUTE = 0
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    
    @Volatile
    private var cachedPreferences: Preferences = runBlocking { dataStore.data.first() }

    init {
        dataStore.data.onEach { cachedPreferences = it }.launchIn(scope)
    }

    override fun isNotificationsEnabled(): Boolean {
        return cachedPreferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    override fun setNotificationsEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
        }
    }

    override fun getNotificationHour(): Int {
        return cachedPreferences[KEY_NOTIFICATION_HOUR] ?: DEFAULT_HOUR
    }

    override fun getNotificationMinute(): Int {
        return cachedPreferences[KEY_NOTIFICATION_MINUTE] ?: DEFAULT_MINUTE
    }

    override fun setNotificationTime(hour: Int, minute: Int) {
        scope.launch {
            dataStore.edit {
                it[KEY_NOTIFICATION_HOUR] = hour
                it[KEY_NOTIFICATION_MINUTE] = minute
            }
        }
    }
}
