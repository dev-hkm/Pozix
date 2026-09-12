package com.hkm.pozix.network

import com.hkm.pozix.data.model.ChatMessage
import com.hkm.pozix.data.model.AiProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import com.hkm.pozix.util.ChatImageStorage
import com.hkm.pozix.util.YoutubeTranscriptStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal OpenAI-compatible chat client (BYOK) supporting standard completions and SSE streaming.
 *
 * Works with any provider exposing:
 * - POST {baseUrl}/chat/completions  (OpenAI chat format with stream=true/false)
 * - GET  {baseUrl}/models             (OpenAI models list format)
 *
 * This covers OpenAI, OpenRouter, Together, DeepSeek, Groq, Ollama,
 * LM Studio, Gemini (via its OpenAI-compat endpoint), etc.
 */
object OpenAiCompatClient {

    // Providers are stateless, but resending every old rendered answer makes
    // latency and prompt size grow without bound. Keep recent context bounded;
    // the current turn is always retained in full.
    private const val MAX_CONTEXT_MESSAGES = 18
    private const val MAX_CONTEXT_CHARS = 72_000
    private const val MAX_OLDER_MESSAGE_CHARS = 16_000
    private const val MAX_ATTACHMENT_CHARS = 32_000

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun buildMessagesList(
        history: List<ChatMessage>,
        systemInstructionText: String?
    ): List<ChatMsgRequest> = buildList {
        if (!systemInstructionText.isNullOrBlank()) {
            add(ChatMsgRequest(role = "system", content = JsonPrimitive(systemInstructionText)))
        }
        val boundedHistory = boundHistory(history)
        boundedHistory.forEachIndexed { index, msg ->
            val role = if (msg.role == "user") "user" else "assistant"
            val documentContext = msg.attachments.filter { it.type == com.hkm.pozix.data.model.AttachmentType.DOCUMENT }.joinToString("\n") { doc ->
                val contents = doc.textContent ?: runCatching {
                    com.hkm.pozix.util.DocumentTextReader.read(java.io.File(doc.localPath), doc.name)
                }.getOrNull()
                "\n[Attached source: ${doc.name}]\n" +
                    (contents?.takeIf { it.isNotBlank() }?.take(MAX_ATTACHMENT_CHARS)
                        ?: "[CONTENT UNAVAILABLE. Do not guess from filename; ask for readable TXT, DOCX, or pasted text.]") +
                    "\n[End attached source]\n"
            }
            val reviewContext = msg.quizReviewJson?.let {
                "\n\n[POZIX_COMPLETED_QUIZ_REVIEW_JSON]\n$it\n[/POZIX_COMPLETED_QUIZ_REVIEW_JSON]"
            }.orEmpty()
            val youtubeContext = msg.youtubeSource?.let(YoutubeTranscriptStore::promptBlock).orEmpty()
            val messageText = contextText(msg.text, index == boundedHistory.lastIndex) + documentContext + youtubeContext +
                (msg.quizJson?.let { "\n```json\n$it\n```" } ?: "") + reviewContext
            if (msg.imagePaths.isEmpty()) {
                add(ChatMsgRequest(role = role, content = JsonPrimitive(messageText)))
            } else {
                val parts = buildJsonArray {
                    if (messageText.isNotBlank()) {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", messageText)
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

    private fun boundHistory(history: List<ChatMessage>): List<ChatMessage> {
        if (history.size <= MAX_CONTEXT_MESSAGES && history.sumOf { it.text.length } <= MAX_CONTEXT_CHARS) {
            return history
        }
        val selected = ArrayDeque<ChatMessage>()
        var chars = 0
        history.asReversed().forEachIndexed { reverseIndex, message ->
            if (selected.size >= MAX_CONTEXT_MESSAGES) return@forEachIndexed
            val isCurrent = reverseIndex == 0
            val mustKeepSource = message.attachments.isNotEmpty() || message.youtubeSource != null ||
                message.quizReviewJson != null
            val estimated = message.text.length.coerceAtMost(MAX_OLDER_MESSAGE_CHARS)
            if (isCurrent || mustKeepSource || selected.isEmpty() || chars + estimated <= MAX_CONTEXT_CHARS) {
                selected.addFirst(message)
                chars += estimated
            }
        }
        return selected.toList()
    }

    private fun contextText(text: String, currentTurn: Boolean): String {
        if (currentTurn || text.length <= MAX_OLDER_MESSAGE_CHARS) return text
        return text.take(MAX_OLDER_MESSAGE_CHARS) + "\n[Earlier response truncated for context window]"
    }

    /**
     * Real-time Server-Sent Events (SSE) streaming flow.
     * Emits incremental string and reasoning chunks as they arrive from the model.
     */
    fun chatCompletionStream(
        baseUrl: String,
        apiKey: String,
        model: String,
        history: List<ChatMessage>,
        systemInstructionText: String? = null,
        reasoningEffort: String? = null,
        httpClient: OkHttpClient = client
    ): Flow<StreamChunk> = flow {
        val root = AiProvider.normalizeBaseUrl(baseUrl)
        require(root.startsWith("http://", true) || root.startsWith("https://", true)) {
            "Invalid Base URL"
        }
        require(model.isNotBlank()) { "Model is required" }

        val messages = buildMessagesList(history, systemInstructionText)
        val validReasoning = reasoningEffort?.trim()?.takeIf {
            it.isNotBlank() && it.lowercase() != "off" && it.lowercase() != "default"
        }?.let {
            if (it.lowercase() in listOf("low", "medium", "high")) it.lowercase() else it
        }

        val bodyJson = json.encodeToString(
            ChatRequest(
                model = model,
                messages = messages,
                stream = true,
                reasoningEffort = validReasoning
            )
        )
        val request = Request.Builder()
            .url("$root/chat/completions")
            .post(bodyJson.toRequestBody(mediaType))
            .header("Accept", "text/event-stream")
            .apply {
                if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
            }
            .build()

        val call = httpClient.newCall(request)
        var response: Response? = null

        coroutineScope {
        // Close the socket immediately when Stop is pressed, including blocked reads.
        val cancellation = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() } finally { call.cancel() }
        }
        try {
            response = call.execute()
            if (!response.isSuccessful) {
                val raw = response.body?.string().orEmpty()
                throw IOException("API error (${response.code}): ${friendlyError(raw)}")
            }

            val body = response.body ?: throw IOException("Empty response body")
            if (body.contentType()?.subtype?.contains("event-stream") != true) {
                throw IOException("Provider did not return a realtime event stream")
            }
            val source = body.source()
            var received = false
            var completed = false
            val event = StringBuilder()
            while (true) {
                currentCoroutineContext().ensureActive()
                // Dispatch the final event even if the server omits its trailing blank line.
                val nextLine = source.readUtf8Line()
                if (nextLine == null && event.isEmpty()) break
                val line = nextLine.orEmpty()
                if (line.isEmpty() && event.isNotEmpty()) {
                    val data = event.toString().trim()
                    event.setLength(0)
                    if (data == "[DONE]") {
                        completed = true
                        break
                    }
                    val chunk = json.decodeFromString<ChatStreamResponse>(data)
                    chunk.error?.let { throw IOException(it.message ?: "Stream error") }
                    val choice = chunk.choices?.firstOrNull()
                    if (choice?.finishReason != null) completed = true
                    val text = choice?.delta?.extractText().orEmpty()
                    val reasoning = choice?.delta?.extractReasoning().orEmpty()
                    if (text.isNotEmpty() || reasoning.isNotEmpty()) {
                        received = true
                        emit(StreamChunk(content = text, reasoning = reasoning))
                    }
                    // A terminal event can also carry the last answer delta. Preserve it first.
                    if (choice?.finishReason == "length") throw IOException("Response reached the provider output limit; received text has been preserved. Request fewer questions per batch.")
                    if (choice?.finishReason == "content_filter") throw IOException("Response was filtered by the provider")
                } else if (line.startsWith("data:")) {
                    if (event.isNotEmpty()) event.append('\n')
                    event.append(line.removePrefix("data:").removePrefix(" "))
                }
            }
            if (!received) throw IOException("Provider returned an empty stream")
            if (!completed) throw IOException("Stream disconnected before completion")
        } catch (e: IOException) {
            currentCoroutineContext().ensureActive()
            throw e
        } finally {
            call.cancel()
            response?.close()
            cancellation.cancel()
        }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Non-streaming fallback chat completion.
     */
    suspend fun chatCompletion(
        baseUrl: String,
        apiKey: String,
        model: String,
        history: List<ChatMessage>,
        systemInstructionText: String? = null,
        reasoningEffort: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = AiProvider.normalizeBaseUrl(baseUrl)
            require(root.startsWith("http://", true) || root.startsWith("https://", true)) {
                "Invalid Base URL"
            }
            require(model.isNotBlank()) { "Model is required" }

            val messages = buildMessagesList(history, systemInstructionText)
            val validReasoning = reasoningEffort?.trim()?.takeIf {
                it.isNotBlank() && it.lowercase() != "off" && it.lowercase() != "default"
            }?.let {
                if (it.lowercase() in listOf("low", "medium", "high")) it.lowercase() else it
            }

            val bodyJson = json.encodeToString(
                ChatRequest(
                    model = model,
                    messages = messages,
                    stream = false,
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
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun fetchModels(
        baseUrl: String,
        apiKey: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val root = AiProvider.normalizeBaseUrl(baseUrl)
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
            if (e is CancellationException) throw e
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
private data class ChatStreamResponse(
    val choices: List<StreamChoice>? = null,
    val error: ApiError? = null
)

@Serializable
private data class StreamChoice(
    val delta: StreamDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

data class StreamChunk(
    val content: String = "",
    val reasoning: String = ""
)

@Serializable
private data class StreamDelta(
    val role: String? = null,
    val content: JsonElement? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
    val reasoning: String? = null
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
                }.joinToString("").takeIf { it.isNotEmpty() }
            }
            else -> null
        }
    }

    fun extractReasoning(): String? {
        return reasoningContent?.takeIf { it.isNotEmpty() } ?: reasoning?.takeIf { it.isNotEmpty() }
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
