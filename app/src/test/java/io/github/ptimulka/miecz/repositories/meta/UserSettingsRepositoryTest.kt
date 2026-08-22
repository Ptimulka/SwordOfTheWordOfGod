package io.github.ptimulka.miecz.repositories.meta

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.ptimulka.miecz.repositories.SettingsRepository
import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class UserSettingsRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()

    private val dataStore: DataStore<Preferences> = mock()
    private val mockPreferences: Preferences = mock()
    private val dataFlow = MutableStateFlow(mockPreferences)

    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        whenever(dataStore.data).thenReturn(dataFlow)
        runBlocking {
            whenever(dataStore.updateData(any())).thenAnswer { invocation ->
                val transform = invocation.getArgument<suspend (Preferences) -> Preferences>(0)
                runBlocking {
                    val next = transform(dataFlow.value)
                    dataFlow.value = next
                    next
                }
            }
        }
        
        repository = UserSettingsRepository(dataStore, testDispatcher)
    }

    @Test
    fun `isNotificationsEnabled defaults to true`() = runTest {
        whenever(mockPreferences[any<Preferences.Key<Boolean>>()]).thenReturn(null)
        assertTrue(repository.isNotificationsEnabled())
    }

    @Test
    fun `getNotificationTime returns defaults`() = runTest {
        whenever(mockPreferences[any<Preferences.Key<Int>>()]).thenReturn(null)
        assertEquals(8, repository.getNotificationHour())
        assertEquals(0, repository.getNotificationMinute())
    }
}
