package com.hkm.pozix.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Utility for compressing, storing, and encoding images for AI Multimodal Chat.
 * Prevents Out of Memory errors and ensures fast, optimized API payloads.
 */
object ChatImageStorage {

    private const val MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 85

    /**
     * Copy, scale down, and compress an image from a content Uri into app-private storage.
     * Returns the absolute path of the saved JPEG file, or null if failed.
     */
    suspend fun saveAndOptimizeImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "ai_chat_images").apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(dir, "img_${UUID.randomUUID()}.jpg")

            // 1. Decode bounds to determine sample size
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext null

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            // Calculate sample size
            var sampleSize = 1
            while (origWidth / sampleSize > MAX_DIMENSION * 2 || origHeight / sampleSize > MAX_DIMENSION * 2) {
                sampleSize *= 2
            }

            // 2. Decode bitmap with inSampleSize
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // 3. Scale precisely if still larger than MAX_DIMENSION
            val finalBitmap = if (sampledBitmap.width > MAX_DIMENSION || sampledBitmap.height > MAX_DIMENSION) {
                val ratio = minOf(
                    MAX_DIMENSION.toFloat() / sampledBitmap.width,
                    MAX_DIMENSION.toFloat() / sampledBitmap.height
                )
                val targetW = (sampledBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (sampledBitmap.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true)
                if (scaled != sampledBitmap) {
                    sampledBitmap.recycle()
                }
                scaled
            } else {
                sampledBitmap
            }

            // 4. Save to target file as JPEG 85%
            FileOutputStream(targetFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            finalBitmap.recycle()

            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convert a local image file to a base64 encoded data URI.
     * e.g. "data:image/jpeg;base64,..."
     */
    suspend fun fileToBase64DataUrl(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) return@withContext null
            val bytes = file.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Safely delete a cached chat image.
     */
    fun deleteImage(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }
}
