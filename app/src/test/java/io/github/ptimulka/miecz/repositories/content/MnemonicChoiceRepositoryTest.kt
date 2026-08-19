package io.github.ptimulka.miecz.repositories.content

import androidx.datastore.core.DataStore
import io.github.ptimulka.miecz.data.UserProgress
import io.github.ptimulka.miecz.repositories.MnemonicChoiceRepository
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
class MnemonicChoiceRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()

    private val dataStore: DataStore<UserProgress> = mock()
    private val dataFlow = MutableStateFlow(UserProgressSerializer.defaultValue)

    private lateinit var repository: MnemonicChoiceRepository

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
        repository = UserMnemonicChoiceRepository(store)
    }

    @Test
    fun `saveMnemonicChoice stores enum name correctly`() {
        repository.saveMnemonicChoice(1, 0, "USER")
        
        assertEquals("USER", repository.getMnemonicChoice(1, 0))
        assertEquals("USER", dataFlow.value.sectionsMap[1]?.mnemonicChoicesMap?.get(0))
    }
}
