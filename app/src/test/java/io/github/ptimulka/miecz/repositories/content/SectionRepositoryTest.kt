package io.github.ptimulka.miecz.repositories.content

import android.content.Context
import android.content.res.Resources
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.repositories.SectionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SectionRepositoryTest {

    private val context: Context = mock()
    private val resources: Resources = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: SectionRepository

    @Before
    fun setup() {
        whenever(context.resources).thenReturn(resources)
        repository = UserSectionRepository(context, testDispatcher)
    }

    @Test
    fun `loadSection returns null on empty input`() = runTest {
        val stream = ByteArrayInputStream("".toByteArray())
        whenever(resources.openRawResource(any())).thenReturn(stream)
        
        val result = repository.loadSection(R.raw.section01)
        org.junit.Assert.assertNull(result)
    }
}
