package io.github.ptimulka.miecz.repositories.content

import io.github.ptimulka.miecz.repositories.MnemonicChoiceRepository
import io.github.ptimulka.miecz.repositories.core.UserProgressStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserMnemonicChoiceRepository @Inject constructor(
    private val store: UserProgressStore
) : MnemonicChoiceRepository {

    override fun getMnemonicChoice(sectionId: Int, verseIndex: Int): String? {
        return store.latest.sectionsMap[sectionId]?.mnemonicChoicesMap?.get(verseIndex)
    }

    override fun saveMnemonicChoice(sectionId: Int, verseIndex: Int, choice: String) {
        store.updateSection(sectionId) { it.putMnemonicChoices(verseIndex, choice) }
    }
}
