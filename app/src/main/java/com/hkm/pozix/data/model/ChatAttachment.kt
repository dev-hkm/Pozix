package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class AttachmentType {
    IMAGE,
    DOCUMENT
}

@Serializable
data class ChatAttachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: AttachmentType,
    val localPath: String,
    val sizeBytes: Long = 0,
    val textContent: String? = null // For JSON, TXT, MD, CSV files, the parsed text content
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return ""
            return if (sizeBytes < 1024) {
                "${sizeBytes} B"
            } else if (sizeBytes < 1024 * 1024) {
                String.format("%.1f KB", sizeBytes / 1024.0)
            } else {
                String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
            }
        }
}
