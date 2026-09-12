package com.hkm.pozix.util

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Reads a plain quiz JSON file or a small quiz bundle ZIP.
 * A bundle may contain quiz.json plus local image assets. Assets are extracted
 * into the app-private quiz_media directory and referenced by asset://filename.
 */
object QuizMediaBundleImporter {
    const val MAX_IMPORT_BYTES = 8 * 1024 * 1024
    private const val MAX_JSON_BYTES = 8 * 1024 * 1024
    private const val MAX_IMAGE_BYTES = 4 * 1024 * 1024
    private const val MAX_TOTAL_IMAGE_BYTES = 24 * 1024 * 1024
    private const val MAX_ENTRIES = 256
    private val safeFileName = Regex("[A-Za-z0-9._-]{1,128}")

    fun readBounded(input: InputStream, maxBytes: Int = MAX_IMPORT_BYTES): ByteArray {
        val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "File is larger than ${maxBytes / (1024 * 1024)} MB" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    fun decode(context: Context, bytes: ByteArray, displayName: String): String {
        require(bytes.isNotEmpty()) { "The selected file is empty" }
        if (!isZip(bytes)) {
            val text = bytes.toString(Charsets.UTF_8)
            require(text.isNotBlank()) { "The selected file is empty" }
            return text
        }

        val mediaRoot = File(context.filesDir, "quiz_media").apply { mkdirs() }
        var jsonText: String? = null
        var fallbackJson: String? = null
        var imageBytes = 0
        var entryCount = 0

        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                require(entryCount <= MAX_ENTRIES) { "Quiz bundle contains too many files" }
                val entryName = entry.name.replace('\\', '/')
                val fileName = entryName.substringAfterLast('/')
                if (entry.isDirectory || fileName.isBlank() || !safeFileName.matches(fileName)) {
                    zip.closeEntry()
                    continue
                }
                val lowerName = fileName.lowercase()
                when {
                    lowerName.endsWith(".json") -> {
                        val candidate = readBounded(zip, MAX_JSON_BYTES).toString(Charsets.UTF_8)
                        if (lowerName.substringAfterLast('/') == "quiz.json") jsonText = candidate
                        else if (fallbackJson == null) fallbackJson = candidate
                    }
                    isImageName(lowerName) -> {
                        val target = File(mediaRoot, fileName)
                        val extracted = extractImage(zip, target, MAX_IMAGE_BYTES)
                        imageBytes += extracted
                        require(imageBytes <= MAX_TOTAL_IMAGE_BYTES) { "Quiz media is too large" }
                    }
                }
                zip.closeEntry()
            }
        }

        return (jsonText ?: fallbackJson)
            ?.takeIf { it.isNotBlank() }
            ?.normalizeAssetReferences()
            ?: throw IllegalArgumentException("${displayName.ifBlank { "Bundle" }} does not contain quiz JSON")
    }

    private fun extractImage(input: InputStream, target: File, maxBytes: Int): Int {
        val temp = File(target.parentFile, ".${target.name}.tmp")
        var total = 0
        try {
            FileOutputStream(temp).use { output ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= maxBytes) { "Quiz image is too large" }
                    output.write(buffer, 0, count)
                }
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            return total
        } finally {
            temp.delete()
        }
    }

    private fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4b.toByte() &&
            (bytes[2] == 0x03.toByte() || bytes[2] == 0x05.toByte() || bytes[2] == 0x07.toByte())

    private fun isImageName(name: String): Boolean =
        name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
            name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".svg")

    private fun String.normalizeAssetReferences(): String =
        replace(Regex("asset://(?:[A-Za-z0-9._-]+/)+([A-Za-z0-9._-]{1,128})"), "asset://$1")
}
