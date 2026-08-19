package io.github.ptimulka.miecz.repositories.content

import androidx.datastore.core.DataStore
import io.github.ptimulka.miecz.data.UserProgress
import io.github.ptimulka.miecz.repositories.CustomSectionRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class CustomSectionRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()

    private val dataStore: DataStore<UserProgress> = mock()
    private val dataFlow = MutableStateFlow(UserProgressSerializer.defaultValue)

    private lateinit var repository: CustomSectionRepository

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
        repository = UserCustomSectionRepository(store)
    }

    @Test
    fun `saveCustomSection stores group IDs correctly`() {
        repository.saveCustomSection(5, 10, 11)
        
        val pair = repository.getCustomSectionGroups(5)
        assertEquals(10, pair?.first)
        assertEquals(11, pair?.second)
        assertEquals("10,11", dataFlow.value.customSectionsMap[5])
    }
}
