package io.github.ptimulka.miecz.repositories

interface MnemonicChoiceRepository {
    fun getMnemonicChoice(sectionId: Int, verseIndex: Int): String?
    fun saveMnemonicChoice(sectionId: Int, verseIndex: Int, choice: String)
}
