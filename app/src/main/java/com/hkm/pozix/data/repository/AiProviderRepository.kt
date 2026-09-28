package com.hkm.pozix.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hkm.pozix.data.model.AiProvider
import com.hkm.pozix.util.SecretCipher
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

    suspend fun ensureBuiltInProviders() {
        context.aiProvidersDataStore.edit { prefs ->
            val current = decode(prefs[PROVIDERS_KEY].orEmpty()).toMutableList()
            com.hkm.pozix.data.repository.PozixAiAccountRepository.builtInProviders.forEach { builtin ->
                if (current.none { it.id == builtin.id }) current.add(0, builtin)
            }
            prefs[PROVIDERS_KEY] = encode(current)
            val active = prefs[ACTIVE_ID_KEY]
            val developerUnlocked = context.getSharedPreferences(
                PozixAiAccountRepository.DEV_UNLOCK_PREFS, Context.MODE_PRIVATE
            ).getBoolean(PozixAiAccountRepository.DEV_UNLOCKED, false)
            if (active.isNullOrBlank() || current.none { it.id == active } || (!developerUnlocked && !active.startsWith("builtin-"))) {
                prefs[ACTIVE_ID_KEY] = "builtin-openai"
            }
        }
    }

    fun getProviders(): Flow<List<AiProvider>> =
        context.aiProvidersDataStore.data.map { prefs ->
            decode(prefs[PROVIDERS_KEY].orEmpty())
        }

    fun getActiveProviderId(): Flow<String> =
        context.aiProvidersDataStore.data.map { prefs -> prefs[ACTIVE_ID_KEY].orEmpty() }

    suspend fun getActiveProvider(): AiProvider? {
        val snapshot = context.aiProvidersDataStore.data.first()
        val providers = decode(snapshot[PROVIDERS_KEY].orEmpty())
        if (providers.isEmpty()) return null
        val activeId = snapshot[ACTIVE_ID_KEY].orEmpty()
        return providers.firstOrNull { it.id == activeId } ?: providers.first()
    }

    suspend fun saveProvider(provider: AiProvider, setActive: Boolean = false) {
        context.aiProvidersDataStore.edit { prefs ->
            val current = decode(prefs[PROVIDERS_KEY].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == provider.id }
            if (index >= 0) current[index] = provider else current.add(provider)
            prefs[PROVIDERS_KEY] = encode(current)
            if (setActive || prefs[ACTIVE_ID_KEY].isNullOrBlank()) {
                prefs[ACTIVE_ID_KEY] = provider.id
            }
        }
    }

    suspend fun deleteProvider(id: String) {
        context.aiProvidersDataStore.edit { prefs ->
            val current = decode(prefs[PROVIDERS_KEY].orEmpty()).filterNot { it.id == id }
            prefs[PROVIDERS_KEY] = encode(current)
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
            prefs[PROVIDERS_KEY] = encode(listOf(provider))
            prefs[ACTIVE_ID_KEY] = provider.id
        }
    }

    private fun decode(raw: String): List<AiProvider> {
        if (raw.isBlank()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(AiProvider.serializer()), raw).map { provider ->
                provider.copy(apiKey = runCatching { SecretCipher.decrypt(provider.apiKey) }.getOrDefault(""))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encode(providers: List<AiProvider>): String = json.encodeToString(
        ListSerializer(AiProvider.serializer()),
        providers.map { provider -> provider.copy(apiKey = SecretCipher.encrypt(provider.apiKey)) }
    )

    suspend fun migrateLegacySecrets() {
        context.aiProvidersDataStore.edit { prefs ->
            val raw = prefs[PROVIDERS_KEY].orEmpty()
            if (raw.isBlank()) return@edit
            val stored = runCatching { json.decodeFromString(ListSerializer(AiProvider.serializer()), raw) }.getOrNull()
                ?: return@edit
            if (stored.any { it.apiKey.isNotBlank() && !SecretCipher.isEncrypted(it.apiKey) }) {
                prefs[PROVIDERS_KEY] = encode(decode(raw))
            }
        }
    }
}
