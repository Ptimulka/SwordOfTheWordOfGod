package io.github.ptimulka.miecz.repositories

import android.graphics.Bitmap
import io.github.ptimulka.miecz.data.Verse

interface MnemonicRepository {
    fun savePicture(sectionId: Int, verseIndex: Int, bitmap: Bitmap)
    fun loadPicture(sectionId: Int, verseIndex: Int): Bitmap?
    fun deletePicture(sectionId: Int, verseIndex: Int)
    fun clearAllPictures()
    
    fun saveImportedPicture(sectionId: Int, verseIndex: Int, bitmap: Bitmap)
    fun loadImportedPicture(sectionId: Int, verseIndex: Int): Bitmap?
    
    fun loadDefaultPicture(assetName: String): Bitmap?
    
    fun saveChoice(sectionId: Int, verseIndex: Int, choice: ChosenPicture)
    fun loadChoice(sectionId: Int, verseIndex: Int): ChosenPicture?
    
    fun downloadAllImages(sectionId: Int, verses: List<Verse>, assetNames: List<String>): Int
    fun loadActivePicture(sectionId: Int, verseIndex: Int, defaultAssetName: String?): Bitmap?
}
