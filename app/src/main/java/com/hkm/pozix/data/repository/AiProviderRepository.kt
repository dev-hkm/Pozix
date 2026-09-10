package com.hkm.pozix.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hkm.pozix.data.model.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.aiProvidersDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_providers")

class AiProviderRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val PROVIDERS_KEY = stringPreferencesKey("providers_json")
    private val ACTIVE_ID_KEY = stringPreferencesKey("active_provider_id")

    fun getProviders(): Flow<List<AiProvider>> =
        context.aiProvidersDataStore.data.map { prefs ->
            val raw = prefs[PROVIDERS_KEY].orEmpty()
            if (raw.isBlank()) return@map emptyList()
            try {
                json.decodeFromString(ListSerializer(AiProvider.serializer()), raw)
            } catch (_: Exception) {
                emptyList()
            }
        }

    fun getActiveProviderId(): Flow<String> =
        context.aiProvidersDataStore.data.map { prefs -> prefs[ACTIVE_ID_KEY].orEmpty() }

    suspend fun getActiveProvider(): AiProvider? {
        val providers = getProviders().first()
        if (providers.isEmpty()) return null
        val activeId = getActiveProviderId().first()
        return providers.firstOrNull { it.id == activeId } ?: providers.first()
    }

    suspend fun saveProvider(provider: AiProvider, setActive: Boolean = false) {
        context.aiProvidersDataStore.edit { prefs ->
            val current = decode(prefs[PROVIDERS_KEY].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == provider.id }
            if (index >= 0) current[index] = provider else current.add(provider)
            prefs[PROVIDERS_KEY] = json.encodeToString(ListSerializer(AiProvider.serializer()), current)
            if (setActive || prefs[ACTIVE_ID_KEY].isNullOrBlank()) {
                prefs[ACTIVE_ID_KEY] = provider.id
            }
        }
    }

    suspend fun deleteProvider(id: String) {
        context.aiProvidersDataStore.edit { prefs ->
            val current = decode(prefs[PROVIDERS_KEY].orEmpty()).filterNot { it.id == id }
            prefs[PROVIDERS_KEY] = json.encodeToString(ListSerializer(AiProvider.serializer()), current)
            if (prefs[ACTIVE_ID_KEY] == id) {
                if (current.isNotEmpty()) prefs[ACTIVE_ID_KEY] = current.first().id
                else prefs.remove(ACTIVE_ID_KEY)
            }
        }
    }

    suspend fun setActiveProvider(id: String) {
        context.aiProvidersDataStore.edit { prefs -> prefs[ACTIVE_ID_KEY] = id }
    }

    /**
     * One-time migration from the old single Gemini API key (settings DataStore)
     * into a provider entry using Gemini's OpenAI-compatible endpoint.
     */
    suspend fun migrateLegacyGeminiKeyIfNeeded() {
        val providers = getProviders().first()
        if (providers.isNotEmpty()) return
        val legacyKey = SettingsRepository(context).getGeminiApiKey().first()
        if (legacyKey.isBlank()) return
        val provider = AiProvider(
            name = "Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            apiKey = legacyKey,
            modelId = "gemini-2.0-flash"
        )
        context.aiProvidersDataStore.edit { prefs ->
            prefs[PROVIDERS_KEY] = json.encodeToString(
                ListSerializer(AiProvider.serializer()), listOf(provider)
            )
            prefs[ACTIVE_ID_KEY] = provider.id
        }
    }

    private fun decode(raw: String): List<AiProvider> {
        if (raw.isBlank()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(AiProvider.serializer()), raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
