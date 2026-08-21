package io.github.ptimulka.miecz.repositories.meta

import androidx.datastore.core.DataStore
import io.github.ptimulka.miecz.data.UserProgress
import io.github.ptimulka.miecz.repositories.AchievementRepository
import io.github.ptimulka.miecz.repositories.CustomSectionRepository
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
class AchievementRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()

    private val dataStore: DataStore<UserProgress> = mock()
    private val customSectionRepo: CustomSectionRepository = mock()
    private val dataFlow = MutableStateFlow(UserProgressSerializer.defaultValue)

    private lateinit var repository: AchievementRepository

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
        repository = UserAchievementRepository(store, customSectionRepo)
    }

    @Test
    fun `addTotalReviewedVerses updates state correctly`() = runTest {
        repository.addTotalReviewedVerses(10)
        assertEquals(10, repository.getTotalReviewedVerses())
        assertEquals(10, dataFlow.value.totalReviewedVerses)
    }

    @Test
    fun `updateBestTime only saves if better`() = runTest {
        repository.updateBestTime(1, "CONNECT", 1000L)
        assertEquals(1000L, repository.getBestTime(1, "CONNECT"))
        
        repository.updateBestTime(1, "CONNECT", 1200L) // Worse
        assertEquals(1000L, repository.getBestTime(1, "CONNECT"))
        
        repository.updateBestTime(1, "CONNECT", 800L) // Better
        assertEquals(800L, repository.getBestTime(1, "CONNECT"))
    }
}
