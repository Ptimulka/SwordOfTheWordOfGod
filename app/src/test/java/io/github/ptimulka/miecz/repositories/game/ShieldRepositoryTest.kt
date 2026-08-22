package io.github.ptimulka.miecz.repositories.game

import androidx.datastore.core.DataStore
import io.github.ptimulka.miecz.data.UserProgress
import io.github.ptimulka.miecz.repositories.ShieldRepository
import io.github.ptimulka.miecz.repositories.core.UserProgressStore
import io.github.ptimulka.miecz.repositories.core.UserProgressSerializer
import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ShieldRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()

    private val dataStore: DataStore<UserProgress> = mock()
    private val dataFlow = MutableStateFlow(UserProgressSerializer.defaultValue)
    
    private var currentTime = 1000000L
    private val timeProvider: () -> Long = { currentTime }

    private lateinit var repository: ShieldRepository

    @Before
    fun setup() {
        whenever(dataStore.data).thenReturn(dataFlow)
        runBlocking {
            whenever(dataStore.updateData(any())).thenAnswer { invocation ->
                val transform = invocation.getArgument<suspend (UserProgress) -> UserProgress>(0)
                runBlocking {
                    val next = transform(dataFlow.value)
                    dataFlow.value = next
                    next
                }
            }
        }
        
        val store = UserProgressStore(dataStore, testDispatcher)
        repository = UserShieldRepository(store, timeProvider)
    }

    @Test
    fun `refreshShields adds one shield after 30 minutes`() = runTest {
        dataFlow.value = dataFlow.value.toBuilder()
            .setShieldsCount(4)
            .setLastShieldUpdateTime(currentTime - 30 * 60 * 1000L)
            .build()
        
        val newCount = repository.refreshShields()
        
        assertEquals(5, newCount)
        assertEquals(5, dataFlow.value.shieldsCount)
        assertEquals(0L, dataFlow.value.lastShieldUpdateTime)
    }

    @Test
    fun `refreshShields preserves remainder time`() = runTest {
        val thirtyMins = 30 * 60 * 1000L
        dataFlow.value = dataFlow.value.toBuilder()
            .setShieldsCount(3)
            .setLastShieldUpdateTime(currentTime - (thirtyMins + 5 * 60 * 1000L))
            .build()
        
        val newCount = repository.refreshShields()
        
        assertEquals(4, newCount)
        val expectedNewUpdateTime = currentTime - (thirtyMins + 5 * 60 * 1000L) + thirtyMins
        assertEquals(expectedNewUpdateTime, dataFlow.value.lastShieldUpdateTime)
    }

    @Test
    fun `getTimeToNextShield returns correct remaining ms`() = runTest {
        dataFlow.value = dataFlow.value.toBuilder()
            .setShieldsCount(4)
            .setLastShieldUpdateTime(currentTime - 10 * 60 * 1000L)
            .build()
        
        val remaining = repository.getTimeToNextShield()
        assertEquals(20 * 60 * 1000L, remaining)
    }
}
