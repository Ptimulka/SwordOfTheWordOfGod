package io.github.ptimulka.miecz.repositories.core

import androidx.datastore.core.DataStore
import io.github.ptimulka.miecz.data.SectionProgress
import io.github.ptimulka.miecz.data.UserProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProgressStore @Inject constructor(
    private val dataStore: DataStore<UserProgress>,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    
    @Volatile
    private var _latest: UserProgress = runBlocking { dataStore.data.first() }
    
    val latest: UserProgress get() = _latest

    init {
        dataStore.data.onEach { _latest = it }.launchIn(scope)
    }

    fun update(action: (UserProgress) -> UserProgress) {
        _latest = action(_latest)
        scope.launch {
            dataStore.updateData { action(it) }
        }
    }

    fun updateSection(sectionId: Int, action: (SectionProgress.Builder) -> Unit) {
        update { user ->
            val sectionBuilder = user.sectionsMap[sectionId]?.toBuilder() ?: SectionProgress.newBuilder()
            action(sectionBuilder)
            user.toBuilder()
                .putSections(sectionId, sectionBuilder.build())
                .build()
        }
    }
}
