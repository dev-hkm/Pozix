package com.hkm.pozix.data.cloud

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CloudBackupApi(
    private val baseUrl: String = BASE_URL,
    private val client: OkHttpClient = OkHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun getSalt(token: String): Result<String> = withContext(ioDispatcher) {
        get("/v1/backups/$token/salt")
            .mapCatching { response -> response.stringField("salt") }
    }

    suspend fun upload(
        token: String,
        payload: EncryptedBackup,
        replace: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        val requestBody = buildJsonObject {
            if (!replace) put("token", token)
            put("verifier", payload.verifier)
            put("salt", payload.salt)
            put("ciphertext", payload.ciphertext)
        }.toString()

        request(
            method = if (replace) "PUT" else "POST",
            path = if (replace) "/v1/backups/$token" else "/v1/backups",
            requestBody = requestBody
        ).map { }
    }

    suspend fun restore(
        token: String,
        verifier: String
    ): Result<EncryptedBackup> = withContext(ioDispatcher) {
        val requestBody = buildJsonObject {
            put("verifier", verifier)
        }.toString()

        request(
            method = "POST",
            path = "/v1/backups/$token/restore",
            requestBody = requestBody
        ).mapCatching { response ->
            EncryptedBackup(
                verifier = verifier,
                salt = response.stringField("salt"),
                ciphertext = response.stringField("ciphertext")
            )
        }
    }

    suspend fun createShare(quizJson: String): Result<String> = withContext(ioDispatcher) {
        val requestBody = buildJsonObject {
            put("quizJson", quizJson)
        }.toString()

        request(
            method = "POST",
            path = "/v1/shares",
            requestBody = requestBody
        ).mapCatching { response ->
            "$baseUrl/v1/shares/${response.stringField("id")}"
        }
    }

    suspend fun loadShare(url: String): Result<String> = withContext(ioDispatcher) {
        runCatching {
            require(url.startsWith("$baseUrl/v1/shares/")) {
                "This is not a Pozix share link"
            }

            client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException(errorMessage(responseBody, "This share link is unavailable or expired"))
                }
                responseBody.stringField("quizJson")
            }
        }
    }

    private fun get(path: String): Result<String> = runCatching {
        client.newCall(Request.Builder().url(baseUrl + path).get().build()).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(errorMessage(responseBody, "Cloud service error"))
            }
            responseBody
        }
    }

    private fun request(
        method: String,
        path: String,
        requestBody: String
    ): Result<String> = runCatching {
        val request = Request.Builder()
            .url(baseUrl + path)
            .method(method, requestBody.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(errorMessage(responseBody, "Cloud service error"))
            }
            responseBody
        }
    }

    private fun String.stringField(name: String): String {
        return json.parseToJsonElement(this)
            .jsonObject[name]
            ?.jsonPrimitive
            ?.content
            ?: throw IOException("Cloud response is missing $name")
    }

    private fun errorMessage(responseBody: String, fallback: String): String {
        return runCatching { responseBody.stringField("error") }
            .getOrDefault(fallback)
    }

    companion object {
        const val BASE_URL = "https://pozix-cloud-backup.hoangkhanhminh2005-vl.workers.dev"
    }
}
