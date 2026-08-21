package io.github.ptimulka.miecz.repositories.content

import android.content.Context
import io.github.ptimulka.miecz.repositories.MnemonicChoiceRepository
import io.github.ptimulka.miecz.repositories.MnemonicRepository
import io.github.ptimulka.miecz.repositories.ProgressionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class MnemonicRepositoryTest {

    private val context: Context = mock()
    private val progressionRepo: ProgressionRepository = mock()
    private val choiceRepo: MnemonicChoiceRepository = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: MnemonicRepository

    @Before
    fun setup() {
        val filesDir = File("temp_files")
        whenever(context.filesDir).thenReturn(filesDir)
        repository = UserMnemonicPicturesRepository(context, progressionRepo, choiceRepo, testDispatcher)
    }

    @Test
    fun `loadPicture returns null if file missing`() = runTest {
        val result = repository.loadPicture(1, 0)
        assertNull(result)
    }
}
