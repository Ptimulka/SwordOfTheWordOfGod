package io.github.ptimulka.miecz.repositories.game

import androidx.datastore.core.DataStore
import io.github.ptimulka.miecz.data.UserProgress
import io.github.ptimulka.miecz.repositories.StreakRepository
import io.github.ptimulka.miecz.repositories.core.UserProgressStore
import io.github.ptimulka.miecz.repositories.core.UserProgressSerializer
import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class StreakRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()

    private val dataStore: DataStore<UserProgress> = mock()
    private val dataFlow = MutableStateFlow(UserProgressSerializer.defaultValue)
    
    private var currentTime = 1000000L
    private val timeProvider: () -> Long = { currentTime }

    private lateinit var repository: StreakRepository

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
        repository = UserStreakRepository(store, timeProvider)
    }

    @Test
    fun `updateDayStreak increments on consecutive days`() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = "2026-08-07"
        val yesterday = "2026-08-06"
        
        dataFlow.value = dataFlow.value.toBuilder()
            .setLastPlayedDate(yesterday)
            .setDayStreak(1)
            .build()
            
        currentTime = sdf.parse(today)!!.time
        repository.updateDayStreak()
        
        assertEquals(2, repository.getCurrentDayStreak())
        assertEquals(today, dataFlow.value.lastPlayedDate)
    }
}
