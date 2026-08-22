package io.github.ptimulka.miecz.repositories

import android.graphics.Bitmap
import io.github.ptimulka.miecz.data.Verse

interface MnemonicRepository {
    suspend fun savePicture(sectionId: Int, verseIndex: Int, bitmap: Bitmap)
    suspend fun loadPicture(sectionId: Int, verseIndex: Int): Bitmap?
    fun deletePicture(sectionId: Int, verseIndex: Int)
    fun clearAllPictures()
    
    suspend fun saveImportedPicture(sectionId: Int, verseIndex: Int, bitmap: Bitmap)
    suspend fun loadImportedPicture(sectionId: Int, verseIndex: Int): Bitmap?
    
    suspend fun loadDefaultPicture(assetName: String): Bitmap?
    
    fun saveChoice(sectionId: Int, verseIndex: Int, choice: ChosenPicture)
    fun loadChoice(sectionId: Int, verseIndex: Int): ChosenPicture?
    
    suspend fun downloadAllImages(sectionId: Int, verses: List<Verse>, assetNames: List<String>): Int
    suspend fun loadActivePicture(sectionId: Int, verseIndex: Int, defaultAssetName: String?): Bitmap?
}
