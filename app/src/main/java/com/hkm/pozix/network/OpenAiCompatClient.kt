package com.hkm.pozix.network

import com.hkm.pozix.data.model.ChatMessage
import com.hkm.pozix.util.ChatImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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
        encodeDefaults = false
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
        systemInstructionText: String? = null,
        reasoningEffort: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = baseUrl.trim().trimEnd('/')
            require(root.startsWith("http://", true) || root.startsWith("https://", true)) {
                "Invalid Base URL"
            }
            require(model.isNotBlank()) { "Model is required" }

            val messages = buildList {
                if (!systemInstructionText.isNullOrBlank()) {
                    add(ChatMsgRequest(role = "system", content = JsonPrimitive(systemInstructionText)))
                }
                history.forEach { msg ->
                    val role = if (msg.role == "user") "user" else "assistant"
                    if (msg.imagePaths.isEmpty()) {
                        add(ChatMsgRequest(role = role, content = JsonPrimitive(msg.text)))
                    } else {
                        val parts = buildJsonArray {
                            if (msg.text.isNotBlank()) {
                                add(buildJsonObject {
                                    put("type", "text")
                                    put("text", msg.text)
                                })
                            }
                            for (path in msg.imagePaths) {
                                val dataUrl = ChatImageStorage.fileToBase64DataUrl(path)
                                if (dataUrl != null) {
                                    add(buildJsonObject {
                                        put("type", "image_url")
                                        put("image_url", buildJsonObject {
                                            put("url", dataUrl)
                                        })
                                    })
                                }
                            }
                        }
                        add(ChatMsgRequest(role = role, content = parts))
                    }
                }
            }

            val validReasoning = reasoningEffort?.trim()?.lowercase()?.takeIf {
                it in listOf("low", "medium", "high")
            }

            val bodyJson = json.encodeToString(
                ChatRequest(
                    model = model,
                    messages = messages,
                    reasoningEffort = validReasoning
                )
            )
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
                val text = parsed.choices?.firstOrNull()?.message?.extractText()
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
private data class ChatMsgRequest(
    val role: String,
    val content: JsonElement
)

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMsgRequest>,
    val stream: Boolean = false,
    @SerialName("reasoning_effort") val reasoningEffort: String? = null
)

@Serializable
private data class ChatResponse(
    val choices: List<Choice>? = null,
    val error: ApiError? = null
)

@Serializable
private data class Choice(
    val message: ChatMsgResponse? = null
)

@Serializable
private data class ChatMsgResponse(
    val role: String? = null,
    val content: JsonElement? = null
) {
    fun extractText(): String? {
        val c = content ?: return null
        return when (c) {
            is JsonPrimitive -> c.contentOrNull
            is JsonArray -> {
                c.mapNotNull { item ->
                    if (item is JsonObject) {
                        item["text"]?.jsonPrimitive?.contentOrNull
                    } else null
                }.joinToString("\n").takeIf { it.isNotBlank() }
            }
            else -> null
        }
    }
}

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
