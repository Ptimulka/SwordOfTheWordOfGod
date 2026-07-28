package io.github.ptimulka.miecz.repositories

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import io.github.ptimulka.miecz.data.Verse
import java.io.File

enum class ChosenPicture { NONE, DEFAULT, USER, IMPORTED }

class MnemonicPicturesRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mnemonic_choices", Context.MODE_PRIVATE)

    // ── User-drawn picture ────────────────────────────────────────────────────

    private fun pictureFile(sectionId: Int, verseIndex: Int): File {
        val dir = File(context.filesDir, "mnemonic")
        dir.mkdirs()
        return File(dir, "section_${sectionId}_verse_${verseIndex}.png")
    }

    fun savePicture(sectionId: Int, verseIndex: Int, bitmap: Bitmap) {
        pictureFile(sectionId, verseIndex).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    fun loadPicture(sectionId: Int, verseIndex: Int): Bitmap? {
        val file = pictureFile(sectionId, verseIndex)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    fun deletePicture(sectionId: Int, verseIndex: Int) {
        pictureFile(sectionId, verseIndex).delete()
    }

    /** Deletes every drawn and imported picture along with the saved picture choices. */
    fun clearAllPictures() {
        prefs.edit().clear().apply()
        File(context.filesDir, "mnemonic").deleteRecursively()
        File(context.filesDir, "mnemonic_imported").deleteRecursively()
    }

    // ── Imported picture ──────────────────────────────────────────────────────

    private fun importedPictureFile(sectionId: Int, verseIndex: Int): File {
        val dir = File(context.filesDir, "mnemonic_imported")
        dir.mkdirs()
        return File(dir, "section_${sectionId}_verse_${verseIndex}.png")
    }

    fun saveImportedPicture(sectionId: Int, verseIndex: Int, bitmap: Bitmap) {
        importedPictureFile(sectionId, verseIndex).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    fun loadImportedPicture(sectionId: Int, verseIndex: Int): Bitmap? {
        val file = importedPictureFile(sectionId, verseIndex)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    // ── Default AI-generated picture ─────────────────────────────────────────

    /**
     * Asset filename pattern: default_mnemonics/section01_01_1Kor13-13.webp
     * The [assetName] is the bare filename without directory, e.g. "section01_01_1Kor13-13.webp".
     */
    fun loadDefaultPicture(assetName: String): Bitmap? {
        return try {
            context.assets.open("default_mnemonics/$assetName").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    // ── Chosen picture preference ─────────────────────────────────────────────

    private fun choiceKey(sectionId: Int, verseIndex: Int) = "choice_${sectionId}_${verseIndex}"

    fun saveChoice(sectionId: Int, verseIndex: Int, choice: ChosenPicture) {
        prefs.edit().putString(choiceKey(sectionId, verseIndex), choice.name).apply()
    }

    // Returns null when no choice has ever been saved (distinct from NONE = explicitly "no picture")
    fun loadChoice(sectionId: Int, verseIndex: Int): ChosenPicture? {
        val raw = prefs.getString(choiceKey(sectionId, verseIndex), null) ?: return null
        return try { ChosenPicture.valueOf(raw) } catch (_: Exception) { null }
    }

    // ── Gallery export ────────────────────────────────────────────────────────

    fun downloadAllImages(
        sectionId: Int,
        verses: List<Verse>,
        assetNames: List<String>
    ): Int {
        var count = 0
        verses.forEachIndexed { index, verse ->
            val bitmap = loadActivePicture(sectionId, index, assetNames.getOrNull(index))
                ?: return@forEachIndexed
            val sigla = "${verse.book}${verse.chapter}-${verse.number.replace(".", "-")}"
            val filename = "mnemonic_${sectionId}_${sigla}.png"
            if (saveToGallery(bitmap, filename)) count++
        }
        return count
    }

    private fun saveToGallery(bitmap: Bitmap, filename: String): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MnemonicPictures")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
            ) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // ── Active bitmap (used by riddle hint) ───────────────────────────────────

    /**
     * Returns the bitmap that should be shown as a hint: user picture if chosen,
     * default if chosen, imported if chosen, null if NONE explicitly. When nothing has been chosen yet,
     * auto-selects: default image if available, otherwise imported, otherwise user, otherwise null.
     */
    fun loadActivePicture(sectionId: Int, verseIndex: Int, defaultAssetName: String?): Bitmap? {
        val userBitmap by lazy { loadPicture(sectionId, verseIndex) }
        val importedBitmap by lazy { loadImportedPicture(sectionId, verseIndex) }
        val defaultBitmap by lazy { defaultAssetName?.let { loadDefaultPicture(it) } }
        return when (loadChoice(sectionId, verseIndex)) {
            null -> defaultBitmap ?: importedBitmap ?: userBitmap
            ChosenPicture.DEFAULT -> defaultBitmap ?: importedBitmap ?: userBitmap
            ChosenPicture.IMPORTED -> importedBitmap ?: userBitmap ?: defaultBitmap
            ChosenPicture.USER -> userBitmap ?: importedBitmap ?: defaultBitmap
            ChosenPicture.NONE -> null
        }
    }
}
