package com.hkm.pozix.util

import android.content.Context
import com.hkm.pozix.data.model.YoutubeSourceReference
import com.hkm.pozix.data.model.YoutubeTranscript
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Persists transcript payloads outside DataStore and keeps only a compact reference in chat history. */
object YoutubeTranscriptStore {
    private const val DIRECTORY = "youtube_transcripts"
    private const val MAX_BYTES = 2_000_000L

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun save(context: Context, transcript: YoutubeTranscript): YoutubeSourceReference {
        val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        val filename = sha256("${transcript.videoId}|${transcript.language}") + ".json"
        val target = File(directory, filename)
        val temporary = File(directory, "$filename.tmp")
        temporary.writeText(json.encodeToString(transcript), StandardCharsets.UTF_8)
        check(temporary.length() <= MAX_BYTES) { "Transcript is too large to store safely" }
        check(temporary.renameTo(target) || temporary.copyTo(target, overwrite = true).let { temporary.delete(); true }) {
            "Could not persist transcript"
        }
        return YoutubeSourceReference(
            videoId = transcript.videoId,
            canonicalUrl = transcript.canonicalUrl,
            language = transcript.language,
            segmentCount = transcript.segments.size,
            provider = transcript.provider,
            origin = transcript.origin,
            transcriptPath = target.absolutePath,
            fetchedAt = transcript.fetchedAt
        )
    }

    fun read(reference: YoutubeSourceReference): YoutubeTranscript? {
        val file = File(reference.transcriptPath)
        if (!file.isFile || file.length() <= 0L || file.length() > MAX_BYTES) return null
        return runCatching {
            json.decodeFromString<YoutubeTranscript>(file.readText(StandardCharsets.UTF_8))
        }.getOrNull()
    }

    fun delete(reference: YoutubeSourceReference) {
        runCatching { File(reference.transcriptPath).delete() }
    }

    fun promptBlock(reference: YoutubeSourceReference): String {
        val transcript = read(reference) ?: return """

[POZIX_YOUTUBE_SOURCE]
video_id=${reference.videoId}
source_url=${reference.canonicalUrl}
status=TRANSCRIPT_UNAVAILABLE
Do not create a quiz from the video title or URL. Ask the user for a readable transcript.
[/POZIX_YOUTUBE_SOURCE]
""".trimIndent()

        val body = transcript.segments.joinToString("\n") { segment ->
            "[segment id=${segment.id} start_ms=${segment.startMs} end_ms=${segment.endMs}] ${segment.text}"
        }
        return """

[POZIX_YOUTUBE_SOURCE]
video_id=${transcript.videoId}
source_url=${transcript.canonicalUrl}
language=${transcript.language}
caption_origin=${transcript.origin}
timestamp_rule=Copy start_ms/end_ms and segment ids from this source. Never invent timestamps.
segments:
$body
[/POZIX_YOUTUBE_SOURCE]
""".trimIndent()
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
