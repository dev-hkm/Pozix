package com.hkm.pozix.network

import com.hkm.pozix.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal OpenAI-compatible chat client (BYOK).
 *
 * Works with any provider exposing:
 * - POST {baseUrl}/chat/completions  (OpenAI chat format)
 * - GET  {baseUrl}/models             (OpenAI models list format)
 *
 * This covers OpenAI, OpenRouter, Together, DeepSeek, Groq, Ollama,
 * LM Studio, Gemini (via its OpenAI-compat endpoint), etc.
 */
object OpenAiCompatClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun chatCompletion(
        baseUrl: String,
        apiKey: String,
        model: String,
        history: List<ChatMessage>,
        systemInstructionText: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = baseUrl.trim().trimEnd('/')
            require(root.startsWith("http://", true) || root.startsWith("https://", true)) {
                "Invalid Base URL"
            }
            require(model.isNotBlank()) { "Model is required" }

            val messages = buildList {
                if (!systemInstructionText.isNullOrBlank()) {
                    add(ChatMsg(role = "system", content = systemInstructionText))
                }
                history.forEach { msg ->
                    val role = if (msg.role == "user") "user" else "assistant"
                    add(ChatMsg(role = role, content = msg.text))
                }
            }

            val bodyJson = json.encodeToString(ChatRequest(model = model, messages = messages))
            val request = Request.Builder()
                .url("$root/chat/completions")
                .post(bodyJson.toRequestBody(mediaType))
                .apply {
                    if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
                }
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("API error (${response.code}): ${friendlyError(raw)}")
                    )
                }
                if (raw.isBlank()) {
                    return@withContext Result.failure(IOException("Empty response body"))
                }
                val parsed = try {
                    json.decodeFromString<ChatResponse>(raw)
                } catch (e: Exception) {
                    return@withContext Result.failure(
                        IOException("Unexpected response format: ${e.message}")
                    )
                }
                val text = parsed.choices?.firstOrNull()?.message?.content
                if (text.isNullOrBlank()) {
                    val err = parsed.error?.message
                    return@withContext Result.failure(
                        IOException(err?.takeIf { it.isNotBlank() } ?: "No text found in response")
                    )
                }
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchModels(
        baseUrl: String,
        apiKey: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val root = baseUrl.trim().trimEnd('/')
            require(root.startsWith("http://", true) || root.startsWith("https://", true)) {
                "Invalid Base URL"
            }
            val request = Request.Builder()
                .url("$root/models")
                .get()
                .apply {
                    if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
                }
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("API error (${response.code}): ${friendlyError(raw)}")
                    )
                }
                if (raw.isBlank()) {
                    return@withContext Result.failure(IOException("Empty response body"))
                }
                // Standard OpenAI shape: {"data":[{"id":"...","object":"model",...}]}
                val parsed = try {
                    json.decodeFromString<ModelListResponse>(raw)
                } catch (e: Exception) {
                    return@withContext Result.failure(
                        IOException("This endpoint did not return an OpenAI-style model list")
                    )
                }
                val ids = parsed.data
                    ?.mapNotNull { it.id?.takeIf { id -> id.isNotBlank() } }
                    ?.distinct()
                    ?.sorted()
                    .orEmpty()
                if (ids.isEmpty()) {
                    return@withContext Result.failure(
                        IOException("No models returned by this endpoint")
                    )
                }
                Result.success(ids)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun friendlyError(raw: String): String {
        if (raw.isBlank()) return "Unknown error"
        return try {
            val parsed = json.decodeFromString<ChatResponse>(raw)
            parsed.error?.message?.takeIf { it.isNotBlank() } ?: raw.take(300)
        } catch (_: Exception) {
            raw.take(300)
        }
    }
}

@Serializable
private data class ChatMsg(
    val role: String,
    val content: String
)

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMsg>,
    val stream: Boolean = false
)

@Serializable
private data class ChatResponse(
    val choices: List<Choice>? = null,
    val error: ApiError? = null
)

@Serializable
private data class Choice(
    val message: ChatMsg? = null
)

@Serializable
private data class ApiError(
    val message: String? = null
)

@Serializable
private data class ModelListResponse(
    val data: List<ModelEntry>? = null
)

@Serializable
private data class ModelEntry(
    val id: String? = null
)
