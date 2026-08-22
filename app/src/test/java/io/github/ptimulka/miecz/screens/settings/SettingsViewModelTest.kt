package io.github.ptimulka.miecz.screens.settings

import io.github.ptimulka.miecz.helpers.MainDispatcherRule
import io.github.ptimulka.miecz.helpers.NotificationScheduler
import io.github.ptimulka.miecz.repositories.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val progressRepo: ProgressRepository = mock()
    private val sectionRepo: SectionRepository = mock()
    private val groupsRepo: VersesGroupsRepository = mock()
    private val settingsRepo: SettingsRepository = mock()
    private val mnemonicRepo: MnemonicRepository = mock()
    private val notificationScheduler: NotificationScheduler = mock()

    @Test
    fun `initialization loads notification settings and version`() = runTest {
        whenever(settingsRepo.isNotificationsEnabled()).thenReturn(true)
        whenever(settingsRepo.getNotificationHour()).thenReturn(15)
        whenever(settingsRepo.getNotificationMinute()).thenReturn(30)
        whenever(sectionRepo.loadInitialSections()).thenReturn(emptyList())
        whenever(groupsRepo.loadVerseGroups()).thenReturn(emptyList())
        
        val viewModel = SettingsViewModel(progressRepo, sectionRepo, groupsRepo, settingsRepo, mnemonicRepo, notificationScheduler, "1.2.3")
        
        val state = viewModel.state.value
        assertTrue(state.notificationsEnabled)
        assertEquals(15, state.notificationHour)
        assertEquals(30, state.notificationMinute)
        assertEquals("1.2.3", state.appVersion)
    }

    @Test
    fun `toggleNotifications updates repo and state`() = runTest {
        whenever(sectionRepo.loadInitialSections()).thenReturn(emptyList())
        whenever(groupsRepo.loadVerseGroups()).thenReturn(emptyList())
        val viewModel = SettingsViewModel(progressRepo, sectionRepo, groupsRepo, settingsRepo, mnemonicRepo, notificationScheduler, "1.0")
        
        viewModel.onEvent(SettingsEvent.ToggleNotifications(true))
        
        assertTrue(viewModel.state.value.notificationsEnabled)
        verify(settingsRepo).setNotificationsEnabled(true)
        verify(notificationScheduler).scheduleDailyNotification(any(), any())
    }

    @Test
    fun `finalConfirmReset clears all data`() = runTest {
        whenever(sectionRepo.loadInitialSections()).thenReturn(emptyList())
        whenever(groupsRepo.loadVerseGroups()).thenReturn(emptyList())
        val viewModel = SettingsViewModel(progressRepo, sectionRepo, groupsRepo, settingsRepo, mnemonicRepo, notificationScheduler, "1.0")
        
        viewModel.onEvent(SettingsEvent.FinalConfirmReset)
        
        verify(progressRepo).clearAllProgress()
        verify(mnemonicRepo).clearAllPictures()
        assertEquals(SettingsDialogState.NONE, viewModel.state.value.dialogState)
    }

    @Test
    fun `requestReset shows confirm dialog`() = runTest {
        whenever(sectionRepo.loadInitialSections()).thenReturn(emptyList())
        whenever(groupsRepo.loadVerseGroups()).thenReturn(emptyList())
        val viewModel = SettingsViewModel(progressRepo, sectionRepo, groupsRepo, settingsRepo, mnemonicRepo, notificationScheduler, "1.0")
        
        viewModel.onEvent(SettingsEvent.RequestReset)
        
        assertEquals(SettingsDialogState.CONFIRM_RESET, viewModel.state.value.dialogState)
    }
    
    private fun any() = org.mockito.kotlin.any<Int>()
}
