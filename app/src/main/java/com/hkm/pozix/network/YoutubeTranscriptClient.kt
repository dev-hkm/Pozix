package com.hkm.pozix.network

import com.hkm.pozix.data.cloud.CloudBackupApi
import com.hkm.pozix.data.model.YoutubeTranscript
import com.hkm.pozix.data.model.YoutubeTranscriptSegment
import com.hkm.pozix.util.YoutubeUrlParser
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class YoutubeTranscriptException(
    val code: String,
    override val message: String
) : IOException(message)

/** Calls the Pozix transcript gateway. The Supadata key never reaches the Android app. */
class YoutubeTranscriptClient(
    private val baseUrl: String = CloudBackupApi.BASE_URL,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetch(
        link: YoutubeUrlParser.Link,
        preferredLanguages: List<String>
    ): Result<YoutubeTranscript> = withContext(ioDispatcher) {
        try {
            val languageList = preferredLanguages
                .map { it.trim().lowercase() }
                .filter { it.matches(Regex("^[a-z]{2,3}(?:-[a-z]{2,4})?$")) }
                .distinct()
                .take(5)
                .ifEmpty { listOf("en") }
            val requestJson = """{"videoId":"${escape(link.videoId)}","languages":[${
                languageList.joinToString(",") { "\"${escape(it)}\"" }
            }],"mode":"native"}"""
            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/v1/youtube/transcript")
                .post(requestJson.toRequestBody(mediaType))
                .header("Accept", "application/json")
                .header("X-Pozix-Client", "android")
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw parseError(response.code, raw)
                val payload = runCatching { json.decodeFromString<GatewayResponse>(raw) }
                    .getOrElse { throw YoutubeTranscriptException("INVALID_RESPONSE", "Transcript service returned invalid data") }
                if (payload.videoId != link.videoId) {
                    throw YoutubeTranscriptException("INVALID_RESPONSE", "Transcript service returned the wrong video")
                }
                val segments = payload.segments.mapIndexedNotNull { index, item ->
                    val text = item.text.trim()
                    if (text.isBlank()) return@mapIndexedNotNull null
                    val start = item.startMs.coerceAtLeast(0L)
                    val end = maxOf(start + 1L, item.endMs)
                    YoutubeTranscriptSegment("s${index + 1}", start, end, text)
                }
                if (segments.isEmpty()) throw YoutubeTranscriptException("NO_TRANSCRIPT", "No readable captions were found for this video")
                if (segments.sumOf { it.text.length.toLong() } > MAX_TEXT_CHARS) {
                    throw YoutubeTranscriptException("TRANSCRIPT_TOO_LARGE", "This transcript is too large for a stable quiz request")
                }
                Result.success(
                    YoutubeTranscript(
                        videoId = link.videoId,
                        canonicalUrl = payload.canonicalUrl ?: link.canonicalUrl,
                        language = payload.language ?: languageList.first(),
                        availableLanguages = payload.availableLanguages,
                        provider = payload.provider ?: "supadata",
                        origin = payload.origin ?: "native_caption",
                        fetchedAt = payload.fetchedAt ?: System.currentTimeMillis(),
                        segments = segments
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: YoutubeTranscriptException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(YoutubeTranscriptException("NETWORK_ERROR", e.message ?: "Could not fetch the YouTube transcript"))
        }
    }

    private fun parseError(status: Int, raw: String): YoutubeTranscriptException {
        val objectValue = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
        val code = objectValue?.get("code")?.jsonPrimitive?.contentOrNull
            ?: when (status) {
                404, 206 -> "NO_TRANSCRIPT"
                401, 403 -> "PROVIDER_NOT_CONFIGURED"
                429 -> "RATE_LIMITED"
                else -> "PROVIDER_ERROR"
            }
        val message = objectValue?.get("error")?.jsonPrimitive?.contentOrNull
            ?: objectValue?.get("message")?.jsonPrimitive?.contentOrNull
            ?: when (code) {
                "NO_TRANSCRIPT" -> "No readable captions were found for this video"
                "RATE_LIMITED" -> "Transcript service rate limit reached"
                "PROVIDER_NOT_CONFIGURED" -> "Transcript service is not configured"
                else -> "Transcript service failed (HTTP $status)"
            }
        return YoutubeTranscriptException(code, message)
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    @Serializable
    private data class GatewayResponse(
        val videoId: String? = null,
        val canonicalUrl: String? = null,
        val language: String? = null,
        val availableLanguages: List<String> = emptyList(),
        val provider: String? = null,
        val origin: String? = null,
        val fetchedAt: Long? = null,
        val segments: List<GatewaySegment> = emptyList()
    )

    @Serializable
    private data class GatewaySegment(
        @SerialName("startMs") val startMs: Long = 0L,
        @SerialName("endMs") val endMs: Long = 0L,
        val text: String = ""
    )

    companion object {
        private const val MAX_TEXT_CHARS = 240_000L
    }
}
