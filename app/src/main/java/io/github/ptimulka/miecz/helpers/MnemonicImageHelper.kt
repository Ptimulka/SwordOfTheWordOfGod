package io.github.ptimulka.miecz.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import io.github.ptimulka.miecz.screens.mnemonic.DrawingStroke
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.core.graphics.scale

object MnemonicImageHelper {

    private const val MAX_IMPORT_DIMENSION = 2048

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            val largestSide = maxOf(bounds.outWidth, bounds.outHeight)
            while (largestSide / sampleSize > MAX_IMPORT_DIMENSION) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    fun cropImageTo34Ratio(
        bitmap: Bitmap,
        offsetX: Float,
        offsetY: Float,
        scale: Float,
        rotation: Float,
        canvasWidth: Int,
        canvasHeight: Int
    ): Bitmap {
        val targetWidth = 1200
        val targetHeight = 1600

        val workBitmap = createBitmap(canvasWidth, canvasHeight)
        val workCanvas = Canvas(workBitmap)

        workCanvas.drawColor(Color.WHITE)
        workCanvas.withSave {

            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            translate(centerX + offsetX, centerY + offsetY)
            rotate(rotation, 0f, 0f)
            scale(scale, scale, 0f, 0f)
            translate(-bitmap.width / 2f, -bitmap.height / 2f)

            drawBitmap(bitmap, 0f, 0f, null)
        }

        return workBitmap.scale(targetWidth, targetHeight)
    }

    fun renderDrawingToBitmap(
        background: Bitmap?,
        strokes: List<DrawingStroke>,
        canvasWidth: Int,
        canvasHeight: Int
    ): Bitmap {
        val bitmap = createBitmap(canvasWidth, canvasHeight)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        background?.let { bg ->
            val scale = minOf(canvasWidth.toFloat() / bg.width, canvasHeight.toFloat() / bg.height)
            val dstW = (bg.width * scale).toInt()
            val dstH = (bg.height * scale).toInt()
            val dstX = (canvasWidth - dstW) / 2
            val dstY = (canvasHeight - dstH) / 2
            canvas.drawBitmap(
                bg.scale(dstW, dstH),
                dstX.toFloat(), dstY.toFloat(), null
            )
        }

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            paint.color = stroke.color.toArgb()
            paint.strokeWidth = stroke.strokeWidthPx
            val path = Path()
            path.moveTo(stroke.points.first().x * canvasWidth, stroke.points.first().y * canvasHeight)
            stroke.points.drop(1).forEach { 
                path.lineTo(it.x * canvasWidth, it.y * canvasHeight) 
            }
            canvas.drawPath(path, paint)
        }
        
        // Final result should be 1200x1600 as per project standards
        return bitmap.scale(1200, 1600)
    }
    
    private fun androidx.compose.ui.graphics.Color.toArgb(): Int {
        return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
               ((this.red * 255.0f + 0.5f).toInt() shl 16) or
               ((this.green * 255.0f + 0.5f).toInt() shl 8) or
               (this.blue * 255.0f + 0.5f).toInt()
    }
}
