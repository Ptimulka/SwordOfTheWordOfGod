package io.github.ptimulka.miecz.helpers

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import io.github.ptimulka.miecz.data.Verse
import java.io.File
import java.io.FileOutputStream

object AnkiExporter {

    fun exportToCsv(context: Context, sectionName: String, verses: List<Verse>) {
        if (verses.isEmpty()) {
            Toast.makeText(context, "Brak wersetów do eksportu", Toast.LENGTH_SHORT).show()
            return
        }

        val csvContent = buildCsv(verses)
        val safeName = sectionName.replace(" ", "_")
        val fileName = "wersety_${safeName}.csv"

        val uri = saveCsv(context, fileName, csvContent)

        if (uri == null) {
            Toast.makeText(context, "Błąd zapisu pliku CSV", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Zapisano w Pobranych: $fileName", Toast.LENGTH_LONG).show()
        }
    }

    private fun buildCsv(verses: List<Verse>): String {
        val sb = StringBuilder()

        for (v in verses) {
            val front = "${v.book} ${v.chapter},${v.number}"
            val back = v.text.replace("_", " ").replace("*", "")
            sb.append("$front\t$back\n")
        }

        return sb.toString()
    }

    private fun saveCsv(context: Context, fileName: String, content: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveCsvApi29(context, fileName, content)
        } else {
            saveCsvLegacy(context, fileName, content)
        }
    }

    // Android 10+ (API 29+)
    private fun saveCsvApi29(context: Context, fileName: String, content: String): Uri? {
        return try {
            val resolver = context.contentResolver

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null

            resolver.openOutputStream(uri)?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            }

            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Android 7–9 (API 24–28)
    private fun saveCsvLegacy(context: Context, fileName: String, content: String): Uri? {
        return try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloads, fileName)

            FileOutputStream(file).use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }

            val uri = Uri.fromFile(file)
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))

            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
