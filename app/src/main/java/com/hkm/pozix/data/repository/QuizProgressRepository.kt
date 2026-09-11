package com.hkm.pozix.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hkm.pozix.data.model.QuizProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.quizProgressDataStore: DataStore<Preferences> by preferencesDataStore(name = "quiz_progress")

class QuizProgressRepository(private val context: Context) {
    private val saveMutex = Mutex()
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // Create a unique key for each quiz set
    private fun getProgressKey(quizSetId: String) = stringPreferencesKey("quiz_progress_$quizSetId")
    
    // Save progress for a specific quiz set
    suspend fun saveProgressForQuiz(quizSetId: String, progress: QuizProgress) {
        saveMutex.withLock {
            val encoded = withContext(Dispatchers.Default) { json.encodeToString(progress) }
            context.quizProgressDataStore.edit { preferences ->
                preferences[getProgressKey(quizSetId)] = encoded
            }
        }
    }
    
    // Get progress for a specific quiz set
    fun getProgressForQuiz(quizSetId: String): Flow<QuizProgress?> {
        return context.quizProgressDataStore.data.map { preferences ->
            val jsonString = preferences[getProgressKey(quizSetId)] ?: return@map null
            try {
                json.decodeFromString<QuizProgress>(jsonString)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    // Get all progress data for all quiz sets
    fun getAllProgress(): Flow<Map<String, QuizProgress>> {
        return context.quizProgressDataStore.data.map { preferences ->
            val progressMap = mutableMapOf<String, QuizProgress>()
            preferences.asMap().forEach { (key, value) ->
                if (key.name.startsWith("quiz_progress_") && value is String) {
                    try {
                        val progress = json.decodeFromString<QuizProgress>(value)
                        progressMap[progress.quizSetId] = progress
                    } catch (e: Exception) {
                        // Skip invalid entries
                    }
                }
            }
            progressMap
        }
    }
    
    // Clear progress for a specific quiz set
    suspend fun clearProgressForQuiz(quizSetId: String) {
        context.quizProgressDataStore.edit { preferences ->
            preferences.remove(getProgressKey(quizSetId))
        }
    }

    suspend fun replaceAllProgress(progressRecords: List<QuizProgress>) {
        context.quizProgressDataStore.edit { preferences ->
            preferences.clear()
            progressRecords.forEach { progress ->
                preferences[getProgressKey(progress.quizSetId)] = json.encodeToString(progress)
            }
        }
    }
    
    // Mark a quiz as completed
    suspend fun markCompleted(quizSetId: String) {
        context.quizProgressDataStore.edit { preferences ->
            val jsonString = preferences[getProgressKey(quizSetId)] ?: return@edit
            try {
                val progress = json.decodeFromString<QuizProgress>(jsonString)
                val completedProgress = progress.copy(
                    isCompleted = true,
                    completedTimestamp = System.currentTimeMillis()
                )
                preferences[getProgressKey(quizSetId)] = json.encodeToString(completedProgress)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    // Legacy support - for backward compatibility with old single-progress system
    @Deprecated("Use saveProgressForQuiz instead")
    suspend fun saveProgress(progress: QuizProgress) {
        saveProgressForQuiz(progress.quizSetId, progress)
    }
    
    @Deprecated("Use getProgressForQuiz instead")
    fun getCurrentProgress(): Flow<QuizProgress?> {
        // Return the most recently updated progress
        return getAllProgress().map { progressMap ->
            progressMap.values.maxByOrNull { it.elapsedTimeMillis }
        }
    }
    
    @Deprecated("Use clearProgressForQuiz instead")
    suspend fun clearProgress() {
        // This is kept for compatibility but should not be used
        context.quizProgressDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
