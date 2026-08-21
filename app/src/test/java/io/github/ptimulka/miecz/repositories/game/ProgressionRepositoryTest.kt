package io.github.ptimulka.miecz.repositories.game

import androidx.datastore.core.DataStore
import io.github.ptimulka.miecz.data.UserProgress
import io.github.ptimulka.miecz.repositories.ProgressionRepository
import io.github.ptimulka.miecz.repositories.core.UserProgressStore
import io.github.ptimulka.miecz.repositories.core.UserProgressSerializer
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
class ProgressionRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()

    private val dataStore: DataStore<UserProgress> = mock()
    private val dataFlow = MutableStateFlow(UserProgressSerializer.defaultValue)

    private lateinit var repository: ProgressionRepository

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
        repository = UserProgressionRepository(store)
    }

    @Test
    fun `setLevelFinished updates state correctly`() = runTest {
        repository.setLevelFinished(1, 1, true)
        assertTrue(repository.isLevelFinished(1, 1))
        assertEquals(true, dataFlow.value.sectionsMap[1]?.finishedLevelsMap?.get(1))
    }

    @Test
    fun `getFinishedLevelsCount includes levels and challenges`() = runTest {
        repository.setLevelFinished(1, 1, true)
        repository.setSiglaFinished(1, true)
        repository.setVerseFinished(1, true)
        
        assertEquals(3, repository.getFinishedLevelsCount())
    }
}
