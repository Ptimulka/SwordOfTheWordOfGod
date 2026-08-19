package io.github.ptimulka.miecz.repositories.content

import androidx.datastore.core.DataStore
import io.github.ptimulka.miecz.data.UserProgress
import io.github.ptimulka.miecz.data.SectionProgress
import io.github.ptimulka.miecz.repositories.RetentionRepository
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
class RetentionRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()

    private val dataStore: DataStore<UserProgress> = mock()
    private val dataFlow = MutableStateFlow(UserProgressSerializer.defaultValue)
    
    private var currentTime = 1000000L
    private val timeProvider: () -> Long = { currentTime }

    private lateinit var repository: RetentionRepository

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
        repository = UserRetentionRepository(store, timeProvider)
    }

    @Test
    fun `applyDailyRetentionDecay subtracts retention after one day`() {
        currentTime = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse("2026-08-07")!!.time
        
        dataFlow.value = dataFlow.value.toBuilder()
            .setRetentionDecayDate("2026-08-06")
            .putSections(1, SectionProgress.newBuilder().setRetention(50).build())
            .build()
        
        repository.applyDailyRetentionDecay()
        
        assertEquals(45, dataFlow.value.sectionsMap[1]?.retention)
        assertEquals("2026-08-07", dataFlow.value.retentionDecayDate)
    }

    @Test
    fun `applyDailyRetentionDecay does not drop below zero`() {
        currentTime = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse("2026-08-07")!!.time
        
        dataFlow.value = dataFlow.value.toBuilder()
            .setRetentionDecayDate("2026-08-06")
            .putSections(1, SectionProgress.newBuilder().setRetention(3).build())
            .build()
        
        repository.applyDailyRetentionDecay()
        
        assertEquals(0, dataFlow.value.sectionsMap[1]?.retention)
    }

    @Test
    fun `incrementVerseRepeatToday resets counts on a new day`() {
        val today = "2026-08-07"
        val tomorrow = "2026-08-08"
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        currentTime = sdf.parse(today)!!.time
        repository.incrementVerseRepeatToday(1, 0)
        assertEquals(1, repository.getVerseRepeatCountToday(1, 0))
        
        currentTime = sdf.parse(tomorrow)!!.time
        repository.incrementVerseRepeatToday(1, 1)
        
        assertEquals(1, repository.getVerseRepeatCountToday(1, 1))
        assertEquals(0, repository.getVerseRepeatCountToday(1, 0))
    }
}
