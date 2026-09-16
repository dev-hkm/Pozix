package com.hkm.pozix.util

import android.content.Context
import android.util.Base64
import com.hkm.pozix.data.model.BackupMediaAsset
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Reads a plain quiz JSON file or a small quiz bundle ZIP.
 * A bundle may contain quiz.json plus local image assets. Assets are extracted
 * into an isolated app-private quiz_media bundle directory.
 */
object QuizMediaBundleImporter {
    const val MAX_IMPORT_BYTES = 8 * 1024 * 1024
    private const val MAX_JSON_BYTES = 8 * 1024 * 1024
    private const val MAX_IMAGE_BYTES = 4 * 1024 * 1024
    private const val MAX_TOTAL_IMAGE_BYTES = 24 * 1024 * 1024
    private const val MAX_ENTRIES = 256
    private val safeFileName = Regex("[A-Za-z0-9._-]{1,128}")
    private val bundleAssetReference = Regex("asset://(bundle_[A-Za-z0-9]{1,64})/[A-Za-z0-9._-]{1,128}")

    /**
     * Backup JSON used to preserve only asset:// references, which made a
     * restored quiz unusable on another device. Export the referenced files
     * with strict per-file and total limits matching bundle import limits.
     */
    fun exportReferencedAssets(context: Context, quizJsonContents: Collection<String>): List<BackupMediaAsset> {
        val root = File(context.filesDir, "quiz_media")
        val references = quizJsonContents.flatMap { content ->
            bundleAssetReference.findAll(content).map { it.groupValues[1] to it.value.substringAfterLast('/') }.toList()
        }.distinct()
        var total = 0
        return references.map { (bundleId, fileName) ->
            val file = File(File(root, bundleId), fileName)
            require(file.isFile && file.canonicalFile.parentFile == File(root, bundleId).canonicalFile) {
                "Quiz media file is missing or unsafe: $fileName"
            }
            val bytes = file.readBytes()
            require(bytes.size <= MAX_IMAGE_BYTES) { "Quiz image is too large" }
            total += bytes.size
            require(total <= MAX_TOTAL_IMAGE_BYTES) { "Quiz media is too large" }
            BackupMediaAsset(
                path = "$bundleId/$fileName",
                contentBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            )
        }
    }

    fun restoreAssets(context: Context, assets: List<BackupMediaAsset>) {
        if (assets.isEmpty()) return
        val root = File(context.filesDir, "quiz_media").apply { mkdirs() }
        var total = 0
        assets.forEach { asset ->
            val parts = asset.path.split('/')
            require(parts.size == 2 && bundleAssetReference.matches("asset://${asset.path}")) { "Invalid backup media path" }
            val bytes = try {
                Base64.decode(asset.contentBase64, Base64.NO_WRAP)
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid backup media data", error)
            }
            require(bytes.size <= MAX_IMAGE_BYTES) { "Quiz image is too large" }
            total += bytes.size
            require(total <= MAX_TOTAL_IMAGE_BYTES) { "Quiz media is too large" }
            val bundle = File(root, parts[0]).apply { mkdirs() }
            val target = File(bundle, parts[1])
            require(target.canonicalFile.parentFile == bundle.canonicalFile) { "Invalid backup media path" }
            val temporary = File(bundle, ".${target.name}.${UUID.randomUUID()}.tmp")
            try {
                FileOutputStream(temporary).use { it.write(bytes) }
                if (!temporary.renameTo(target)) temporary.copyTo(target, overwrite = true)
            } finally {
                temporary.delete()
            }
        }
    }

    fun validateBackupAssets(assets: List<BackupMediaAsset>) {
        require(assets.map { it.path }.distinct().size == assets.size) { "Duplicate backup media paths" }
        var total = 0
        assets.forEach { asset ->
            require(bundleAssetReference.matches("asset://${asset.path}")) { "Invalid backup media path" }
            val bytes = try {
                Base64.decode(asset.contentBase64, Base64.NO_WRAP)
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid backup media data", error)
            }
            require(bytes.size <= MAX_IMAGE_BYTES) { "Quiz image is too large" }
            total += bytes.size
            require(total <= MAX_TOTAL_IMAGE_BYTES) { "Quiz media is too large" }
        }
    }

    fun cleanupUnusedBundles(context: Context, quizJsonContents: Collection<String>) {
        val referenced = quizJsonContents.flatMap { content ->
            bundleAssetReference.findAll(content).map { it.groupValues[1] }.toList()
        }.toSet()
        val root = File(context.filesDir, "quiz_media")
        root.listFiles()?.filter { it.isDirectory && it.name.startsWith("bundle_") && it.name !in referenced }
            ?.forEach { it.deleteRecursively() }
    }

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

        val bundleId = "bundle_${UUID.randomUUID().toString().replace("-", "").take(24)}"
        val mediaRoot = File(context.filesDir, "quiz_media")
        val bundleRoot = File(mediaRoot, bundleId).apply { mkdirs() }
        var jsonText: String? = null
        var fallbackJson: String? = null
        var imageBytes = 0
        var entryCount = 0
        val extractedNames = hashSetOf<String>()

        try {
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
                        require(extractedNames.add(fileName)) { "Quiz bundle contains duplicate media filenames" }
                        val target = File(bundleRoot, fileName)
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
            ?.normalizeAssetReferences(bundleId)
            ?: throw IllegalArgumentException("${displayName.ifBlank { "Bundle" }} does not contain quiz JSON")
        } catch (error: Exception) {
            bundleRoot.deleteRecursively()
            throw error
        }
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

    private fun String.normalizeAssetReferences(bundleId: String): String =
        replace(Regex("asset://(?:[A-Za-z0-9._-]+/)*([A-Za-z0-9._-]{1,128})"), "asset://$bundleId/$1")
}
