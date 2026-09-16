package com.hkm.pozix.util

import java.net.URI

/** Parses one supported YouTube video URL without accepting playlists or channels as sources. */
object YoutubeUrlParser {
    data class Link(
        val videoId: String,
        val canonicalUrl: String,
        val rawUrl: String
    )

    private val urlPattern = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)
    private val videoIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
    private val supportedHosts = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtube-nocookie.com",
        "www.youtube-nocookie.com",
        "youtu.be"
    )

    fun find(text: String): Link? {
        return urlPattern.findAll(text)
            .map { it.value.trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '}') }
            .firstNotNullOfOrNull(::parse)
    }

    fun containsYoutubeHost(text: String): Boolean {
        return urlPattern.findAll(text).any { candidate ->
            runCatching {
                val host = URI(candidate.value).host?.lowercase()
                host != null && host in supportedHosts
            }
                .getOrDefault(false)
        }
    }

    fun parse(rawUrl: String): Link? {
        val cleanUrl = rawUrl.trim().trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '}')
        val uri = runCatching { URI(cleanUrl) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", true) && !uri.scheme.equals("http", true)) return null
        val host = uri.host?.lowercase() ?: return null
        if (host !in supportedHosts) return null

        val pathSegments = uri.path.orEmpty().trim('/').split('/').filter { it.isNotBlank() }
        val firstPathSegment = pathSegments.firstOrNull()?.lowercase()
        val id = when {
            host == "youtu.be" -> pathSegments.firstOrNull()
            uri.path?.trimEnd('/').equals("/watch", true) -> queryParameter(uri.rawQuery, "v")
            firstPathSegment != null && firstPathSegment in setOf("shorts", "live", "embed", "v") ->
                pathSegments.getOrNull(1)
            else -> null
        }?.trim()

        if (id.isNullOrBlank() || !videoIdPattern.matches(id)) return null
        return Link(
            videoId = id,
            canonicalUrl = "https://www.youtube.com/watch?v=$id",
            rawUrl = cleanUrl
        )
    }

    fun removeFromPrompt(text: String, link: Link): String {
        return text.replace(link.rawUrl, " ", ignoreCase = false)
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    private fun queryParameter(query: String?, key: String): String? = query
        ?.split('&')
        ?.firstOrNull { it.substringBefore('=') == key }
        ?.substringAfter('=', "")
}
