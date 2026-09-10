package com.hkm.pozix.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hkm.pozix.data.model.ExamSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.examSessionDataStore by preferencesDataStore(name = "exam_session")

class ExamSessionRepository(private val context: Context) {
    private val key = stringPreferencesKey("active_exam")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; allowStructuredMapKeys = true }

    fun activeSession(): Flow<ExamSession?> = context.examSessionDataStore.data.map { preferences ->
        preferences[key]?.let { runCatching { json.decodeFromString<ExamSession>(it) }.getOrNull() }
    }

    suspend fun save(session: ExamSession) = context.examSessionDataStore.edit { it[key] = json.encodeToString(session) }
    suspend fun clear() = context.examSessionDataStore.edit { it.remove(key) }
}
