package com.hkm.pozix.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.hkm.pozix.data.model.AttachmentType
import com.hkm.pozix.data.model.ChatAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Robust helper for processing and caching user attachments (images, JSON, TXT, documents).
 * Safely copies content URIs immediately into app-internal storage so file handles and permissions
 * never expire.
 */
object ChatAttachmentHelper {

    private const val MAX_IMAGE_DIM = 1280
    private const val JPEG_QUALITY = 85
    private const val MAX_TEXT_FILE_BYTES = 2 * 1024 * 1024 // 2MB max for text ingestion

    /**
     * Copy an attachment from a content Uri into app private cache and inspect its type.
     */
    suspend fun processUri(context: Context, uri: Uri, isExplicitImage: Boolean = false): ChatAttachment? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val fileName = queryFileName(context, uri) ?: ("attachment_" + UUID.randomUUID().toString().take(8))
            val mimeType = contentResolver.getType(uri)?.lowercase() ?: ""
            val extension = fileName.substringAfterLast('.', "").lowercase()

            val isImage = isExplicitImage ||
                    mimeType.startsWith("image/") ||
                    extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")

            val cacheDir = File(context.filesDir, "ai_chat_attachments").apply {
                if (!exists()) mkdirs()
            }

            if (isImage) {
                // 1. Process as Image
                val targetFile = File(cacheDir, "img_${UUID.randomUUID()}.jpg")

                // Copy stream to temp first to avoid multiple stream openings
                val tempRaw = File(cacheDir, "raw_${UUID.randomUUID()}")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempRaw).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null

                try {
                    // Decode bounds
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(tempRaw.absolutePath, options)
                    val origW = options.outWidth
                    val origH = options.outHeight

                    var sampleSize = 1
                    if (origW > 0 && origH > 0) {
                        while (origW / sampleSize > MAX_IMAGE_DIM * 2 || origH / sampleSize > MAX_IMAGE_DIM * 2) {
                            sampleSize *= 2
                        }
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val sampledBitmap = BitmapFactory.decodeFile(tempRaw.absolutePath, decodeOptions)
                    if (sampledBitmap == null) {
                        // Fallback: if decode failed, keep raw file as attachment
                        tempRaw.renameTo(targetFile)
                        return@withContext ChatAttachment(
                            name = fileName,
                            type = AttachmentType.IMAGE,
                            localPath = targetFile.absolutePath,
                            sizeBytes = targetFile.length()
                        )
                    }

                    val finalBitmap = if (sampledBitmap.width > MAX_IMAGE_DIM || sampledBitmap.height > MAX_IMAGE_DIM) {
                        val ratio = minOf(
                            MAX_IMAGE_DIM.toFloat() / sampledBitmap.width,
                            MAX_IMAGE_DIM.toFloat() / sampledBitmap.height
                        )
                        val targetW = (sampledBitmap.width * ratio).toInt().coerceAtLeast(1)
                        val targetH = (sampledBitmap.height * ratio).toInt().coerceAtLeast(1)
                        val scaled = Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true)
                        if (scaled != sampledBitmap) sampledBitmap.recycle()
                        scaled
                    } else {
                        sampledBitmap
                    }

                    FileOutputStream(targetFile).use { out ->
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    }
                    finalBitmap.recycle()

                    ChatAttachment(
                        name = fileName,
                        type = AttachmentType.IMAGE,
                        localPath = targetFile.absolutePath,
                        sizeBytes = targetFile.length()
                    )
                } finally {
                    if (tempRaw.exists()) tempRaw.delete()
                }
            } else {
                // 2. Process as Document / Text / JSON File
                val targetFile = File(cacheDir, "doc_${UUID.randomUUID()}_$fileName")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null

                val size = targetFile.length()
                var textContent: String? = null

                // If file is text-based (JSON, TXT, CSV, MD, XML, etc.) and under 2MB, extract text content
                val isTextBased = extension in listOf("json", "txt", "md", "csv", "xml", "html", "kt", "java", "py", "cpp", "c") ||
                        mimeType.startsWith("text/") ||
                        mimeType.contains("json")

                if (isTextBased && size <= MAX_TEXT_FILE_BYTES) {
                    try {
                        textContent = targetFile.readText(Charsets.UTF_8)
                    } catch (_: Exception) {
                        // Ignore charset errors
                    }
                }

                ChatAttachment(
                    name = fileName,
                    type = AttachmentType.DOCUMENT,
                    localPath = targetFile.absolutePath,
                    sizeBytes = size,
                    textContent = textContent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convert an image file path to Base64 data URL for vision models.
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

    fun deleteAttachment(path: String) {
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.lastPathSegment
    }
}
