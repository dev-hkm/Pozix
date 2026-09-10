package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AiProvider(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val reasoningEffort: String? = null
) {
    /** Normalized base URL without trailing slash, e.g. https://api.openai.com/v1 */
    fun normalizedBaseUrl(): String = baseUrl.trim().trimEnd('/')

    companion object {
        fun normalizeBaseUrl(input: String): String {
            var url = input.trim().trimEnd('/')
            // Allow pasting full endpoint URLs: strip known suffixes back to the API root.
            val suffixes = listOf("/chat/completions", "/completions", "/models")
            for (suffix in suffixes) {
                if (url.endsWith(suffix, ignoreCase = true)) {
                    url = url.dropLast(suffix.length).trimEnd('/')
                    break
                }
            }
            return url
        }

        fun isPlausibleBaseUrl(input: String): Boolean {
            val url = input.trim()
            return url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true)
        }
    }
}
