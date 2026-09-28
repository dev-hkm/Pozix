package com.hkm.pozix.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hkm.pozix.util.SecretCipher
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

private val Context.pozixAiAccountStore by preferencesDataStore("pozix_ai_account")

data class PozixAiAccount(val displayName: String, val username: String)

class PozixAiAccountRepository(private val context: Context) {
    companion object {
        const val WORKER_URL = "https://pozix-ai-gateway.cloud-backup-worker.workers.dev"
        const val DEV_UNLOCK_PREFS = "pozix_developer_options"
        const val DEV_UNLOCKED = "unlocked"
        val builtInProviders = listOf(
            com.hkm.pozix.data.model.AiProvider(
                id = "builtin-generalcompute", name = "MiniMax M2.7 · General Compute",
                baseUrl = "$WORKER_URL/v1/generalcompute", apiKey = "", modelId = "minimax-m2.7"
            ),
            com.hkm.pozix.data.model.AiProvider(
                id = "builtin-openai", name = "GPT-6 Luna · OpenAI",
                baseUrl = "$WORKER_URL/v1/openai", apiKey = "", modelId = "gpt-6-luna"
            ),
            com.hkm.pozix.data.model.AiProvider(
                id = "builtin-openrouter", name = "GLM 5.3 Flash · OpenRouter",
                baseUrl = "$WORKER_URL/v1/openrouter", apiKey = "", modelId = "z-ai/glm-5.3-flash"
            )
        )
    }

    private val tokenKey = stringPreferencesKey("session_token")
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun token(): String = context.pozixAiAccountStore.data.first()[tokenKey]?.let {
        runCatching { SecretCipher.decrypt(it) }.getOrDefault("")
    }.orEmpty()

    suspend fun current(): PozixAiAccount? {
        val token = token().takeIf { it.isNotBlank() } ?: return null
        val body = request("GET", "/v1/auth/me", token, null)
        return PozixAiAccount(body.string("displayName"), body.string("username"))
    }

    suspend fun register(displayName: String, username: String, password: String): PozixAiAccount =
        signIn("/v1/auth/register", buildJsonObject {
            put("displayName", displayName); put("username", username); put("password", password)
        })

    suspend fun login(username: String, password: String): PozixAiAccount =
        signIn("/v1/auth/login", buildJsonObject { put("username", username); put("password", password) })

    private suspend fun signIn(path: String, payload: kotlinx.serialization.json.JsonObject): PozixAiAccount {
        val result = request("POST", path, null, payload.toString())
        val token = result.string("token")
        require(token.isNotBlank()) { "Worker did not return a session token." }
        context.pozixAiAccountStore.edit { it[tokenKey] = SecretCipher.encrypt(token) }
        return PozixAiAccount(result.string("displayName"), result.string("username"))
    }

    suspend fun logout() {
        val token = token()
        if (token.isNotBlank()) runCatching { request("POST", "/v1/auth/logout", token, "{}") }
        context.pozixAiAccountStore.edit { it.remove(tokenKey) }
    }

    suspend fun deleteAccount() {
        val token = token()
        request("POST", "/v1/account/delete", token, "{}")
        context.pozixAiAccountStore.edit { it.remove(tokenKey) }
    }

    private suspend fun request(method: String, path: String, token: String?, body: String?): kotlinx.serialization.json.JsonObject =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val builder = Request.Builder().url(WORKER_URL + path).header("Accept", "application/json")
            if (token != null) builder.header("Authorization", "Bearer $token")
            if (method == "GET") builder.get()
            else builder.method(method, body.orEmpty().toRequestBody(mediaType)).header("Content-Type", "application/json")
            client.newCall(builder.build()).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val parsed = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
                if (!response.isSuccessful) throw IOException(parsed?.get("error")?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull ?: "AI account request failed (${response.code}).")
                parsed ?: throw IOException("Invalid response from Pozix AI Worker.")
            }
        }

    private fun kotlinx.serialization.json.JsonObject.string(key: String) =
        this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
}
