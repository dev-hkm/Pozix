package com.hkm.pozix.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    private val LANGUAGE_KEY = stringPreferencesKey("language")
    private val FONT_KEY = stringPreferencesKey("font")
    private val SHUFFLE_QUESTIONS_KEY = booleanPreferencesKey("shuffle_questions")
    private val SHUFFLE_ANSWERS_KEY = booleanPreferencesKey("shuffle_answers")
    private val SHOW_EXPLANATION_KEY = booleanPreferencesKey("show_explanation")
    private val GEMINI_API_KEY_KEY = stringPreferencesKey("gemini_api_key")
    private val CLOUD_BACKUP_TOKEN_KEY = stringPreferencesKey("cloud_backup_token")
    
    suspend fun setLanguage(languageCode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }
    
    fun getLanguage(): Flow<String> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[LANGUAGE_KEY] ?: "en"
        }
    }
    
    suspend fun setFont(fontName: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[FONT_KEY] = fontName
        }
    }
    
    fun getFont(): Flow<String> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[FONT_KEY] ?: "default"
        }
    }
    
    suspend fun setShuffleQuestions(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SHUFFLE_QUESTIONS_KEY] = enabled
        }
    }
    
    fun getShuffleQuestions(): Flow<Boolean> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[SHUFFLE_QUESTIONS_KEY] ?: false
        }
    }
    
    suspend fun setShuffleAnswers(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SHUFFLE_ANSWERS_KEY] = enabled
        }
    }
    
    fun getShuffleAnswers(): Flow<Boolean> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[SHUFFLE_ANSWERS_KEY] ?: false
        }
    }
    
    suspend fun setShowExplanation(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SHOW_EXPLANATION_KEY] = enabled
        }
    }
    
    fun getShowExplanation(): Flow<Boolean> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[SHOW_EXPLANATION_KEY] ?: true
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[GEMINI_API_KEY_KEY] = apiKey
        }
    }

    fun getGeminiApiKey(): Flow<String> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[GEMINI_API_KEY_KEY] ?: ""
        }
    }

    suspend fun setCloudBackupToken(token: String) = context.settingsDataStore.edit { it[CLOUD_BACKUP_TOKEN_KEY] = token }
    fun getCloudBackupToken(): Flow<String> = context.settingsDataStore.data.map { it[CLOUD_BACKUP_TOKEN_KEY] ?: "" }
    suspend fun clearCloudBackupToken() = context.settingsDataStore.edit { it.remove(CLOUD_BACKUP_TOKEN_KEY) }
}
