package com.hkm.pozix.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.quizDataStore: DataStore<Preferences> by preferencesDataStore(name = "quiz_data")

class QuizRepository(private val context: Context) {
    
    private val QUIZ_JSON_KEY = stringPreferencesKey("current_quiz_json")
    private val QUIZ_SET_ID_KEY = stringPreferencesKey("current_quiz_set_id")
    
    suspend fun saveQuizJson(jsonString: String, quizSetId: String = "") {
        context.quizDataStore.edit { preferences ->
            preferences[QUIZ_JSON_KEY] = jsonString
            preferences[QUIZ_SET_ID_KEY] = quizSetId
        }
    }
    
    fun getQuizJson(): Flow<String?> {
        return context.quizDataStore.data.map { preferences ->
            preferences[QUIZ_JSON_KEY]
        }
    }
    
    fun getQuizSetId(): Flow<String?> {
        return context.quizDataStore.data.map { preferences ->
            preferences[QUIZ_SET_ID_KEY]
        }
    }
    
    suspend fun clearQuiz() {
        context.quizDataStore.edit { preferences ->
            preferences.remove(QUIZ_JSON_KEY)
            preferences.remove(QUIZ_SET_ID_KEY)
        }
    }
}
