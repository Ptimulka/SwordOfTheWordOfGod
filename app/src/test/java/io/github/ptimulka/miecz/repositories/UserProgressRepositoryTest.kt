package io.github.ptimulka.miecz.repositories

import androidx.datastore.core.DataStore
import io.github.ptimulka.miecz.data.UserProgress
import io.github.ptimulka.miecz.data.SectionProgress
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
class UserProgressRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()

    private val dataStore: DataStore<UserProgress> = mock()
    private val dataFlow = MutableStateFlow(UserProgressSerializer.defaultValue)
    
    private var currentTime = 1000000L
    private val timeProvider: () -> Long = { currentTime }

    private lateinit var repository: UserProgressRepository

    private fun createRepository() {
        // Use testDispatcher for the scope so updates are immediate
        repository = UserProgressRepository(dataStore, timeProvider, testDispatcher)
    }

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
    }

    @Test
    fun `refreshShields adds one shield after 30 minutes`() {
        dataFlow.value = dataFlow.value.toBuilder()
            .setShieldsCount(4)
            .setLastShieldUpdateTime(currentTime - 30 * 60 * 1000L)
            .build()
        createRepository()
        
        val newCount = repository.refreshShields()
        
        assertEquals(5, newCount)
        assertEquals(5, dataFlow.value.shieldsCount)
        assertEquals(0L, dataFlow.value.lastShieldUpdateTime)
    }

    @Test
    fun `refreshShields preserves remainder time`() {
        val thirtyMins = 30 * 60 * 1000L
        dataFlow.value = dataFlow.value.toBuilder()
            .setShieldsCount(3)
            .setLastShieldUpdateTime(currentTime - (thirtyMins + 5 * 60 * 1000L))
            .build()
        createRepository()
        
        val newCount = repository.refreshShields()
        
        assertEquals(4, newCount)
        val expectedNewUpdateTime = currentTime - (thirtyMins + 5 * 60 * 1000L) + thirtyMins
        assertEquals(expectedNewUpdateTime, dataFlow.value.lastShieldUpdateTime)
    }

    @Test
    fun `refreshShields caps at MAX_SHIELDS`() {
        dataFlow.value = dataFlow.value.toBuilder()
            .setShieldsCount(4)
            .setLastShieldUpdateTime(currentTime - 120 * 60 * 1000L)
            .build()
        createRepository()
        
        val newCount = repository.refreshShields()
        
        assertEquals(5, newCount)
        assertEquals(5, dataFlow.value.shieldsCount)
    }

    @Test
    fun `getTimeToNextShield returns correct remaining ms`() {
        dataFlow.value = dataFlow.value.toBuilder()
            .setShieldsCount(4)
            .setLastShieldUpdateTime(currentTime - 10 * 60 * 1000L)
            .build()
        createRepository()
        
        val remaining = repository.getTimeToNextShield()
        assertEquals(20 * 60 * 1000L, remaining)
    }

    @Test
    fun `applyDailyRetentionDecay subtracts retention after one day`() {
        currentTime = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse("2026-08-07")!!.time
        
        dataFlow.value = dataFlow.value.toBuilder()
            .setRetentionDecayDate("2026-08-06")
            .putSections(1, SectionProgress.newBuilder().setRetention(50).build())
            .build()
        createRepository()
        
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
        createRepository()
        
        repository.applyDailyRetentionDecay()
        
        assertEquals(0, dataFlow.value.sectionsMap[1]?.retention)
    }

    @Test
    fun `incrementVerseRepeatToday resets counts on a new day`() {
        val today = "2026-08-07"
        val tomorrow = "2026-08-08"
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // 1. Set date to Today and repeat Verse 0
        currentTime = sdf.parse(today)!!.time
        createRepository()
        repository.incrementVerseRepeatToday(1, 0)
        
        assertEquals(1, repository.getVerseRepeatCountToday(1, 0))
        
        // 2. Set date to Tomorrow and repeat Verse 1
        currentTime = sdf.parse(tomorrow)!!.time
        // cachedProgress is updated via Flow in real app, in test we re-create repo or wait
        // But since we use UnconfinedTestDispatcher, the flow emission should be immediate
        
        repository.incrementVerseRepeatToday(1, 1)
        
        // 3. Verify Verse 1 is 1, but Verse 0 is reset to 0
        assertEquals(1, repository.getVerseRepeatCountToday(1, 1))
        assertEquals(0, repository.getVerseRepeatCountToday(1, 0))
    }
}
