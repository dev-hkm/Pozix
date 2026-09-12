package com.hkm.pozix.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hkm.pozix.data.model.SavedQuizSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.savedQuizDataStore: DataStore<Preferences> by preferencesDataStore(name = "saved_quiz_sets")

class SavedQuizRepository(private val context: Context) {
    
    private val SAVED_QUIZ_SETS_KEY = stringPreferencesKey("saved_quiz_sets")
    private val CORRUPT_BACKUP_KEY = stringPreferencesKey("saved_quiz_sets_corrupt_backup")
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }
    
    suspend fun saveQuizSet(quizSet: SavedQuizSet) {
        context.savedQuizDataStore.edit { preferences ->
            val currentSets = readForMutation(preferences) ?: return@edit
            val updatedSets = listOf(quizSet) + currentSets.filter { it.id != quizSet.id }
            preferences[SAVED_QUIZ_SETS_KEY] = json.encodeToString(updatedSets)
        }
    }
    
    suspend fun deleteQuizSet(id: String) {
        context.savedQuizDataStore.edit { preferences ->
            val currentSets = readForMutation(preferences) ?: return@edit
            val updatedSets = currentSets.filter { it.id != id }
            preferences[SAVED_QUIZ_SETS_KEY] = json.encodeToString(updatedSets)
        }
    }

    suspend fun replaceQuizSets(quizSets: List<SavedQuizSet>) {
        context.savedQuizDataStore.edit { preferences ->
            if (preferences[SAVED_QUIZ_SETS_KEY] != null && readForMutation(preferences) == null) return@edit
            preferences[SAVED_QUIZ_SETS_KEY] = json.encodeToString(quizSets)
        }
    }
    
    suspend fun updateQuizSetName(id: String, newName: String) {
        context.savedQuizDataStore.edit { preferences ->
            val currentSets = readForMutation(preferences) ?: return@edit
            val updatedSets = currentSets.map { set ->
                if (set.id == id) set.copy(name = newName) else set
            }
            preferences[SAVED_QUIZ_SETS_KEY] = json.encodeToString(updatedSets)
        }
    }
    
    suspend fun updateLastUsedTimestamp(id: String) {
        context.savedQuizDataStore.edit { preferences ->
            val currentSets = readForMutation(preferences) ?: return@edit
            val updatedSets = currentSets.map { set ->
                if (set.id == id) set.copy(lastUsedTimestamp = System.currentTimeMillis()) else set
            }
            preferences[SAVED_QUIZ_SETS_KEY] = json.encodeToString(updatedSets)
        }
    }
    
    fun getSavedQuizSets(): Flow<List<SavedQuizSet>> {
        return context.savedQuizDataStore.data.map { preferences ->
            getSavedQuizSetsSync(preferences)
        }
    }
    
    fun getQuizSetById(id: String): Flow<SavedQuizSet?> {
        return getSavedQuizSets().map { sets ->
            sets.find { it.id == id }
        }
    }
    
    private fun getSavedQuizSetsSync(preferences: Preferences): List<SavedQuizSet> {
        val jsonString = preferences[SAVED_QUIZ_SETS_KEY] ?: return emptyList()
        return try {
            json.decodeFromString<List<SavedQuizSet>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun readForMutation(preferences: MutablePreferences): List<SavedQuizSet>? {
        val raw = preferences[SAVED_QUIZ_SETS_KEY] ?: return emptyList()
        return try {
            json.decodeFromString<List<SavedQuizSet>>(raw)
        } catch (_: Exception) {
            // Preserve the bytes so a transient schema/corruption issue can
            // never turn the next save/delete into a silent full data wipe.
            if (preferences[CORRUPT_BACKUP_KEY] == null) preferences[CORRUPT_BACKUP_KEY] = raw
            null
        }
    }
}
